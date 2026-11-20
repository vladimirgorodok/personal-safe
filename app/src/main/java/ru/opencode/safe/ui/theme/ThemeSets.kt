package ru.opencode.safe.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class ThemeSet(
    val id: String,
    val name: String,
    val description: String,
    val gradient: List<Color>,
    val isDark: Boolean,
    val scheme: androidx.compose.material3.ColorScheme,
    val glow: Color,
)

private fun dark(
    primary: Color, secondary: Color, tertiary: Color,
    background: Color, surface: Color, surfaceVariant: Color,
    onSurface: Color = Color(0xFFE9EDFF), glow: Color = primary,
) = darkColorScheme(
    primary = primary,
    onPrimary = Color(0xFF0B0B18),
    primaryContainer = primary.copy(alpha = 0.24f),
    onPrimaryContainer = Color(0xFFEDE9FF),
    secondary = secondary,
    onSecondary = Color(0xFF04140F),
    secondaryContainer = secondary.copy(alpha = 0.22f),
    onSecondaryContainer = Color(0xFFE3FDF8),
    tertiary = tertiary,
    onTertiary = Color(0xFF160B14),
    background = background,
    onBackground = onSurface,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = Color(0xFFB9C1E0),
    outline = Color(0xFF6C7597),
    outlineVariant = Color(0xFF2A3154),
    error = Color(0xFFFF6B81),
    onError = Color(0xFF2A0710),
    errorContainer = Color(0xFF5C1020),
    onErrorContainer = Color(0xFFFFD9DF),
)

private fun light(
    primary: Color, secondary: Color, tertiary: Color,
    background: Color, surface: Color, surfaceVariant: Color,
) = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.16f),
    onPrimaryContainer = primary.copy(alpha = 0.92f),
    secondary = secondary,
    onSecondary = Color.White,
    secondaryContainer = secondary.copy(alpha = 0.16f),
    onSecondaryContainer = secondary.copy(alpha = 0.92f),
    tertiary = tertiary,
    onTertiary = Color.White,
    background = background,
    onBackground = Color(0xFF12162B),
    surface = surface,
    onSurface = Color(0xFF12162B),
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = Color(0xFF4A5270),
    outline = Color(0xFF8A90AC),
    outlineVariant = Color(0xFFD7DBEC),
    error = Color(0xFFD3304A),
    onError = Color.White,
)

private fun set(
    id: String, name: String, description: String,
    darkColors: List<Color>, lightColors: List<Color>,
    gradient: List<Color>, glow: Color,
): Pair<String, Pair<ThemeSet, ThemeSet>> {
    val d = darkColors
    val l = lightColors
    val dark = ThemeSet(id, name, description, gradient, true, dark(d[0], d[1], d[2], d[3], d[4], d[5], glow = glow), glow)
    val lightSet = ThemeSet(id, name, description, gradient, false, light(l[0], l[1], l[2], l[3], l[4], l[5]), glow)
    return id to (dark to lightSet)
}

object ThemeSets {

    private val registry: Map<String, Pair<ThemeSet, ThemeSet>> = listOf(
        set(
            "aurora", "Аврора", "Глубокий космос и северное сияние",
            darkColors = listOf(Color(0xFF8C7BFF), Color(0xFF22D3EE), Color(0xFFFF7AD9), Color(0xFF070B1C), Color(0xFF0F1430), Color(0xFF1B2246)),
            lightColors = listOf(Color(0xFF6A4DE0), Color(0xFF0891B2), Color(0xFFC026D3), Color(0xFFF5F6FF), Color(0xFFFFFFFF), Color(0xFFE7E9F7)),
            gradient = listOf(Color(0xFF7C5CFF), Color(0xFF22D3EE), Color(0xFF2A1250)),
            glow = Color(0xFF7C5CFF),
        ),
        set(
            "ocean", "Океан", "Спокойные волны и глубина",
            darkColors = listOf(Color(0xFF3B82F6), Color(0xFF14B8A6), Color(0xFF38BDF8), Color(0xFF04121F), Color(0xFF0A1B2E), Color(0xFF12314C)),
            lightColors = listOf(Color(0xFF1D6FE0), Color(0xFF0D9488), Color(0xFF0284C7), Color(0xFFF2F8FF), Color(0xFFFFFFFF), Color(0xFFE1ECF8)),
            gradient = listOf(Color(0xFF2563EB), Color(0xFF14B8A6), Color(0xFF062A44)),
            glow = Color(0xFF38BDF8),
        ),
        set(
            "emerald", "Изумруд", "Свежая зелень и мягкий свет",
            darkColors = listOf(Color(0xFF34D399), Color(0xFFA3E635), Color(0xFF2DD4BF), Color(0xFF04140F), Color(0xFF0A1F18), Color(0xFF123528)),
            lightColors = listOf(Color(0xFF0F9D6A), Color(0xFF65A30D), Color(0xFF0D9488), Color(0xFFF3FBF5), Color(0xFFFFFFFF), Color(0xFFDDF3E6)),
            gradient = listOf(Color(0xFF10B981), Color(0xFF84CC16), Color(0xFF083B2C)),
            glow = Color(0xFF34D399),
        ),
        set(
            "sunset", "Закат", "Тёплое золото и розовый горизонт",
            darkColors = listOf(Color(0xFFFB923C), Color(0xFFF472B6), Color(0xFFFACC15), Color(0xFF1A0C07), Color(0xFF241109), Color(0xFF3A1A12)),
            lightColors = listOf(Color(0xFFEA580C), Color(0xFFDB2777), Color(0xFFCA8A04), Color(0xFFFFF7F0), Color(0xFFFFFFFF), Color(0xFFFBE4D5)),
            gradient = listOf(Color(0xFFF97316), Color(0xFFEC4899), Color(0xFF7C2D12)),
            glow = Color(0xFFFB923C),
        ),
        set(
            "rose", "Роза", "Нежная пастель и ягодные акценты",
            darkColors = listOf(Color(0xFFF472B6), Color(0xFFC084FC), Color(0xFFFDA4AF), Color(0xFF170A12), Color(0xFF22101C), Color(0xFF3A1A2C)),
            lightColors = listOf(Color(0xFFDB2777), Color(0xFF9333EA), Color(0xFFE11D48), Color(0xFFFFF5F9), Color(0xFFFFFFFF), Color(0xFFF8DEE9)),
            gradient = listOf(Color(0xFFEC4899), Color(0xFFA855F7), Color(0xFF4C1D3A)),
            glow = Color(0xFFF472B6),
        ),
        set(
            "graphite", "Графит", "Строгий минимализм и сталь",
            darkColors = listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8), Color(0xFFE2E8F0), Color(0xFF0B0D12), Color(0xFF141821), Color(0xFF232A38)),
            lightColors = listOf(Color(0xFF334155), Color(0xFF64748B), Color(0xFF0F172A), Color(0xFFF6F7F9), Color(0xFFFFFFFF), Color(0xFFE2E8F0)),
            gradient = listOf(Color(0xFF94A3B8), Color(0xFF475569), Color(0xFF0F172A)),
            glow = Color(0xFFCBD5E1),
        ),
        set(
            "cyber", "Кибер", "Неон, терминал и матрица",
            darkColors = listOf(Color(0xFF22FF88), Color(0xFF00E5FF), Color(0xFFB4FF39), Color(0xFF03070A), Color(0xFF06121A), Color(0xFF0C2230)),
            lightColors = listOf(Color(0xFF059669), Color(0xFF0891B2), Color(0xFF65A30D), Color(0xFFF0FDF9), Color(0xFFFFFFFF), Color(0xFFD6F5EA)),
            gradient = listOf(Color(0xFF22FF88), Color(0xFF00E5FF), Color(0xFF052A22)),
            glow = Color(0xFF22FF88),
        ),
        set(
            "lavender", "Лаванда", "Мягкий фиолетовый туман",
            darkColors = listOf(Color(0xFFC4B5FD), Color(0xFFA5B4FC), Color(0xFFF0ABFC), Color(0xFF0D0A1A), Color(0xFF161128), Color(0xFF26203F)),
            lightColors = listOf(Color(0xFF7C3AED), Color(0xFF4F46E5), Color(0xFFDB2777), Color(0xFFF8F6FF), Color(0xFFFFFFFF), Color(0xFFEAE4FB)),
            gradient = listOf(Color(0xFFA78BFA), Color(0xFF818CF8), Color(0xFF2E1065)),
            glow = Color(0xFFC4B5FD),
        ),
    ).toMap()

    val all: List<ThemeSet> = registry.values.map { it.first }

    fun light(id: String): ThemeSet = (registry[id] ?: registry.getValue("aurora")).second
    fun dark(id: String): ThemeSet = (registry[id] ?: registry.getValue("aurora")).first

    fun byId(id: String, dark: Boolean): ThemeSet = if (dark) dark(id) else light(id)
}
