package ru.opencode.safe.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "safe_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_SET = stringPreferencesKey("theme_set")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val GLASS = booleanPreferencesKey("glass_effects")
        val CARD_OPACITY = floatPreferencesKey("card_opacity")
        val BACKGROUND_OPACITY = floatPreferencesKey("background_opacity")
        val BORDER_OPACITY = floatPreferencesKey("border_opacity")
        val SPLASH = booleanPreferencesKey("splash_enabled")
        val AI_SUGGEST = booleanPreferencesKey("ai_suggest_category")
        val CLIENT_ID = stringPreferencesKey("google_client_id")
        val BIOMETRIC = booleanPreferencesKey("biometric_lock")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }

    val settings: Flow<DisplaySettings> = context.settingsDataStore.data.map { p ->
        DisplaySettings(
            themeSetId = p[Keys.THEME_SET] ?: "aurora",
            darkMode = runCatching { DarkMode.valueOf(p[Keys.DARK_MODE] ?: "DARK") }.getOrDefault(DarkMode.DARK),
            dynamicColor = p[Keys.DYNAMIC] ?: false,
            glassEffects = p[Keys.GLASS] ?: true,
            cardOpacity = p[Keys.CARD_OPACITY] ?: 1f,
            backgroundOpacity = p[Keys.BACKGROUND_OPACITY] ?: 1f,
            borderOpacity = p[Keys.BORDER_OPACITY] ?: 1f,
            splashEnabled = p[Keys.SPLASH] ?: true,
            aiSuggestCategory = p[Keys.AI_SUGGEST] ?: true,
            googleClientId = p[Keys.CLIENT_ID] ?: "",
            biometricLock = p[Keys.BIOMETRIC] ?: false,
            onboarded = p[Keys.ONBOARDED] ?: false,
        )
    }

    suspend fun setThemeSet(id: String) = context.settingsDataStore.edit { it[Keys.THEME_SET] = id }
    suspend fun setDarkMode(mode: DarkMode) = context.settingsDataStore.edit { it[Keys.DARK_MODE] = mode.name }
    suspend fun setDynamicColor(value: Boolean) = context.settingsDataStore.edit { it[Keys.DYNAMIC] = value }
    suspend fun setGlassEffects(value: Boolean) = context.settingsDataStore.edit { it[Keys.GLASS] = value }
    suspend fun setCardOpacity(value: Float) = context.settingsDataStore.edit { it[Keys.CARD_OPACITY] = value.coerceIn(0f, 1f) }
    suspend fun setBackgroundOpacity(value: Float) = context.settingsDataStore.edit { it[Keys.BACKGROUND_OPACITY] = value.coerceIn(0f, 1f) }
    suspend fun setBorderOpacity(value: Float) = context.settingsDataStore.edit { it[Keys.BORDER_OPACITY] = value.coerceIn(0f, 1f) }
    suspend fun setSplashEnabled(value: Boolean) = context.settingsDataStore.edit { it[Keys.SPLASH] = value }
    suspend fun setAiSuggest(value: Boolean) = context.settingsDataStore.edit { it[Keys.AI_SUGGEST] = value }
    suspend fun setGoogleClientId(value: String) = context.settingsDataStore.edit { it[Keys.CLIENT_ID] = value }
    suspend fun setBiometric(value: Boolean) = context.settingsDataStore.edit { it[Keys.BIOMETRIC] = value }
    suspend fun setOnboarded(value: Boolean) = context.settingsDataStore.edit { it[Keys.ONBOARDED] = value }
}
