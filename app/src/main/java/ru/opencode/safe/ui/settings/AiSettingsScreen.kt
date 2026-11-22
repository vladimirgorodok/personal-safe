package ru.opencode.safe.ui.settings

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.opencode.safe.data.ai.AiDefaults
import ru.opencode.safe.data.ai.AiKeyFormat
import ru.opencode.safe.data.model.AiConfig
import ru.opencode.safe.ui.components.CollapsibleSection
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.data.ai.AiProviders
import ru.opencode.safe.ui.components.ModelPickerDialog
import ru.opencode.safe.ui.components.SafeTopBar

@Composable
fun AiSettingsScreen(
    config: AiConfig,
    onBack: () -> Unit,
    onSave: (AiConfig) -> Unit,
    onTest: (AiConfig) -> Unit,
    onLoadModels: (AiConfig, (Result<List<String>>) -> Unit) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    var providerId by remember(config.providerId) { mutableStateOf(config.providerId) }
    var baseUrl by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var model by remember(config.model) { mutableStateOf(config.model) }
    var visionModel by remember(config.visionModel) { mutableStateOf(config.visionModel) }
    var temperature by remember(config.temperature) { mutableStateOf(config.temperature.toFloat()) }
    var enabled by remember(config.enabled) { mutableStateOf(config.enabled) }
    var apiKeys by remember(config.apiKeys) { mutableStateOf(config.apiKeys) }
    var showKey by remember { mutableStateOf(false) }
    var loadingModels by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf<List<String>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var clipboardNote by remember { mutableStateOf<String?>(null) }
    var clipboardCandidate by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val preset = AiProviders.byId(providerId)
    val presetKeysUrl = preset?.keysUrl.orEmpty()

    // Если ключа ещё нет, но в буфере лежит что-то похожее на ключ — предлагаем вставить.
    LaunchedEffect(Unit) {
        if (apiKey.isBlank()) clipboardCandidate = readKeyFromClipboard(context)
    }

    fun pasteFromClipboard() {
        val found = readKeyFromClipboard(context)
        if (found != null) {
            apiKey = found
            clipboardNote = null
            clipboardCandidate = null
        } else {
            clipboardNote = "В буфере нет текста, похожего на API-ключ. Скопируйте ключ и нажмите снова."
        }
    }

    fun current() = AiConfig(
        providerId = providerId,
        providerName = preset?.name ?: providerId,
        baseUrl = baseUrl.trim(),
        apiKey = apiKey.trim(),
        model = model.trim(),
        visionModel = visionModel.trim(),
        temperature = temperature.toDouble(),
        enabled = enabled,
        apiKeys = apiKeys,
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { onTest(current()) }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                        Text("Проверить")
                    }
                    Button(
                        onClick = { onSave(current()) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("Сохранить", fontWeight = FontWeight.SemiBold) }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SafeTopBar("ИИ-агент", onBack = onBack, subtitle = if (apiKey.isBlank()) "Ключ не подключён" else "Готов к работе") }

            if (apiKey.isBlank()) {
                item {
                    SetupStepsCard(
                        providerName = preset?.name ?: "провайдера",
                        keysUrl = presetKeysUrl,
                        freeHint = preset?.freeHint.orEmpty(),
                        canPaste = clipboardCandidate != null,
                        onOpenKeyPage = { onOpenLink(presetKeysUrl) },
                        onPaste = ::pasteFromClipboard,
                    )
                }
            }

            item {
                CollapsibleSection(
                    title = "Провайдер и модель",
                    subtitle = if (model.isBlank()) "Модель не выбрана" else model,
                    initiallyExpanded = apiKey.isBlank(),
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AiProviders.all.forEach { item ->
                                FilterChip(
                                    selected = providerId == item.id,
                                    onClick = {
                                        val next = AiProviders.switchTo(current(), item.id)
                                        providerId = next.providerId
                                        baseUrl = next.baseUrl
                                        model = next.model
                                        apiKey = next.apiKey
                                        apiKeys = next.apiKeys
                                    },
                                    label = { Text(item.name) },
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text("Base URL (OpenAI-совместимый)") },
                            placeholder = { Text(AiDefaults.BASE_URL) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors(),
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it; clipboardNote = null },
                            label = { Text("API-ключ") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None
                            else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showKey = !showKey }) {
                                    Icon(if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Показать ключ")
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = ::pasteFromClipboard,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.ContentPaste, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Вставить из буфера")
                        }
                        clipboardNote?.let { note ->
                            Spacer(Modifier.height(6.dp))
                            Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                        if (presetKeysUrl.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            TextButton(onClick = { onOpenLink(presetKeysUrl) }) {
                                Text("Получить ключ у ${preset?.name ?: "провайдера"}")
                            }
                        }
                        preset?.freeHint?.takeIf { it.isNotBlank() }?.let { hint ->
                            Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Модель") },
                            placeholder = { Text(AiDefaults.MODEL) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    loadingModels = true
                                    error = null
                                    onLoadModels(current()) { result ->
                                        loadingModels = false
                                        result.onSuccess { models = it }
                                            .onFailure { error = it.message }
                                    }
                                }) {
                                    if (loadingModels) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                    else Icon(Icons.Rounded.ArrowDropDown, "Загрузить список моделей")
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors(),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Стрелка в поле загружает список моделей провайдера — их можно искать по названию.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = visionModel,
                            onValueChange = { visionModel = it },
                            label = { Text("Модель для изображений (необязательно)") },
                            placeholder = { Text("если пусто — используется основная модель") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors(),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Скриншоты распознаёт только модель с поддержкой картинок. В списке моделей такие " +
                                "помечены значком изображения, есть фильтр «С картинками».",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Температура: ${"%.1f".format(temperature)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Slider(value = temperature, onValueChange = { temperature = it }, valueRange = 0f..1f)
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Использовать агента", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Switch(checked = enabled, onCheckedChange = { enabled = it })
                        }
                    }
                }
            }

            item {
                Text(
                    "Ключ хранится только на этом телефоне, вместе с сейфом он шифруется и попадает в резервную " +
                        "копию — на втором устройстве вводить его заново не нужно. Ваши запросы идут напрямую " +
                        "провайдеру, минуя сторонние серверы.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            error?.let { message ->
                item { Text("Ошибка: $message", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    models?.let { list ->
        ModelPickerDialog(
            models = list,
            selected = model,
            onPick = { id -> model = id; models = null },
            onDismiss = { models = null },
        )
    }
}

/** Карточка «подключение в 3 шага», пока ключа нет. */
@Composable
private fun SetupStepsCard(
    providerName: String,
    keysUrl: String,
    freeHint: String,
    canPaste: Boolean,
    onOpenKeyPage: () -> Unit,
    onPaste: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Подключение ИИ: 3 шага", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            SetupStep("1", "Получите бесплатный ключ у $providerName")
            SetupStep("2", "Вставьте его в поле «API-ключ» — кнопка «Вставить из буфера»")
            SetupStep("3", "Нажмите «Проверить», затем «Сохранить»")
            if (keysUrl.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onOpenKeyPage,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Получить ключ", fontWeight = FontWeight.SemiBold) }
            }
            if (canPaste) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onPaste,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.ContentPaste, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Вставить ключ из буфера")
                }
            }
            if (freeHint.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(freeHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SetupStep(number: String, text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Достаёт из буфера значение, похожее на API-ключ. Иначе null. */
private fun readKeyFromClipboard(context: Context): String? {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = manager.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim()
    return text?.takeIf { AiKeyFormat.looksLikeApiKey(it) }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
    focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    unfocusedIndicatorColor = Color.Transparent,
)
