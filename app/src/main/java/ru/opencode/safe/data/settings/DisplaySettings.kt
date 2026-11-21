package ru.opencode.safe.data.settings

enum class DarkMode { SYSTEM, LIGHT, DARK }

data class DisplaySettings(
    val themeSetId: String = "aurora",
    val darkMode: DarkMode = DarkMode.DARK,
    val dynamicColor: Boolean = false,
    val glassEffects: Boolean = true,
    /** 1f — как задумано темой, 0f — полностью прозрачные карточки. */
    val cardOpacity: Float = 1f,
    /** Насыщенность фонового градиента: 1f — полная, 0f — только базовый цвет. */
    val backgroundOpacity: Float = 1f,
    /** Прозрачность рамок и разделителей. */
    val borderOpacity: Float = 1f,
    val splashEnabled: Boolean = true,
    val aiSuggestCategory: Boolean = true,
    val googleClientId: String = "",
    val biometricLock: Boolean = false,
    /** Не спрашивать Google-аккаунт при каждом запуске — входить в сохранённый профиль. */
    val rememberSignIn: Boolean = true,
    val onboarded: Boolean = false,
)
