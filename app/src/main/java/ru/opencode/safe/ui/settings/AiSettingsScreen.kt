package ru.opencode.safe.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.opencode.safe.data.ai.AiDefaults
import ru.opencode.safe.data.model.AiConfig
import ru.opencode.safe.ui.components.CollapsibleSection
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.components.SafeTopBar

private data class ProviderPreset(val id: String, val name: String, val baseUrl: String, val model: String)

private val presets = listOf(
    ProviderPreset(AiDefaults.PROVIDER_ID, "Groq — бесплатно", AiDefaults.BASE_URL, AiDefaults.MODEL),
    ProviderPreset("openrouter", "OpenRouter", "https://openrouter.ai/api/v1", "google/gemma-4-31b-it:free"),
    ProviderPreset("gemini", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-4-hydrogen"),
    ProviderPreset("aitunnel", "aitunnel", "https://api.aitunnel.ru/v1", "gpt-4o-mini"),
    ProviderPreset("openai", "OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
    ProviderPreset("custom", "Другой", "", ""),
)

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
    var showKey by remember { mutableStateOf(false) }
    var loadingModels by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf<List<String>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun current() = AiConfig(
        providerId = providerId,
        providerName = presets.firstOrNull { it.id == providerId }?.name ?: providerId,
        baseUrl = baseUrl.trim(),
        apiKey = apiKey.trim(),
        model = model.trim(),
        visionModel = visionModel.trim(),
        temperature = temperature.toDouble(),
        enabled = enabled,
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
            item { SafeTopBar("ИИ-агент", onBack = onBack, subtitle = "Провайдер и модель") }

            item {
                CollapsibleSection(
                    title = "Провайдер и модель",
                    subtitle = if (model.isBlank()) "Модель не выбрана" else model,
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            presets.forEach { preset ->
                                FilterChip(
                                    selected = providerId == preset.id,
                                    onClick = {
                                        providerId = preset.id
                                        if (preset.id != "custom") {
                                            baseUrl = preset.baseUrl
                                            model = preset.model
                                        }
                                    },
                                    label = { Text(preset.name) },
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
                            onValueChange = { apiKey = it },
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
                        if (apiKey.isBlank()) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onOpenLink(AiDefaults.KEYS_URL) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text("Получить бесплатный ключ") }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Ключ выдаётся бесплатно, без карты: лимит 1000 запросов в сутки и распознавание " +
                                    "изображений. Скопируйте ключ и вставьте в поле выше — агент заработает.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
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
                            "Нажмите на стрелку, чтобы загрузить список доступных моделей.",
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
                    "По умолчанию подставлена бесплатная модель. Если её лимит на сутки закончится, агент " +
                        "честно скажет об этом и продолжит работать после обновления лимита — либо переключите " +
                        "модель на другую бесплатную, список подгружается стрелкой в поле «Модель».",
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

/**
 * Выбор модели из списка провайдера. Список бывает длинным, поэтому есть поиск,
 * а строки выбираются нажатием (текущая модель помечена галочкой).
 */
@Composable
private fun ModelPickerDialog(
    models: List<String>,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(models, query) {
        if (query.isBlank()) models else models.filter { it.contains(query.trim(), ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Модели провайдера") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Поиск модели") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Text(
                        if (models.isEmpty()) "Список пуст" else "Ничего не найдено",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(filtered.size) { index ->
                            val id = filtered[index]
                            val isSelected = id.equals(selected, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPick(id) }
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    id,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        "Выбрано",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
    focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    unfocusedIndicatorColor = Color.Transparent,
)
