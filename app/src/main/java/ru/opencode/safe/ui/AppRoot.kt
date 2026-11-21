package ru.opencode.safe.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import ru.opencode.safe.SafeApp
import ru.opencode.safe.ui.analyze.AnalyzeScreen
import ru.opencode.safe.ui.auth.AuthScreen
import ru.opencode.safe.ui.auth.LockScreen
import ru.opencode.safe.ui.components.LocalIconDir
import ru.opencode.safe.ui.nav.Navigator
import ru.opencode.safe.ui.nav.Screen
import ru.opencode.safe.ui.settings.AiSettingsScreen
import ru.opencode.safe.ui.settings.AppearanceScreen
import ru.opencode.safe.ui.settings.SettingsScreen
import ru.opencode.safe.ui.splash.SplashScreen
import ru.opencode.safe.ui.theme.LocalThemeSet
import ru.opencode.safe.ui.theme.LocalUiOpacity
import ru.opencode.safe.ui.theme.SafeTheme
import ru.opencode.safe.ui.theme.UiOpacity
import ru.opencode.safe.ui.theme.backgroundBrush
import ru.opencode.safe.ui.theme.resolveDark
import ru.opencode.safe.ui.vault.BrandDetailScreen
import ru.opencode.safe.ui.vault.CategoryDetailScreen
import ru.opencode.safe.ui.vault.CategoryEditScreen
import ru.opencode.safe.ui.vault.ImageSearchScreen
import ru.opencode.safe.ui.vault.SecretDetailScreen
import ru.opencode.safe.ui.vault.VaultScreen

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as SafeApp).container }
    val viewModel: MainViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val analyzeState by viewModel.analyze.collectAsState()
    val imageSearchState by viewModel.imageSearch.collectAsState()
    val nav = remember { Navigator() }
    val activity = context as? Activity
    var unlocked by remember { mutableStateOf(false) }

    val dark = resolveDark(state.display.darkMode)
    val uiOpacity = UiOpacity(
        card = state.display.cardOpacity,
        background = state.display.backgroundOpacity,
        border = state.display.borderOpacity,
    )
    val signatureSha1 = remember { appSignatureSha1(context) }
    val googleSupport = remember { checkGoogleSupport(context) }
    val openUrl: (String) -> Unit = { url ->
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    LaunchedEffect(state.display.splashEnabled) {
        if (!state.display.splashEnabled && state.phase == Phase.SPLASH) viewModel.onSplashFinished()
    }

    SafeTheme(
        themeSetId = state.display.themeSetId,
        darkMode = state.display.darkMode,
        dynamicColor = state.display.dynamicColor,
    ) {
        val theme = LocalThemeSet.current
        CompositionLocalProvider(
            LocalIconDir provides container.icons.directory.absolutePath,
            LocalUiOpacity provides uiOpacity,
        ) {
            Box(modifier = Modifier.fillMaxSize().background(theme.backgroundBrush(dark, state.display.backgroundOpacity))) {

                when (state.phase) {
                    Phase.SPLASH -> SplashScreen(onFinished = viewModel::onSplashFinished)
                    Phase.AUTH -> AuthScreen(
                        profile = state.profile,
                        googleClientId = state.display.googleClientId,
                        packageName = context.packageName,
                        signatureSha1 = signatureSha1,
                        support = googleSupport,
                        error = state.authError,
                        loading = state.authLoading,
                        onGoogle = {
                            val act = activity
                            if (act != null) viewModel.signInGoogle(act)
                            else viewModel.showToast("Не удалось открыть окно входа — попробуйте ещё раз")
                        },
                        onDemo = viewModel::signInDemo,
                        onClientId = viewModel::setGoogleClientId,
                        onClearError = viewModel::clearAuthError,
                        onCopy = { text -> viewModel.copyToClipboard(context, text, "Текст ошибки") },
                        onOpenLink = openUrl,
                    )
                    Phase.READY -> {
                        if (state.display.biometricLock && !unlocked) {
                            LockScreen(
                                activity = activity as? androidx.fragment.app.FragmentActivity,
                                onUnlocked = { unlocked = true },
                                onFallback = { unlocked = true },
                            )
                        } else {
                            ReadyContent(
                                context = context,
                                state = state,
                                analyzeState = analyzeState,
                                imageSearchState = imageSearchState,
                                nav = nav,
                                viewModel = viewModel,
                                signatureSha1 = signatureSha1,
                                googleSupport = googleSupport,
                                onBiometric = { enabled ->
                                    viewModel.setBiometric(enabled)
                                    if (enabled) unlocked = true
                                },
                                onOpenLink = { url ->
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                },
                                onPickedUri = { uri -> viewModel.attachFromUri(context, uri) },
                                onCopy = { text -> viewModel.copyToClipboard(context, text) },
                            )
                        }
                    }
                }

                state.toast?.let { message ->
                    LaunchedEffect(message) {
                        delay(2800)
                        viewModel.consumeToast()
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 28.dp, start = 24.dp, end = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                    ) {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyContent(
    context: android.content.Context,
    state: UiState,
    analyzeState: AnalyzeState,
    imageSearchState: ImageSearchState,
    nav: Navigator,
    viewModel: MainViewModel,
    signatureSha1: String?,
    googleSupport: GoogleSupport,
    onBiometric: (Boolean) -> Unit,
    onOpenLink: (String) -> Unit,
    onPickedUri: (Uri) -> Unit,
    onCopy: (String) -> Unit,
) {
    BackHandler(enabled = nav.canGoBack) { nav.pop() }

    AnimatedContent(
        targetState = nav.current,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
        label = "nav",
    ) { screen ->
        when (screen) {
            Screen.Vault -> VaultScreen(
                profile = state.profile,
                categories = state.categories,
                secrets = state.secrets,
                brandIcons = state.brandIcons,
                onOpenSettings = { nav.push(Screen.Settings) },
                onOpenAnalyze = { nav.push(Screen.Analyze) },
                onOpenImageSearch = { nav.push(Screen.ImageSearch) },
                onOpenCategory = { nav.push(Screen.CategoryDetail(it)) },
                onOpenSecret = { nav.push(Screen.SecretDetail(it)) },
                onOpenBrand = { domain, name -> nav.push(Screen.BrandDetail(domain, name)) },
                onCreateCategory = { nav.push(Screen.CategoryEdit(null)) },
                onToggleFavorite = viewModel::toggleFavorite,
                onEnsureBrandIcon = viewModel::ensureBrandIcon,
            )

            Screen.Analyze -> AnalyzeScreen(
                state = analyzeState,
                categories = state.categories,
                aiConfigured = state.ai.isConfigured && state.ai.enabled,
                autoPaste = true,
                onInput = viewModel::setAnalyzeInput,
                onPasteClipboard = { viewModel.readClipboard(context) },
                onPickedUri = onPickedUri,
                onClearAttachments = viewModel::clearAttachments,
                onRun = viewModel::runAnalyze,
                onSave = { categoryId, newName ->
                    viewModel.saveAnalyzedResult(categoryId, newName)
                    viewModel.resetAnalyze()
                    nav.resetTo(Screen.Vault)
                },
                onOpenLink = onOpenLink,
                onCopy = onCopy,
                onOpenAiSettings = { nav.push(Screen.AiSettings) },
                onBack = { nav.pop() },
            )

            Screen.ImageSearch -> ImageSearchScreen(
                state = imageSearchState,
                matches = viewModel.secretsByIds(imageSearchState.matches),
                gallery = viewModel.secretsWithImages(),
                categories = state.categories,
                brandIcons = state.brandIcons,
                aiConfigured = state.ai.isConfigured && state.ai.enabled,
                onAnalyze = viewModel::runImageSearch,
                onClear = viewModel::clearImageSearch,
                onOpenSecret = { nav.push(Screen.SecretDetail(it)) },
                onOpenLink = onOpenLink,
                onOpenAiSettings = { nav.push(Screen.AiSettings) },
                onEnsureBrandIcon = viewModel::ensureBrandIcon,
                onBack = { nav.pop() },
            )

            is Screen.BrandDetail -> BrandDetailScreen(
                name = screen.name,
                domain = screen.domain.ifBlank { null },
                iconFile = viewModel.brandIcon(screen.domain),
                secrets = state.secrets.filter { secret ->
                    val category = state.categories.firstOrNull { it.id == secret.categoryId }
                    ru.opencode.safe.data.ai.BrandGrouper.resolveBrand(secret, category)?.domain == screen.domain
                },
                categories = state.categories,
                onBack = { nav.pop() },
                onOpenSecret = { nav.push(Screen.SecretDetail(it)) },
                onToggleFavorite = viewModel::toggleFavorite,
            )

            Screen.Settings -> SettingsScreen(
                profile = state.profile,
                display = state.display,
                themeName = themeName(state.display.themeSetId),
                aiConfigured = state.ai.isConfigured,
                onBack = { nav.pop() },
                onAppearance = { nav.push(Screen.Appearance) },
                onAiSettings = { nav.push(Screen.AiSettings) },
                onSplash = viewModel::setSplashEnabled,
                onGlass = viewModel::setGlassEffects,
                onAiSuggest = viewModel::setAiSuggest,
                onBiometric = onBiometric,
                onSignOut = { viewModel.signOut(); nav.resetTo(Screen.Vault) },
            )

            Screen.Appearance -> AppearanceScreen(
                display = state.display,
                signatureSha1 = signatureSha1,
                packageName = context.packageName,
                support = googleSupport,
                onBack = { nav.pop() },
                onTheme = viewModel::setThemeSet,
                onDarkMode = viewModel::setDarkMode,
                onDynamic = viewModel::setDynamicColor,
                onGlass = viewModel::setGlassEffects,
                onClientId = viewModel::setGoogleClientId,
                onCardOpacity = viewModel::setCardOpacity,
                onBackgroundOpacity = viewModel::setBackgroundOpacity,
                onBorderOpacity = viewModel::setBorderOpacity,
                onOpenLink = onOpenLink,
                onCopy = onCopy,
            )

            Screen.AiSettings -> AiSettingsScreen(
                config = state.ai,
                onBack = { nav.pop() },
                onSave = { viewModel.saveAiConfig(it); viewModel.showToast("Настройки агента сохранены") },
                onTest = { viewModel.testAiConfig(it) {} },
                onLoadModels = { config, callback -> viewModel.loadModels(config, callback) },
            )

            is Screen.CategoryDetail -> {
                val category = state.categories.firstOrNull { it.id == screen.categoryId }
                if (category == null) {
                    LaunchedEffect(Unit) { nav.pop() }
                } else {
                    CategoryDetailScreen(
                        category = category,
                        secrets = state.secrets.filter { it.categoryId == category.id },
                        onBack = { nav.pop() },
                        onEdit = { nav.push(Screen.CategoryEdit(category.id)) },
                        onRegenerateIcon = { viewModel.regenerateIcon(category.id) },
                        onOpenSecret = { nav.push(Screen.SecretDetail(it)) },
                        onAddSecret = { nav.push(Screen.SecretDetail(viewModel.createEmptySecret(category.id).id)) },
                        onToggleFavorite = viewModel::toggleFavorite,
                    )
                }
            }

            is Screen.CategoryEdit -> {
                val category = screen.categoryId?.let { id -> state.categories.firstOrNull { it.id == id } }
                CategoryEditScreen(
                    category = category,
                    onBack = { nav.pop() },
                    onSave = { updated ->
                        viewModel.saveCategory(updated)
                        viewModel.showToast("Категория «${updated.name}» сохранена")
                        nav.pop()
                    },
                    onDelete = if (category != null) {
                        { viewModel.deleteCategory(category.id); nav.pop() }
                    } else null,
                    onSearchLogo = { query ->
                        val id = category?.id ?: return@CategoryEditScreen
                        viewModel.resolveIconFor(id, query)
                    },
                )
            }

            is Screen.SecretDetail -> {
                val secret = state.secrets.firstOrNull { it.id == screen.secretId }
                if (secret == null) {
                    LaunchedEffect(Unit) { nav.pop() }
                } else {
                    SecretDetailScreen(
                        secret = secret,
                        category = state.categories.firstOrNull { it.id == secret.categoryId },
                        categories = state.categories,
                        onBack = { nav.pop() },
                        onSave = { viewModel.saveSecret(it); viewModel.showToast("Секрет сохранён") },
                        onDelete = { viewModel.deleteSecret(secret.id); nav.pop() },
                        onToggleFavorite = { viewModel.toggleFavorite(secret.id) },
                        onOpenLink = onOpenLink,
                        onCopy = onCopy,
                    )
                }
            }
        }
    }
}

private fun themeName(id: String): String =
    ru.opencode.safe.ui.theme.ThemeSets.all.firstOrNull { it.id == id }?.name ?: "Аврора"

/** SHA-1 подписи приложения — его требуют при создании Android OAuth-клиента в Google Cloud. */
private fun appSignatureSha1(context: android.content.Context): String? = runCatching {
    val pm = context.packageManager
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        info.signingInfo?.apkContentsSigners
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
    }
    val bytes = signatures?.firstOrNull()?.toByteArray() ?: return null
    java.security.MessageDigest.getInstance("SHA-1").digest(bytes)
        .joinToString(":") { "%02X".format(it) }
}.getOrNull()
