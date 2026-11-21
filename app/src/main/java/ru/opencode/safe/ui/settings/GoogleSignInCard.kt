package ru.opencode.safe.ui.settings

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.opencode.safe.ui.GoogleSupport
import ru.opencode.safe.ui.components.GlassCard

const val GOOGLE_CREDENTIALS_URL = "https://console.cloud.google.com/apis/credentials"
private const val CLIENT_ID_SUFFIX = ".apps.googleusercontent.com"

/**
 * Настройка входа через Google: два шага, куда что вставлять, поле Client ID
 * с вставкой из буфера и диагностика устройства.
 */
@Composable
fun GoogleSignInCard(
    value: String,
    onValueChange: (String) -> Unit,
    packageName: String,
    signatureSha1: String?,
    support: GoogleSupport?,
    onCopy: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pasteFailed by remember { mutableStateOf(false) }
    val looksLikeClientId = value.isBlank() || value.trim().endsWith(CLIENT_ID_SUFFIX)

    GlassCard(modifier = modifier.fillMaxWidth()) {
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
            InstructionStep(3, "В списке записей нажмите на созданную веб-запись и скопируйте её «Client ID».")
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
                value = value,
                onValueChange = { onValueChange(it); pasteFailed = false },
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
            Row {
                TextButton(onClick = {
                    val text = pasteFromClipboard(context)
                    if (text.isNullOrBlank()) {
                        pasteFailed = true
                    } else {
                        onValueChange(text)
                        pasteFailed = false
                    }
                }) {
                    Text("Вставить из буфера")
                }
            }
            if (pasteFailed) {
                Text(
                    "В буфере обмена нет текста. Скопируйте Client ID в браузере и нажмите ещё раз.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (!looksLikeClientId) {
                Text(
                    "Не похоже на Client ID: обычно он заканчивается на $CLIENT_ID_SUFFIX. Проверьте, что скопирован именно Client ID веб-записи.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "SHA-1 показан для установленной сборки. Если поставите другую сборку (например, debug), подпись будет другой — возьмите SHA-1 из неё.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (support != null) {
                Spacer(Modifier.height(18.dp))
                Text("Диагностика", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                DiagRow(
                    ok = support.available,
                    label = "Google Play Services",
                    value = if (support.available) {
                        "доступны" + (support.playServicesVersion?.let { " (версия $it)" } ?: "")
                    } else {
                        "недоступны: ${support.playServicesMessage}"
                    },
                )
                Spacer(Modifier.height(6.dp))
                DiagRow(
                    ok = value.isNotBlank() && looksLikeClientId,
                    label = "Client ID",
                    value = when {
                        value.isBlank() -> "не задан — вход через Google невозможен"
                        looksLikeClientId -> "задан"
                        else -> "задан, но формат не похож на Client ID"
                    },
                )
            }
        }
    }
}

private fun pasteFromClipboard(context: Context): String? {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = manager.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()?.trim()
}

@Composable
private fun DiagRow(ok: Boolean, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            null,
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            Text(
                value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
