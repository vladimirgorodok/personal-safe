package ru.opencode.safe.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.opencode.safe.data.settings.DarkMode
import ru.opencode.safe.data.settings.DisplaySettings
import ru.opencode.safe.ui.GoogleSupport
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.components.SafeTopBar
import ru.opencode.safe.ui.components.SectionTitle
import ru.opencode.safe.ui.theme.ThemeSet
import ru.opencode.safe.ui.theme.ThemeSets
import kotlin.math.roundToInt

@Composable
fun AppearanceScreen(
    display: DisplaySettings,
    signatureSha1: String?,
    packageName: String,
    support: GoogleSupport?,
    onBack: () -> Unit,
    onTheme: (String) -> Unit,
    onDarkMode: (DarkMode) -> Unit,
    onDynamic: (Boolean) -> Unit,
    onGlass: (Boolean) -> Unit,
    onClientId: (String) -> Unit,
    onCardOpacity: (Float) -> Unit,
    onBackgroundOpacity: (Float) -> Unit,
    onBorderOpacity: (Float) -> Unit,
    onOpenLink: (String) -> Unit,
    onCopy: (String) -> Unit,
) {
    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SafeTopBar("Оформление", onBack = onBack, subtitle = "Темы, прозрачность и вход") }

            item { SectionTitle("Набор тем", Modifier.padding(top = 6.dp)) }
            items(ThemeSets.all.size) { index ->
                val set = ThemeSets.all[index]
                ThemeCard(set, selected = set.id == display.themeSetId) { onTheme(set.id) }
            }

            item { SectionTitle("Режим", Modifier.padding(top = 12.dp)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkMode.entries.forEach { mode ->
                        FilterChip(
                            selected = display.darkMode == mode,
                            onClick = { onDarkMode(mode) },
                            label = { Text(modeLabel(mode)) },
                        )
                    }
                }
            }

            item { SectionTitle("Прозрачность элементов", Modifier.padding(top = 12.dp)) }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        OpacitySlider("Карточки", display.cardOpacity, onCardOpacity)
                        OpacitySlider("Фон", display.backgroundOpacity, onBackgroundOpacity)
                        OpacitySlider("Рамки", display.borderOpacity, onBorderOpacity)
                    }
                }
            }

            item { SectionTitle("Дополнительно", Modifier.padding(top = 12.dp)) }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SwitchLine("Динамические цвета (Android 12+)", display.dynamicColor, onDynamic)
                        SwitchLine("Стеклянные эффекты", display.glassEffects, onGlass)
                    }
                }
            }

            item { SectionTitle("Google-вход", Modifier.padding(top = 12.dp)) }
            item {
                GoogleSignInCard(
                    value = display.googleClientId,
                    onValueChange = onClientId,
                    packageName = packageName,
                    signatureSha1 = signatureSha1,
                    support = support,
                    onCopy = onCopy,
                    onOpenLink = onOpenLink,
                )
            }
        }
    }
}

@Composable
private fun OpacitySlider(label: String, opacity: Float, onChange: (Float) -> Unit) {
    val transparency = (1f - opacity).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(
                "${(transparency * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = transparency,
            onValueChange = { onChange((1f - it).coerceIn(0f, 1f)) },
            valueRange = 0f..1f,
        )
    }
}

@Composable
private fun ThemeCard(set: ThemeSet, selected: Boolean, onClick: () -> Unit) {
    val outline = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    GlassCard(modifier = Modifier.fillMaxWidth().border(1.dp, outline, RoundedCornerShape(24.dp)), onClick = onClick) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(set.gradient)),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier.size(22.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Check, null, tint = set.gradient.first(), modifier = Modifier.size(15.dp)) }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(set.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(set.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                set.gradient.take(3).forEach { color ->
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                }
            }
        }
    }
}

@Composable
private fun SwitchLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun modeLabel(mode: DarkMode) = when (mode) {
    DarkMode.SYSTEM -> "Система"
    DarkMode.LIGHT -> "Светлая"
    DarkMode.DARK -> "Тёмная"
}
