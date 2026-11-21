package ru.opencode.safe.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.opencode.safe.BuildConfig
import ru.opencode.safe.data.settings.DisplaySettings
import ru.opencode.safe.data.model.UserProfile
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.components.ProfileAvatar
import ru.opencode.safe.ui.components.SafeTopBar
import ru.opencode.safe.ui.components.SectionTitle

@Composable
fun SettingsScreen(
    profile: UserProfile?,
    display: DisplaySettings,
    themeName: String,
    aiConfigured: Boolean,
    onBack: () -> Unit,
    onAppearance: () -> Unit,
    onAiSettings: () -> Unit,
    onSplash: (Boolean) -> Unit,
    onGlass: (Boolean) -> Unit,
    onAiSuggest: (Boolean) -> Unit,
    onBiometric: (Boolean) -> Unit,
    onSignOut: () -> Unit,
) {
    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SafeTopBar("Настройки", onBack = onBack) }

            item { ProfileCard(profile, onSignOut) }

            item { SectionTitle("Внешний вид", Modifier.padding(top = 6.dp)) }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onAppearance) {
                    SettingRow(Icons.Rounded.Palette, "Оформление и темы", "Сейчас: $themeName")
                }
            }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SwitchRow(Icons.Rounded.Shield, "Стеклянные эффекты", "Полупрозрачные карточки и блики", display.glassEffects, onGlass)
                        SwitchRow(Icons.Rounded.Info, "Заставка при запуске", "Анимация на 5 секунд", display.splashEnabled, onSplash)
                    }
                }
            }

            item { SectionTitle("ИИ-агент", Modifier.padding(top = 6.dp)) }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onAiSettings) {
                    SettingRow(
                        Icons.Rounded.AutoAwesome,
                        "Провайдер и модель",
                        if (aiConfigured) "Подключён" else "Не настроен — офлайн-режим",
                    )
                }
            }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SwitchRow(Icons.Rounded.AutoAwesome, "Предлагать новые категории", "Агент предлагает создать категорию под бренд", display.aiSuggestCategory, onAiSuggest)
                }
            }

            item { SectionTitle("Безопасность", Modifier.padding(top = 6.dp)) }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SwitchRow(Icons.Rounded.Fingerprint, "Вход по биометрии", "Отпечаток или лицо при запуске", display.biometricLock, onBiometric)
                }
            }

            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Личный сейф ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Секреты хранятся только на устройстве в зашифрованном виде. Ключ шифрования — в Android Keystore.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: UserProfile?, onSignOut: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(profile, 44.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile?.name ?: "Гость", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (profile?.isDemo == false) "Аккаунт Google" else "Локальный профиль",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Rounded.ExpandMore,
                    if (expanded) "Свернуть" else "Подробнее",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        profile?.email?.takeIf { it.isNotBlank() } ?: "локальный профиль",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Выйти из аккаунта")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
