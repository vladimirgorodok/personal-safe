package ru.opencode.safe.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.opencode.safe.R
import ru.opencode.safe.data.model.UserProfile
import ru.opencode.safe.ui.GoogleSupport
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.settings.GOOGLE_CONSENT_URL
import ru.opencode.safe.ui.settings.GoogleSignInCard
import ru.opencode.safe.ui.theme.LocalThemeSet

@Composable
fun AuthScreen(
    profile: UserProfile?,
    googleClientId: String,
    packageName: String,
    signatureSha1: String?,
    support: GoogleSupport,
    error: String?,
    loading: Boolean,
    onGoogle: () -> Unit,
    onDemo: () -> Unit,
    onClientId: (String) -> Unit,
    onClearError: () -> Unit,
    onCopy: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenAccounts: () -> Unit,
) {
    val theme = LocalThemeSet.current
    // Если Client ID ещё не задан — сразу показываем инструкцию: иначе Google-вход
    // выглядит как «нажимаю и ничего не происходит».
    var showConfig by remember { mutableStateOf(googleClientId.isBlank()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.scheme.background, theme.gradient.first().copy(alpha = 0.28f), theme.scheme.background))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_logo), null, modifier = Modifier.size(104.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Личный сейф", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Храните токены, пароли, карты и документы в одном защищённом месте. ИИ-агент распознаёт находки из буфера обмена и раскладывает их по категориям.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureChip(Icons.Rounded.Shield, "Шифрование")
                FeatureChip(Icons.Rounded.AutoAwesome, "ИИ-агент")
                FeatureChip(Icons.Rounded.CloudOff, "Офлайн")
            }

            Spacer(Modifier.height(28.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (profile != null) {
                        Text(
                            "Продолжить как ${profile.name}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }

                    if (!support.available) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Icon(
                                    Icons.Rounded.ErrorOutline,
                                    null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Google Play Services недоступны",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Вход через Google работает через Google Play Services, и без них кнопка ниже не сработает " +
                                            "(${support.playServicesMessage}). Это обычная ситуация на китайских прошивках MIUI/HyperOS. " +
                                            "Доступен гостевой вход.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    Button(
                        onClick = {
                            if (googleClientId.isBlank()) showConfig = true
                            onGoogle()
                        },
                        enabled = !loading && support.available,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1F2430)),
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF1F2430),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Подключение…", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(painterResource(R.drawable.ic_google), null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Войти через Google", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    error?.let {
                        AuthErrorBlock(
                            error = it,
                            onCopy = onCopy,
                            onClear = onClearError,
                            onRetry = onGoogle,
                            onOpenAccounts = onOpenAccounts,
                            onOpenConsent = { onOpenLink(GOOGLE_CONSENT_URL) },
                            onShowConfig = { showConfig = true },
                        )
                    }

                    if (googleClientId.isBlank() && error == null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Client ID не задан, поэтому вход через Google не запускается. Инструкция — в блоке «Настроить Google-вход» ниже.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onDemo, modifier = Modifier.fillMaxWidth()) {
                        Text("Продолжить как гость")
                    }

                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { showConfig = !showConfig; if (showConfig) onClearError() }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showConfig) "Скрыть настройку Google-входа" else "Настроить Google-вход")
                    }

                    if (showConfig) {
                        Spacer(Modifier.height(12.dp))
                        GoogleSignInCard(
                            value = googleClientId,
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

            Spacer(Modifier.height(14.dp))
            Text(
                "Ключи и секреты шифруются ключом устройства. Google-вход настраивается по Client ID в разделе «Оформление».",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Ошибка входа: подсказка сверху, технический текст ниже — его можно скопировать и прислать. */
@Composable
private fun AuthErrorBlock(
    error: String,
    onCopy: (String) -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenConsent: () -> Unit,
    onShowConfig: () -> Unit,
) {
    val parts = error.split("\n\nТехнически: ", limit = 2)
    val noCredentials = error.contains("NoCredential", ignoreCase = true)
    Column(modifier = Modifier.padding(top = 14.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Вход через Google не выполнен",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(parts[0], style = MaterialTheme.typography.bodySmall)
                if (parts.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Технически: ${parts[1]}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (noCredentials) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Что сделать:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onOpenAccounts,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Проверить аккаунты Google на устройстве") }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onOpenConsent,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Добавить мой аккаунт в Test users") }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onShowConfig,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Показать имя пакета и SHA-1 для Android-клиента") }
                }

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Проверить снова") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { onCopy(error) }) { Text("Скопировать текст", maxLines = 1) }
                    TextButton(onClick = onClear) { Text("Скрыть", maxLines = 1) }
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}
