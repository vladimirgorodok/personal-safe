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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.opencode.safe.data.settings.DarkMode
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.components.SafeTopBar
import ru.opencode.safe.ui.components.SectionTitle
import ru.opencode.safe.ui.theme.ThemeSet
import ru.opencode.safe.ui.theme.ThemeSets

@Composable
fun AppearanceScreen(
    selectedThemeId: String,
    darkMode: DarkMode,
    dynamicColor: Boolean,
    glassEffects: Boolean,
    googleClientId: String,
    onBack: () -> Unit,
    onTheme: (String) -> Unit,
    onDarkMode: (DarkMode) -> Unit,
    onDynamic: (Boolean) -> Unit,
    onGlass: (Boolean) -> Unit,
    onClientId: (String) -> Unit,
) {
    var clientId by remember { mutableStateOf(googleClientId) }

    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SafeTopBar("Оформление", onBack = onBack, subtitle = "Темы, цвета и вход") }

            item { SectionTitle("Набор тем", Modifier.padding(top = 6.dp)) }
            items(ThemeSets.all.size) { index ->
                val set = ThemeSets.all[index]
                ThemeCard(set, selected = set.id == selectedThemeId) { onTheme(set.id) }
            }

            item { SectionTitle("Режим", Modifier.padding(top = 12.dp)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkMode.entries.forEach { mode ->
                        FilterChip(
                            selected = darkMode == mode,
                            onClick = { onDarkMode(mode) },
                            label = { Text(modeLabel(mode)) },
                        )
                    }
                }
            }

            item { SectionTitle("Дополнительно", Modifier.padding(top = 12.dp)) }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SwitchLine("Динамические цвета (Android 12+)", dynamicColor, onDynamic)
                        SwitchLine("Стеклянные эффекты", glassEffects, onGlass)
                    }
                }
            }

            item { SectionTitle("Google-вход", Modifier.padding(top = 12.dp)) }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Укажите OAuth Client ID (web), чтобы включить настоящий вход через Google. Без него работает демо-профиль.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = clientId,
                            onValueChange = { clientId = it; onClientId(it) },
                            label = { Text("Google OAuth Client ID") },
                            placeholder = { Text("1234567890-abc.apps.googleusercontent.com") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(set: ThemeSet, selected: Boolean, onClick: () -> Unit) {
    val outline = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
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
