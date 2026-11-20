package ru.opencode.safe.data.settings

enum class DarkMode { SYSTEM, LIGHT, DARK }

data class DisplaySettings(
    val themeSetId: String = "aurora",
    val darkMode: DarkMode = DarkMode.DARK,
    val dynamicColor: Boolean = false,
    val glassEffects: Boolean = true,
    val splashEnabled: Boolean = true,
    val aiSuggestCategory: Boolean = true,
    val googleClientId: String = "",
    val biometricLock: Boolean = false,
    val onboarded: Boolean = false,
)
