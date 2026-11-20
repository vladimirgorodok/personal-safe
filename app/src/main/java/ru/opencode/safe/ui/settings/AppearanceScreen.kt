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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.opencode.safe.data.settings.DarkMode
import ru.opencode.safe.data.settings.DisplaySettings
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.components.SafeTopBar
import ru.opencode.safe.ui.components.SectionTitle
import ru.opencode.safe.ui.theme.ThemeSet
import ru.opencode.safe.ui.theme.ThemeSets
import kotlin.math.roundToInt

private const val GOOGLE_CREDENTIALS_URL = "https://console.cloud.google.com/apis/credentials"

@Composable
fun AppearanceScreen(
    display: DisplaySettings,
    signatureSha1: String?,
    packageName: String,
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
    var clientId by remember { mutableStateOf(display.googleClientId) }

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
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Вход через Google включается двумя записями в Google Cloud Console. Имя пакета и SHA-1 подписи нужны для Android-записи (по ним Google проверяет, что запрос идёт из вашего приложения), а Client ID вы копируете из веб-записи.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(Modifier.height(16.dp))
                        StepHeader("Шаг 1", "Android-клиент — сюда вставляются имя пакета и SHA-1")
                        InstructionStep(1, "Слева вверху нажмите «+ Create credentials» → «OAuth client ID».")
                        InstructionStep(2, "В поле «Application type» выберите «Android».")
                        InstructionStep(3, "Заполните два поля формы значениями ниже — они называются точно так же.")
                        Spacer(Modifier.height(6.dp))
                        CopyInfoRow(
                            label = "Имя пакета",
                            target = "вставьте в поле «Package name»",
                            value = packageName,
                            onCopy = onCopy,
                        )
                        Spacer(Modifier.height(8.dp))
                        CopyInfoRow(
                            label = "SHA-1 подписи",
                            target = "вставьте в поле «SHA-1 certificate fingerprint»",
                            value = signatureSha1 ?: "недоступно",
                            onCopy = onCopy,
                        )
                        Spacer(Modifier.height(8.dp))
                        InstructionStep(4, "Нажмите «Create». Client ID этой записи приложению не нужен — ничего не копируйте.")

                        Spacer(Modifier.height(16.dp))
                        StepHeader("Шаг 2", "Веб-клиент — из него берётся Client ID для приложения")
                        InstructionStep(1, "Снова «+ Create credentials» → «OAuth client ID».")
                        InstructionStep(2, "В поле «Application type» выберите «Web application» и нажмите «Create».")
                        InstructionStep(3, "В списке записей нажмите на созданную веб-запись и скопируйте её «Client ID» (вида 1234567890-abc.apps.googleusercontent.com).")
                        InstructionStep(4, "Вставьте его в поле ниже — он применится при следующем входе.")

                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { onOpenLink(GOOGLE_CREDENTIALS_URL) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Открыть Google Cloud Console")
                        }

                        Spacer(Modifier.height(16.dp))
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
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "SHA-1 показан для установленной сборки. Если поставите другую сборку (например, debug), подпись будет другой — возьмите SHA-1 из неё.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
private fun InstructionStep(number: Int, text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StepHeader(step: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                step,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun CopyInfoRow(
    label: String,
    target: String,
    value: String,
    onCopy: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    target,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { onCopy(value) }) { Text("Копировать") }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.primary,
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
