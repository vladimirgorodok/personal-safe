package ru.opencode.safe.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import ru.opencode.safe.data.settings.DarkMode

val LocalThemeSet = staticCompositionLocalOf { ThemeSets.dark("aurora") }

fun isDarkNow(mode: DarkMode): Boolean = when (mode) {
    DarkMode.SYSTEM -> false
    DarkMode.LIGHT -> false
    DarkMode.DARK -> true
}

@Composable
fun resolveDark(mode: DarkMode): Boolean = when (mode) {
    DarkMode.SYSTEM -> isSystemInDarkTheme()
    DarkMode.LIGHT -> false
    DarkMode.DARK -> true
}

@Composable
fun SafeTheme(
    themeSetId: String,
    darkMode: DarkMode,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = resolveDark(darkMode)
    val selected = ThemeSets.byId(themeSetId, dark)
    val context = LocalContext.current
    val scheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        selected.scheme
    }
    CompositionLocalProvider(LocalThemeSet provides selected) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** Фоновый градиент текущей темы (для заголовков, заставки, карточек). */
fun ThemeSet.backgroundBrush(dark: Boolean): Brush {
    val base = scheme.background
    return if (dark) {
        Brush.linearGradient(
            listOf(
                base,
                gradient.getOrNull(0)?.copy(alpha = 0.18f) ?: base,
                gradient.lastOrNull()?.copy(alpha = 0.24f) ?: base,
                base,
            )
        )
    } else {
        Brush.linearGradient(listOf(base, gradient.getOrNull(0)?.copy(alpha = 0.10f) ?: base, base))
    }
}

fun accentBrush(themeSet: ThemeSet): Brush = Brush.linearGradient(themeSet.gradient)

val SafeGlassStroke: Color
    get() = Color.White.copy(alpha = 0.18f)
