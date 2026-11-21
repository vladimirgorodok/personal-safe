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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import ru.opencode.safe.SafeApp
import ru.opencode.safe.data.ai.AgentInput
import ru.opencode.safe.data.ai.AgentResult
import ru.opencode.safe.data.ai.AgentSource
import ru.opencode.safe.data.ai.BrandCatalog
import ru.opencode.safe.data.ai.Heuristics
import ru.opencode.safe.data.model.AiConfig
import ru.opencode.safe.data.model.Category
import ru.opencode.safe.data.model.CategoryType
import ru.opencode.safe.data.model.Defaults
import ru.opencode.safe.data.model.Secret
import ru.opencode.safe.data.model.SecretLink
import ru.opencode.safe.data.model.UserProfile
import ru.opencode.safe.data.model.VaultData
import ru.opencode.safe.data.settings.DarkMode
import ru.opencode.safe.data.settings.DisplaySettings

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

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as SafeApp).container

    private var data: VaultData = VaultData()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _analyze = MutableStateFlow(AnalyzeState())
    val analyze: StateFlow<AnalyzeState> = _analyze.asStateFlow()

    private val _imageSearch = MutableStateFlow(ImageSearchState())
    val imageSearch: StateFlow<ImageSearchState> = _imageSearch.asStateFlow()

    private val brandIconRequests = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            container.settings.settings.collect { display ->
                _state.update { it.copy(display = display) }
            }
        }
        loadVault()
    }

    private fun loadVault() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = container.vault.load()
            val seeded = if (loaded.categories.isEmpty()) {
                loaded.copy(categories = Defaults.defaultCategories())
            } else loaded
            withContext(Dispatchers.Main) {
                data = seeded
                if (loaded.categories.isEmpty()) persist()
                publish()
                seeded.categories.forEach { ensureIconFor(it.id) }
            }
        }
    }

    private fun persist() {
        val snapshot = data
        viewModelScope.launch(Dispatchers.IO) { container.vault.save(snapshot) }
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
        _state.update { it.copy(phase = Phase.AUTH) }
    }

    fun signInDemo() {
        data = data.copy(profile = UserProfile(name = "Гость", email = "guest@local", provider = "demo", isDemo = true))
        persist()
        publish()
        _state.update { it.copy(phase = Phase.READY, authError = null, authLoading = false) }
    }

    fun clearAuthError() = _state.update { it.copy(authError = null) }

    fun signInGoogle(activity: Activity) {
        val clientId = _state.value.display.googleClientId.trim()
        if (clientId.isBlank()) {
            _state.update {
                it.copy(
                    authLoading = false,
                    authError = "Client ID не задан.\n\n" +
                        "Разверните «Настроить Google-вход» и вставьте Client ID веб-записи из Google Cloud. " +
                        "Без него Google не может подтвердить приложение, поэтому остаётся только гостевой вход.",
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
        val secret = Secret(
            categoryId = categoryId,
            type = category?.type ?: CategoryType.OTHER,
            title = "",
            value = "",
            description = "",
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
    fun saveAnalyzedResult(targetCategoryId: String?, newCategoryName: String? = null): Secret? {
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
        )
        saveSecret(secret)
        _state.update { it.copy(toast = "Добавлено в «${category.name}»") }
        return secret
    }

    // ---------------- ИИ-настройки ----------------

    fun saveAiConfig(config: AiConfig) {
        data = data.copy(ai = config)
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
