package ru.opencode.safe.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import ru.opencode.safe.data.backup.BackupCrypto
import ru.opencode.safe.data.backup.DriveErrors
import ru.opencode.safe.ui.BackupState
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.components.SafeTopBar
import ru.opencode.safe.ui.drive.DriveAuth

/**
 * Резервная копия сейфа в Google Drive. Копия шифруется фразой ниже и лежит в скрытой
 * папке приложения, поэтому прочитать её может только «Личный сейф» с этой фразой.
 */
@Composable
fun BackupScreen(
    state: BackupState,
    accountEmail: String?,
    activity: Activity?,
    onBack: () -> Unit,
    onRefresh: (String) -> Unit,
    onBackup: (String, String) -> Unit,
    onRestore: (String, String) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<CompletableDeferred<Intent>?>(null) }

    val resolver = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val deferred = pending
        pending = null
        if (deferred != null) {
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                deferred.complete(data)
            } else {
                deferred.completeExceptionally(IllegalStateException("Доступ к Google Drive не предоставлен"))
            }
        }
    }

    // Каждый запрос к Drive начинается с получения токена; при необходимости показываем экран согласия.
    val withToken: ((String) -> Unit) -> Unit = { action ->
        val act = activity
        if (act == null) {
            localError = "Нет доступа к окну приложения — закройте и откройте приложение заново"
        } else {
            localError = null
            scope.launch {
                val token = runCatching {
                    try {
                        DriveAuth.requestToken(act, accountEmail)
                    } catch (approval: DriveAuth.ApprovalRequired) {
                        val deferred = CompletableDeferred<Intent>()
                        pending = deferred
                        resolver.launch(
                            IntentSenderRequest.Builder(approval.pendingIntent.intentSender).build(),
                        )
                        DriveAuth.tokenFromIntent(act, deferred.await())
                    }
                }
                token.onSuccess(action).onFailure { e ->
                    localError = e.message ?: "Не удалось получить доступ к Google Drive"
                }
            }
        }
    }

    LaunchedEffect(Unit) { withToken { onRefresh(it) } }

    val tooShort = passphrase.isNotEmpty() && passphrase.length < BackupCrypto.MIN_PASSPHRASE
    val mismatch = confirm.isNotEmpty() && confirm != passphrase
    val ready = passphrase.length >= BackupCrypto.MIN_PASSPHRASE && passphrase == confirm

    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SafeTopBar(
                    "Резервная копия",
                    onBack = onBack,
                    subtitle = "Google Drive",
                    actions = {
                        if (state.running) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { withToken { onRefresh(it) } }) {
                                Icon(Icons.Rounded.Refresh, "Обновить")
                            }
                        }
                    },
                )
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    accountEmail?.takeIf { it.isNotBlank() } ?: "Google-аккаунт",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "Копия лежит в скрытой папке приложения — в «Моём диске» её не видно.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (state.hasRemote) {
                                "Копия есть на Google Диске" +
                                    (state.remoteModified?.let { " · ${it.take(16).replace('T', ' ')}" } ?: "")
                            } else {
                                "Копий пока нет"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Фраза для шифрования", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Копия шифруется этой фразой прямо на телефоне. Запомните её: без неё восстановить " +
                                "данные на новом устройстве нельзя, и восстановить фразу мы тоже не сможем.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = passphrase,
                            onValueChange = { passphrase = it },
                            label = { Text("Фраза") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = tooShort,
                            supportingText = {
                                if (tooShort) Text("Минимум ${BackupCrypto.MIN_PASSPHRASE} символов")
                            },
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
                        OutlinedTextField(
                            value = confirm,
                            onValueChange = { confirm = it },
                            label = { Text("Повторите фразу") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = mismatch,
                            supportingText = { if (mismatch) Text("Фразы не совпадают") },
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

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { withToken { token -> onBackup(token, passphrase) } },
                        enabled = ready && !state.running,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Rounded.CloudUpload, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Сохранить в Google Drive", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { withToken { token -> onRestore(token, passphrase) } },
                        enabled = ready && !state.running,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Rounded.CloudDownload, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Восстановить из Google Drive")
                    }
                    Text(
                        "Восстановление заменяет текущие данные тем, что лежит в копии.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val error = localError ?: state.error
            error?.let {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row {
                                Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                            if (DriveErrors.needsApiEnable(it)) {
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { onOpenLink(DriveErrors.DRIVE_API_URL) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                ) { Text("Включить Google Drive API") }
                            }
                        }
                    }
                }
            }

            state.message?.let {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}
