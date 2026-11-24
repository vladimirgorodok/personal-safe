package ru.opencode.safe.ui

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import ru.opencode.safe.SafeApp
import ru.opencode.safe.data.ai.AgentInput
import ru.opencode.safe.data.ai.AgentResult
import ru.opencode.safe.data.ai.AgentSource
import ru.opencode.safe.data.ai.AiProviders
import ru.opencode.safe.data.ai.BrandCatalog
import ru.opencode.safe.data.ai.Heuristics
import ru.opencode.safe.data.backup.BackupCrypto
import ru.opencode.safe.data.backup.BackupSafety
import ru.opencode.safe.data.backup.DriveBackupClient
import ru.opencode.safe.data.model.AiConfig
import ru.opencode.safe.data.model.Category
import ru.opencode.safe.data.model.CategoryType
import ru.opencode.safe.data.model.Defaults
import ru.opencode.safe.data.model.FieldTemplates
import ru.opencode.safe.data.model.Secret
import ru.opencode.safe.data.model.SecretField
import ru.opencode.safe.data.model.SecretLink
import ru.opencode.safe.data.model.UserProfile
import ru.opencode.safe.data.model.VaultData
import ru.opencode.safe.data.settings.DarkMode
import ru.opencode.safe.data.settings.DisplaySettings
import ru.opencode.safe.ui.drive.DriveAuth
import java.text.SimpleDateFormat
import java.util.Locale
import javax.crypto.AEADBadTagException

/** Задержка перед автоматической копией: ждём, пока пользователь закончит правку. */
private const val AUTO_BACKUP_DELAY_MS = 8_000L

enum class Phase { SPLASH, AUTH, READY }

data class UiState(
    val phase: Phase = Phase.SPLASH,
    val profile: UserProfile? = null,
    val categories: List<Category> = emptyList(),
    val secrets: List<Secret> = emptyList(),
    val ai: AiConfig = AiConfig(),
    val display: DisplaySettings = DisplaySettings(),
    val brandIcons: Map<String, String> = emptyMap(),
    val toast: String? = null,
    val authError: String? = null,
    val authLoading: Boolean = false,
    /** Служебные флаги запуска: пока настройки и сейф не загружены, вход ещё не решаем показывать. */
    val splashDone: Boolean = false,
    val vaultLoaded: Boolean = false,
    val settingsLoaded: Boolean = false,
)

data class AnalyzeState(
    val input: String = "",
    val images: List<ByteArray> = emptyList(),
    val attachmentName: String? = null,
    val imageUrl: String? = null,
    val running: Boolean = false,
    val status: String? = null,
    val result: AgentResult? = null,
    val error: String? = null,
)

data class ImageSearchState(
    val running: Boolean = false,
    val result: AgentResult? = null,
    val matches: List<String> = emptyList(),
    val error: String? = null,
)

/** Состояние резервной копии в Google Drive. */
data class BackupState(
    val running: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val hasRemote: Boolean = false,
    val remoteModified: String? = null,
    /** Сколько секретов в копии на Google Drive (null — копии нет или она не читается). */
    val remoteSecrets: Int? = null,
    /** Копия полнее локальных данных — предлагаем загрузить её одной кнопкой. */
    val offerRestore: Boolean = false,
    /** Доступ к Drive получен — автоматическая копия после изменений работает. */
    val autoActive: Boolean = false,
    /** Время последней автоматической копии. */
    val autoSavedAt: String? = null,
    /** Копия сделана старой версией (с фразой) — для восстановления нужна фраза. */
    val phraseRequired: Boolean = false,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as SafeApp).container

    private var data: VaultData = VaultData()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _analyze = MutableStateFlow(AnalyzeState())
    val analyze: StateFlow<AnalyzeState> = _analyze.asStateFlow()

    private val _imageSearch = MutableStateFlow(ImageSearchState())
    val imageSearch: StateFlow<ImageSearchState> = _imageSearch.asStateFlow()

    private val _backup = MutableStateFlow(BackupState())
    val backup: StateFlow<BackupState> = _backup.asStateFlow()

    private val drive = DriveBackupClient(container.http)

    /** Ключ шифрования копии, полученный из Google Drive в этой сессии. */
    private var driveKey: ByteArray? = null

    /** Проверяли ли уже в этой сессии, что удалённую копию не затрём. */
    private var guardChecked = false

    /** Пробовали ли уже получить доступ к Drive без окон. */
    private var drivePrimed = false

    /** На этом устройстве сейфа ещё не было: чистая установка (можно поднять копию молча). */
    private var freshInstall = false

    private var autoBackupJob: Job? = null

    private val brandIconRequests = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            container.settings.settings.collect { display ->
                _state.update { it.copy(display = display, settingsLoaded = true) }
                resolveStartPhase()
            }
        }
        loadVault()
    }

    private fun loadVault() {
        viewModelScope.launch(Dispatchers.IO) {
            // Смотрим до первого сохранения: иначе сейф создастся и «чистую установку» уже не отличить.
            val hadVault = container.vault.exists()
            val loaded = container.vault.load()
            val seeded = if (loaded.categories.isEmpty()) {
                loaded.copy(categories = Defaults.defaultCategories())
            } else loaded
            withContext(Dispatchers.Main) {
                freshInstall = !hadVault
                data = seeded
                if (loaded.categories.isEmpty()) persist()
                publish()
                seeded.categories.forEach { ensureIconFor(it.id) }
                _state.update { it.copy(vaultLoaded = true) }
                resolveStartPhase()
            }
        }
    }

    private fun persist() {
        val snapshot = data
        viewModelScope.launch(Dispatchers.IO) { container.vault.save(snapshot) }
        scheduleAutoBackup()
    }

    private fun publish() {
        _state.update {
            it.copy(
                categories = data.categories,
                secrets = data.secrets,
                ai = data.ai,
                profile = data.profile,
                brandIcons = data.brandIcons,
            )
        }
    }

    // ---------------- Заставка и вход ----------------

    fun onSplashFinished() {
        _state.update { it.copy(splashDone = true) }
        resolveStartPhase()
    }

    /**
     * Показывает экран входа только если профиля нет или «Запомнить вход» выключено.
     * Ждём загрузки настроек и сейфа, иначе решение пришлось бы принимать по умолчанию.
     */
    private fun resolveStartPhase() {
        val s = _state.value
        if (!s.splashDone || !s.vaultLoaded || !s.settingsLoaded) return
        val next = SessionPolicy.startPhase(s.profile != null, s.display.rememberSignIn)
        if (s.phase != next) _state.update { it.copy(phase = next) }
        if (next == Phase.READY) primeDrive()
    }

    /**
     * Один раз за сессию тихо проверяем доступ к Drive. Если разрешение уже выдано, это
     * происходит без единого окна — и дальше автоматическая копия работает сама.
     */
    private fun primeDrive() {
        if (drivePrimed) return
        val profile = data.profile ?: return
        if (profile.isDemo) return
        drivePrimed = true
        viewModelScope.launch {
            val token = driveToken()
            _backup.update { it.copy(autoActive = token != null) }
            if (token != null) checkRemoteAtStart(token)
        }
    }

    /**
     * Проверяет копию в Drive при запуске. На чистой установке (локальный сейф ещё не создавался)
     * сразу поднимает её — именно этого не хватало: копия сохранялась, но сама не загружалась.
     * Если локальные данные уже есть, а копия полнее, тихо ничего не меняем, а показываем
     * карточку «Загрузить» — пользователь мог удалить записи намеренно.
     */
    private suspend fun checkRemoteAtStart(token: String) {
        val remote = runCatching { drive.findBackup(token) }.getOrNull() ?: return
        val blob = runCatching { drive.download(token, remote.id) }.getOrNull() ?: return
        // Копия старого формата: её не расшифровать без фразы, разбирается на экране резервной копии.
        if (BackupCrypto.isPassphraseBackup(blob)) {
            _backup.update {
                it.copy(hasRemote = true, remoteModified = remote.modifiedTime, remoteSecrets = null)
            }
            return
        }
        val restored = runCatching {
            val json = String(BackupCrypto.decryptWithKey(blob, driveKey(token)), Charsets.UTF_8)
            VaultData.fromJson(JSONObject(json))
        }.getOrNull() ?: return

        val localSecrets = data.secrets.size
        val remoteSecrets = restored.secrets.size
        _backup.update {
            it.copy(hasRemote = true, remoteModified = remote.modifiedTime, remoteSecrets = remoteSecrets)
        }

        when (BackupSafety.startupAction(freshInstall, localSecrets, remoteSecrets)) {
            BackupSafety.StartupAction.RESTORE ->
                applyRestore(restored, remote.modifiedTime, toast = "Загружено из Google Drive: $remoteSecrets секретов")
            BackupSafety.StartupAction.OFFER ->
                _backup.update { it.copy(offerRestore = true) }
            BackupSafety.StartupAction.NONE -> Unit
        }
    }

    /** Загружает предложенную копию — кнопка на карточке в сейфе. */
    fun restoreOfferedBackup() {
        viewModelScope.launch {
            val token = driveToken()
            if (token == null) {
                _backup.update {
                    it.copy(offerRestore = false, error = "Нет доступа к Google Drive: войдите в аккаунт заново")
                }
                return@launch
            }
            restoreBackup(token)
        }
    }

    /** Убирает карточку «есть копия полнее» до следующего запуска. */
    fun dismissRestoreOffer() = _backup.update { it.copy(offerRestore = false) }

    /** Общий путь восстановления: заменить локальные данные копией и записать их на диск. */
    private fun applyRestore(restored: VaultData, modifiedTime: String?, toast: String?) {
        data = restored
        guardChecked = true
        persist()
        publish()
        restored.categories.forEach { ensureIconFor(it.id) }
        if (toast != null) _state.update { it.copy(toast = toast) }
        _backup.update {
            it.copy(
                hasRemote = true,
                remoteModified = modifiedTime,
                remoteSecrets = restored.secrets.size,
                offerRestore = false,
                phraseRequired = false,
            )
        }
    }

    fun signInDemo() {
        data = data.copy(profile = UserProfile(name = "Локальный режим", email = "local@device", provider = "local", isDemo = true))
        persist()
        publish()
        _state.update { it.copy(phase = Phase.READY, authError = null, authLoading = false) }
    }

    fun clearAuthError() = _state.update { it.copy(authError = null) }

    fun setAuthError(message: String) = _state.update { it.copy(authError = message, authLoading = false) }

    fun signInGoogle(activity: Activity) {
        val clientId = _state.value.display.googleClientId.trim()
        if (clientId.isBlank()) {
            _state.update {
                it.copy(
                    authLoading = false,
                    authError = "Client ID не задан.\n\n" +
                        "Разверните «Настроить Google-вход» и вставьте Client ID веб-записи из Google Cloud. " +
                        "Без него Google не может подтвердить приложение, поэтому остаётся только локальный режим.",
                )
            }
            return
        }
        _state.update { it.copy(authLoading = true, authError = null) }
        viewModelScope.launch {
            runCatching { GoogleSignIn(activity).signIn(clientId) }
                .onSuccess { profile ->
                    data = data.copy(profile = profile)
                    persist()
                    publish()
                    // Аккаунт мог смениться — старый токен Drive больше не подходит.
                    DriveAuth.forget()
                    drivePrimed = false
                    primeDrive()
                    _state.update {
                        it.copy(
                            phase = Phase.READY,
                            authLoading = false,
                            authError = null,
                            toast = "Добро пожаловать, ${profile.name}!",
                        )
                    }
                }
                .onFailure { e ->
                    if (e is GetCredentialCancellationException) {
                        _state.update { it.copy(authLoading = false, toast = "Вход отменён") }
                    } else {
                        _state.update { it.copy(authLoading = false, authError = describeGoogleError(e)) }
                    }
                }
        }
    }

    fun signOut() {
        data = data.copy(profile = null)
        persist()
        publish()
        DriveAuth.forget()
        autoBackupJob?.cancel()
        driveKey = null
        guardChecked = false
        drivePrimed = false
        _backup.value = BackupState()
        _state.update { it.copy(phase = Phase.AUTH) }
    }

    fun consumeToast() = _state.update { it.copy(toast = null) }

    fun showToast(message: String) = _state.update { it.copy(toast = message) }

    // ---------------- Настройки отображения ----------------

    fun setThemeSet(id: String) = scope { container.settings.setThemeSet(id) }
    fun setDarkMode(mode: DarkMode) = scope { container.settings.setDarkMode(mode) }
    fun setDynamicColor(value: Boolean) = scope { container.settings.setDynamicColor(value) }
    fun setGlassEffects(value: Boolean) = scope { container.settings.setGlassEffects(value) }
    fun setCardOpacity(value: Float) = scope { container.settings.setCardOpacity(value) }
    fun setBackgroundOpacity(value: Float) = scope { container.settings.setBackgroundOpacity(value) }
    fun setBorderOpacity(value: Float) = scope { container.settings.setBorderOpacity(value) }
    fun setSplashEnabled(value: Boolean) = scope { container.settings.setSplashEnabled(value) }
    fun setAiSuggest(value: Boolean) = scope { container.settings.setAiSuggest(value) }
    fun setGoogleClientId(value: String) = scope { container.settings.setGoogleClientId(value) }
    fun setBiometric(value: Boolean) = scope { container.settings.setBiometric(value) }
    fun setRememberSignIn(value: Boolean) = scope { container.settings.setRememberSignIn(value) }

    /** Автоматическая копия после изменений: включить или выключить. */
    fun setAutoBackup(value: Boolean) = scope {
        container.settings.setAutoBackup(value)
        if (value) {
            drivePrimed = false
            primeDrive()
        } else {
            autoBackupJob?.cancel()
            _backup.update { it.copy(autoActive = false, autoSavedAt = null) }
        }
    }

    private fun scope(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    // ---------------- Секреты и категории ----------------

    fun secretsOf(categoryId: String): List<Secret> = data.secrets.filter { it.categoryId == categoryId }
    fun categoryOf(id: String?): Category? = data.categories.firstOrNull { it.id == id }

    fun saveSecret(secret: Secret) {
        val now = System.currentTimeMillis()
        val updated = if (data.secrets.any { it.id == secret.id }) {
            data.secrets.map { if (it.id == secret.id) secret.copy(updatedAt = now) else it }
        } else {
            data.secrets + secret.copy(updatedAt = now)
        }
        data = data.copy(secrets = updated)
        persist()
        publish()
    }

    fun deleteSecret(id: String) {
        data = data.copy(secrets = data.secrets.filterNot { it.id == id })
        persist()
        publish()
    }

    fun toggleFavorite(id: String) {
        data = data.copy(secrets = data.secrets.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it })
        persist()
        publish()
    }

    fun saveCategory(category: Category) {
        val updated = if (data.categories.any { it.id == category.id }) {
            data.categories.map { if (it.id == category.id) category else it }
        } else {
            data.categories + category
        }
        data = data.copy(categories = updated)
        persist()
        publish()
        ensureIconFor(category.id)
    }

    fun deleteCategory(id: String) {
        data = data.copy(
            categories = data.categories.filterNot { it.id == id },
            secrets = data.secrets.filterNot { it.categoryId == id },
        )
        persist()
        publish()
    }

    /** Ищет логотип категории в интернете, при неудаче — рисует иконку с инициалами. */
    fun ensureIconFor(categoryId: String) {
        val category = categoryOf(categoryId) ?: return
        if (!category.iconFile.isNullOrBlank()) return
        viewModelScope.launch {
            val domain = category.brandDomain
                ?: BrandCatalog.detect(category.name)?.domain
            val resolved = if (domain != null) container.icons.resolveForBrand(domain)
            else container.icons.resolveForQuery(category.name)
            val generated = container.icons.ensureGenerated(
                category.name,
                category.colorSeed.takeIf { it != 0L } ?: Defaults.seedFor(category.name),
            )
            val file = resolved.file ?: generated
            updateCategorySilently(
                categoryId,
                category.copy(
                    iconFile = file.name,
                    iconUrl = resolved.remoteUrl,
                    brandDomain = resolved.brandDomain ?: category.brandDomain,
                ),
            )
        }
    }

    /** Ручной поиск логотипа по запросу пользователя (например «Яндекс»). */
    fun resolveIconFor(categoryId: String, query: String) {
        viewModelScope.launch {
            val category = categoryOf(categoryId) ?: return@launch
            val resolved = container.icons.resolveForQuery(query)
            val file = resolved.file ?: container.icons.ensureGenerated(
                category.name,
                category.colorSeed.takeIf { it != 0L } ?: Defaults.seedFor(category.name),
            )
            updateCategorySilently(
                categoryId,
                category.copy(
                    iconFile = file.name,
                    iconUrl = resolved.remoteUrl ?: category.iconUrl,
                    brandDomain = resolved.brandDomain ?: category.brandDomain,
                ),
            )
            _state.update {
                it.copy(
                    toast = if (resolved.file != null) "Логотип «${resolved.brandName ?: query}» найден в интернете"
                    else "Логотип не найден — сгенерирована иконка",
                )
            }
        }
    }

    fun createEmptySecret(categoryId: String): Secret {
        val category = categoryOf(categoryId)
        val type = category?.type ?: CategoryType.OTHER
        val secret = Secret(
            categoryId = categoryId,
            type = type,
            title = "",
            value = "",
            description = "",
            // Типовые поля типа (у карты — срок, CVV, PIN): их сразу можно заполнить и копировать.
            fields = FieldTemplates.seed(type),
        )
        saveSecret(secret)
        return secret
    }

    fun regenerateIcon(categoryId: String) {        viewModelScope.launch {
            val category = categoryOf(categoryId) ?: return@launch
            val resolved = category.brandDomain?.let { container.icons.resolveForBrand(it) }
            val generated = container.icons.ensureGenerated(category.name, System.currentTimeMillis())
            updateCategorySilently(
                categoryId,
                category.copy(
                    iconFile = resolved?.file?.name ?: generated.name,
                    iconUrl = resolved?.remoteUrl,
                ),
            )
        }
    }

    private fun updateCategorySilently(categoryId: String, category: Category) {
        data = data.copy(categories = data.categories.map { if (it.id == categoryId) category else it })
        persist()
        publish()
    }

    // ---------------- Бренды и поиск по изображениям ----------------

    fun brandGroups(): List<ru.opencode.safe.data.ai.BrandGroup> =
        ru.opencode.safe.data.ai.BrandGrouper.group(data.secrets, data.categories)

    fun brandIcon(domain: String?): String? {
        if (domain.isNullOrBlank()) return null
        data.brandIcons[domain]?.let { if (container.icons.fileFor(it) != null) return it }
        data.categories.firstOrNull { it.brandDomain == domain }?.iconFile?.let { return it }
        return null
    }

    /** Ищет логотип бренда в интернете (или рисует заглушку) и запоминает его. */
    fun ensureBrandIcon(domain: String, name: String) {
        if (domain.isBlank()) return
        if (data.brandIcons[domain]?.let { container.icons.fileFor(it) } != null) return
        if (!brandIconRequests.add(domain)) return
        viewModelScope.launch {
            val resolved = container.icons.resolveForBrand(domain)
            val file = resolved.file ?: container.icons.ensureGenerated(name, Defaults.seedFor(name))
            data = data.copy(brandIcons = data.brandIcons + (domain to file.name))
            persist()
            publish()
        }
    }

    fun secretsWithImages(): List<Secret> = data.secrets.filter { !it.imageUrl.isNullOrBlank() }

    fun secretsByIds(ids: List<String>): List<Secret> {
        val byId = data.secrets.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    fun clearImageSearch() {
        _imageSearch.value = ImageSearchState()
    }

    /** Поиск по изображению: агент распознаёт бренд/тип, затем ищем совпадения в сейфе. */
    fun runImageSearch(bytes: ByteArray) {
        _imageSearch.update { it.copy(running = true, error = null, result = null, matches = emptyList()) }
        viewModelScope.launch {
            val outcome = runCatching {
                container.analyzer.analyze(
                    AgentInput(text = "", images = listOf(bytes), sourceLabel = "поиск по изображению"),
                    data.ai,
                    data.categories,
                )
            }
            outcome.onSuccess { result ->
                _imageSearch.update {
                    it.copy(running = false, result = result, matches = matchSecrets(result).map { s -> s.id })
                }
            }.onFailure { e ->
                _imageSearch.update { it.copy(running = false, error = e.message ?: "Не удалось распознать изображение") }
            }
        }
    }

    private fun matchSecrets(result: AgentResult): List<Secret> {
        val matchedCategoryId = result.matchedCategoryId
        return data.secrets
            .map { secret ->
                val category = categoryOf(secret.categoryId)
                var score = 0
                if (!result.brandDomain.isNullOrBlank() &&
                    (secret.brandDomain == result.brandDomain || category?.brandDomain == result.brandDomain)
                ) score += 4
                if (!result.brandName.isNullOrBlank() &&
                    (secret.tags.any { it.equals(result.brandName, true) } || category?.name.equals(result.brandName, true))
                ) score += 3
                if (matchedCategoryId != null && secret.categoryId == matchedCategoryId) score += 2
                if (secret.type == result.type) score += 1
                secret to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    // ---------------- Анализ ----------------

    fun setAnalyzeInput(text: String) = _analyze.update { it.copy(input = text, error = null) }

    fun setImageUrl(url: String?) = _analyze.update { it.copy(imageUrl = url) }

    fun resetAnalyze() {
        _analyze.value = AnalyzeState()
    }

    fun attachBytes(bytes: ByteArray, name: String?) =
        _analyze.update { it.copy(images = it.images + bytes, attachmentName = name, error = null) }

    fun clearAttachments() = _analyze.update { it.copy(images = emptyList(), attachmentName = null, imageUrl = null) }

    fun readClipboard(context: Context) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = manager.primaryClip ?: run {
            _analyze.update { it.copy(error = "Буфер обмена пуст") }
            return
        }
        if (clip.itemCount == 0) {
            _analyze.update { it.copy(error = "Буфер обмена пуст") }
            return
        }
        val item = clip.getItemAt(0)
        val uri = item.uri
        if (uri != null) {
            attachFromUri(context, uri)
            return
        }
        val text = item.coerceToText(context)?.toString().orEmpty()
        if (text.isBlank()) {
            _analyze.update { it.copy(error = "В буфере нет текста") }
            return
        }
        applyClipboardText(text)
    }

    private fun applyClipboardText(text: String) {
        val trimmed = text.trim()
        if (Heuristics.isUrl(trimmed) && Heuristics.isImageUrl(trimmed)) {
            _analyze.update { it.copy(imageUrl = trimmed, input = it.input.ifBlank { trimmed }) }
        } else {
            _analyze.update { it.copy(input = trimmed, error = null) }
        }
    }

    fun attachFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val payload = withContext(Dispatchers.IO) { readUri(context, uri) }
            if (payload == null) {
                _analyze.update { it.copy(error = "Не удалось прочитать файл") }
                return@launch
            }
            val name = queryName(context, uri)
            if (payload.isImage) {
                attachBytes(payload.bytes, name)
            } else {
                _analyze.update {
                    it.copy(input = payload.text ?: it.input, attachmentName = name, error = null)
                }
            }
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Секрет") {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText(label, text))
        _state.update { it.copy(toast = "Скопировано в буфер") }
    }

    fun runAnalyze() {
        val current = _analyze.value
        if (current.input.isBlank() && current.images.isEmpty() && current.imageUrl.isNullOrBlank()) {
            _analyze.update { it.copy(error = "Добавьте текст, файл или ссылку") }
            return
        }
        _analyze.update { it.copy(running = true, error = null, status = "Агент изучает находку…", result = null) }
        viewModelScope.launch {
            val images = current.images.toMutableList()
            if (images.isEmpty() && !current.imageUrl.isNullOrBlank()) {
                withContext(Dispatchers.IO) { fetchBytes(current.imageUrl) }?.let { bytes ->
                    images.add(bytes)
                    _analyze.update { it.copy(status = "Загружено изображение по ссылке…") }
                }
            }
            val input = AgentInput(
                text = current.input,
                images = images,
                imageUrl = current.imageUrl,
                sourceLabel = current.attachmentName ?: if (images.isNotEmpty()) "изображение" else "текст",
            )
            val result = runCatching {
                container.analyzer.analyze(input, data.ai, data.categories)
            }
            result.onSuccess { agentResult ->
                val stored = if (images.isNotEmpty() && current.images.isEmpty()) listOf(images.last()) else current.images
                _analyze.update {
                    it.copy(
                        running = false,
                        result = agentResult,
                        images = stored,
                        status = null,
                        error = null,
                    )
                }
            }.onFailure { e ->
                _analyze.update { it.copy(running = false, status = null, error = e.message ?: "Ошибка анализа") }
            }
        }
    }

    /** Сохраняет найденный секрет: в существующую или только что созданную категорию. */
    fun saveAnalyzedResult(
        targetCategoryId: String?,
        newCategoryName: String? = null,
        fields: List<SecretField>? = null,
    ): Secret? {
        val result = _analyze.value.result ?: return null
        val category = when {
            targetCategoryId != null -> categoryOf(targetCategoryId)
            else -> {
                val name = newCategoryName?.trim().takeUnless { it.isNullOrBlank() }
                    ?: result.categoryName.ifBlank { Defaults.systemCategoryName(result.type) }
                val created = Category(
                    name = name,
                    type = result.type,
                    description = result.description,
                    colorSeed = Defaults.seedFor(name),
                    brandDomain = result.brandDomain,
                    iconUrl = result.brandDomain?.let { ru.opencode.safe.data.icons.IconRepository.logoUrl(it) },
                )
                saveCategory(created)
                created
            }
        } ?: return null

        val secret = Secret(
            categoryId = category.id,
            type = result.type,
            title = result.title.ifBlank { category.name },
            value = result.value,
            description = result.description,
            note = result.note,
            imageUrl = _analyze.value.imageUrl,
            brandDomain = result.brandDomain,
            detailedInfo = result.detailedInfo,
            links = result.links,
            tags = result.brandName?.let { listOf(it) } ?: emptyList(),
            foundByAgent = result.source == AgentSource.AI,
            // Значения, проверенные на экране анализа; если их не передали — раскладываем из ответа агента.
            fields = (fields ?: FieldTemplates.merge(result.type, result.fields))
                .filter { it.label.isNotBlank() || it.value.isNotBlank() },
        )
        saveSecret(secret)
        _state.update { it.copy(toast = "Добавлено в «${category.name}»") }
        return secret
    }

    // ---------------- ИИ-настройки ----------------

    fun saveAiConfig(config: AiConfig) {
        data = data.copy(ai = AiProviders.normalized(config))
        persist()
        publish()
    }

    fun testAiConfig(config: AiConfig, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val message = runCatching {
                val models = container.aiClient.models(config)
                "Соединение установлено. Доступно моделей: ${models.size}."
            }.getOrElse { "Ошибка: ${it.message}" }
            _state.update { it.copy(toast = message) }
            onResult(message)
        }
    }

    fun loadModels(config: AiConfig, onResult: (Result<List<String>>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { container.aiClient.models(config) }
            onResult(result)
        }
    }

    // ---------------- Резервная копия (Google Drive) ----------------

    fun clearBackupStatus() = _backup.update { it.copy(message = null, error = null) }

    /** Проверяет, есть ли в Google Drive сохранённая копия. */
    fun refreshDriveBackup(token: String) {
        _backup.update { it.copy(running = true, error = null, message = null) }
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { drive.findBackup(token) } }
                .onSuccess { file ->
                    _backup.update {
                        it.copy(
                            running = false,
                            hasRemote = file != null,
                            remoteModified = file?.modifiedTime,
                            phraseRequired = false,
                        )
                    }
                }
                .onFailure { e ->
                    _backup.update { it.copy(running = false, error = e.message ?: "Не удалось проверить копии") }
                }
        }
    }

    // ---------------- Автоматическая копия: ключ лежит рядом с копией в Drive ----------------

    /**
     * Тихо получает доступ к Google Drive, если разрешение уже выдано. Окна согласия здесь
     * не показываем — их вызывают только кнопки на экране резервной копии.
     */
    private suspend fun driveToken(): String? {
        DriveAuth.cachedToken()?.let { return it }
        val profile = data.profile ?: return null
        if (profile.isDemo) return null
        return DriveAuth.silentToken(getApplication(), profile.email)
    }

    /** Ключ шифрования копии: отдельный файл в скрытой папке приложения, рядом с копией. */
    private suspend fun driveKey(token: String): ByteArray {
        driveKey?.let { return it }
        val existing = drive.findKey(token)
        val stored = existing?.let { runCatching { drive.download(token, it.id) }.getOrNull() }
        if (stored != null && stored.size == BackupCrypto.KEY_LEN) {
            driveKey = stored
            return stored
        }
        val created = BackupCrypto.newKey()
        drive.uploadKey(token, created, existing?.id)
        driveKey = created
        return created
    }

    /** Копия после каждого изменения. Небольшая задержка — чтобы не отправлять файл на каждое нажатие. */
    private fun scheduleAutoBackup() {
        if (!_state.value.display.autoBackup) return
        val profile = data.profile ?: return
        if (profile.isDemo) return
        if (data.secrets.isEmpty()) return
        autoBackupJob?.cancel()
        autoBackupJob = viewModelScope.launch {
            delay(AUTO_BACKUP_DELAY_MS)
            autoBackupNow()
        }
    }

    private suspend fun autoBackupNow() {
        val token = driveToken() ?: run {
            _backup.update { it.copy(autoActive = false) }
            return
        }
        _backup.update { it.copy(autoActive = true) }
        val snapshot = data
        val payload = snapshot.toJson().toString().toByteArray(Charsets.UTF_8)
        val localSecrets = snapshot.secrets.size
        runCatching {
            withContext(Dispatchers.IO) {
                if (!guardChecked) checkRemoteSafe(token, localSecrets)
                val key = driveKey(token)
                val blob = BackupCrypto.encryptWithKey(payload, key)
                val existing = drive.findBackup(token)
                drive.uploadBackup(token, blob, existing?.id)
            }
        }
            .onSuccess { saved ->
                _backup.update {
                    it.copy(
                        hasRemote = true,
                        remoteModified = saved.modifiedTime,
                        autoSavedAt = saved.modifiedTime,
                        error = null,
                    )
                }
            }
            .onFailure { e ->
                _backup.update { it.copy(autoActive = false, error = e.message ?: "Не удалось сохранить копию") }
            }
    }

    /**
     * Перед первой в этой сессии автоматической копией смотрим, что лежит в Drive. Если там
     * копия с большим числом секретов (приложение открыли на новом устройстве) или копия
     * старого формата, автоматически перезаписывать её нельзя — иначе хорошая копия пропадёт.
     */
    private suspend fun checkRemoteSafe(token: String, localSecrets: Int) {
        val remote = drive.findBackup(token)
        if (remote == null) {
            guardChecked = true
            return
        }
        val blob = drive.download(token, remote.id)
        if (BackupCrypto.isPassphraseBackup(blob)) {
            throw IllegalStateException(
                "В Google Drive лежит копия старого формата (с фразой). Чтобы её не затереть, автоматически " +
                    "не сохраняю. Нажмите «Сохранить сейчас», если готовы её заменить.",
            )
        }
        val remoteSecrets = runCatching {
            val key = driveKey(token)
            val json = String(BackupCrypto.decryptWithKey(blob, key), Charsets.UTF_8)
            JSONObject(json).optJSONArray("secrets")?.length() ?: 0
        }.getOrDefault(0)
        if (!BackupSafety.safeToOverwrite(localSecrets, remoteSecrets)) {
            throw IllegalStateException(
                "В Google Drive копия, где секретов больше ($remoteSecrets против $localSecrets). Автоматически " +
                    "не сохраняю, чтобы её не затереть: сначала «Восстановить из Google Drive».",
            )
        }
        guardChecked = true
    }

    /** Ручное сохранение — явное действие, поэтому ничего не переспрашиваем. */
    fun createBackup(token: String) {
        _backup.update { it.copy(running = true, error = null, message = null) }
        val snapshot = data
        val payload = snapshot.toJson().toString().toByteArray(Charsets.UTF_8)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val key = driveKey(token)
                    val blob = BackupCrypto.encryptWithKey(payload, key)
                    val existing = drive.findBackup(token)
                    drive.uploadBackup(token, blob, existing?.id)
                }
            }
                .onSuccess { saved ->
                    guardChecked = true
                    _backup.update {
                        it.copy(
                            running = false,
                            hasRemote = true,
                            remoteModified = saved.modifiedTime,
                            autoSavedAt = saved.modifiedTime,
                            error = null,
                            message = "Копия сохранена в Google Drive${formatRemoteTime(saved.modifiedTime)?.let { " · $it" } ?: ""}",
                        )
                    }
                }
                .onFailure { e ->
                    _backup.update { it.copy(running = false, error = e.message ?: "Не удалось сохранить копию") }
                }
        }
    }

    /**
     * Скачивает копию и заменяет локальные данные. Обычно копия зашифрована ключом, который
     * лежит рядом в Drive, — тогда фраза не нужна. Копии, сделанные старыми версиями, шифровались
     * фразой: для них просим её ввести.
     */
    fun restoreBackup(token: String, passphrase: String? = null) {
        _backup.update { it.copy(running = true, error = null, message = null) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val file = drive.findBackup(token) ?: throw IllegalStateException("В Google Drive нет резервной копии")
                    val blob = drive.download(token, file.id)
                    val json = if (BackupCrypto.isPassphraseBackup(blob)) {
                        val phrase = passphrase?.takeIf { it.isNotBlank() } ?: throw PhraseRequired()
                        String(BackupCrypto.decrypt(blob, phrase.toCharArray()), Charsets.UTF_8)
                    } else {
                        String(BackupCrypto.decryptWithKey(blob, driveKey(token)), Charsets.UTF_8)
                    }
                    VaultData.fromJson(JSONObject(json)) to file
                }
            }
            result
                .onSuccess { (restored, file) ->
                    applyRestore(restored, file.modifiedTime, toast = null)
                    _backup.update {
                        it.copy(
                            running = false,
                            message = "Данные восстановлены: ${restored.secrets.size} секретов · ${restored.categories.size} категорий",
                        )
                    }
                }
                .onFailure { e ->
                    val message = when {
                        e is PhraseRequired -> "Копия сделана старой версией — введите фразу, которой она шифровалась"
                        e is AEADBadTagException -> "Расшифровать копию не удалось: неверная фраза или ключ не подходит"
                        else -> e.message
                    }
                    _backup.update {
                        it.copy(
                            running = false,
                            phraseRequired = e is PhraseRequired,
                            error = message ?: "Не удалось восстановить копию",
                        )
                    }
                }
        }
    }

    /** Копия сделана с фразой — без неё не расшифровать. */
    private class PhraseRequired : Exception()

    private fun formatRemoteTime(raw: String?): String? = runCatching {
        raw ?: return null
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
        parser.parse(raw.take(19))?.let { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(it) }
    }.getOrNull()

    // ---------------- Вспомогательное ----------------

    private suspend fun fetchBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).header("User-Agent", "SafeApp/1.0").get().build()
            container.http.newCall(request).execute().use { it.body?.bytes() }
        }.getOrNull()
    }

    private data class UriPayload(val bytes: ByteArray, val isImage: Boolean, val text: String?)

    private fun readUri(context: Context, uri: Uri): UriPayload? = runCatching {
        val resolver = context.contentResolver
        val type = resolver.getType(uri).orEmpty()
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        if (type.startsWith("image/") || looksLikeImage(bytes)) {
            UriPayload(bytes, true, null)
        } else {
            val text = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull()
            UriPayload(bytes, false, text)
        }
    }.getOrNull()

    private fun looksLikeImage(bytes: ByteArray): Boolean =
        bytes.size >= 4 && (bytes[0] == 0x89.toByte() || (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()))

    private fun queryName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }.getOrNull() ?: uri.lastPathSegment
}
