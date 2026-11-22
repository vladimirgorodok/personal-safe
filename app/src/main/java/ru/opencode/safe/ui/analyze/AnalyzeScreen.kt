package ru.opencode.safe.ui.analyze

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.opencode.safe.data.ai.AgentSource
import ru.opencode.safe.data.model.AiConfig
import ru.opencode.safe.data.model.Category
import ru.opencode.safe.ui.AnalyzeState
import ru.opencode.safe.ui.components.CategoryAvatar
import ru.opencode.safe.ui.components.DetailExpander
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.components.ModelPickerDialog
import ru.opencode.safe.ui.components.SafeTopBar
import ru.opencode.safe.ui.components.TypeBadge

@Composable
fun AnalyzeScreen(
    state: AnalyzeState,
    categories: List<Category>,
    ai: AiConfig,
    autoPaste: Boolean,
    onInput: (String) -> Unit,
    onPasteClipboard: () -> Unit,
    onPickedUri: (Uri) -> Unit,
    onClearAttachments: () -> Unit,
    onClear: () -> Unit,
    onRun: () -> Unit,
    onSave: (categoryId: String?, newCategoryName: String?) -> Unit,
    onOpenLink: (String) -> Unit,
    onCopy: (String) -> Unit,
    onOpenAiSettings: () -> Unit,
    onLoadModels: ((Result<List<String>>) -> Unit) -> Unit,
    onPickModel: (String) -> Unit,
    onBack: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(onPickedUri)
    }

    val aiConfigured = ai.isConfigured && ai.enabled
    var showPicker by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf<List<String>?>(null) }
    var modelsLoading by remember { mutableStateOf(false) }
    var modelsError by remember { mutableStateOf<String?>(null) }

    fun openModelPicker() {
        showPicker = true
        if (models == null && !modelsLoading) {
            modelsLoading = true
            modelsError = null
            onLoadModels { result ->
                modelsLoading = false
                result
                    .onSuccess { models = it }
                    .onFailure { modelsError = it.message ?: "Не удалось получить список" }
            }
        }
    }

    val hasData = state.input.isNotBlank() || state.images.isNotEmpty() ||
        state.imageUrl != null || state.result != null || state.error != null

    var selectedCategory by remember(state.result) { mutableStateOf(state.result?.matchedCategoryId) }
    var title by remember(state.result) { mutableStateOf(state.result?.title.orEmpty()) }
    var description by remember(state.result) { mutableStateOf(state.result?.description.orEmpty()) }
    var note by remember(state.result) { mutableStateOf(state.result?.note.orEmpty()) }

    LaunchedEffect(autoPaste) {
        if (autoPaste && state.input.isBlank() && state.images.isEmpty() && state.imageUrl.isNullOrBlank()) {
            onPasteClipboard()
        }
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SafeTopBar(
                    title = "Анализ находки",
                    subtitle = "Текст, файл или ссылка на изображение",
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = onClear, enabled = hasData) {
                            Icon(Icons.Rounded.DeleteSweep, "Очистить данные анализа")
                        }
                        IconButton(onClick = onOpenAiSettings) { Icon(Icons.Rounded.Settings, "Настройки ИИ") }
                    },
                )
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { if (aiConfigured) openModelPicker() else onOpenAiSettings() },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            null,
                            tint = if (aiConfigured) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (aiConfigured) "Модель ИИ" else "ИИ не настроен",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                if (aiConfigured) {
                                    val provider = ai.providerName.ifBlank { "Провайдер" }
                                    val modelName = ai.model.ifBlank { "модель не выбрана" }
                                    "$provider · $modelName"
                                } else {
                                    "Офлайн-режим — нажмите, чтобы подключить"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Rounded.ArrowDropDown,
                            if (aiConfigured) "Сменить модель" else "Настроить ИИ",
                        )
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        OutlinedTextField(
                            value = state.input,
                            onValueChange = onInput,
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 5,
                            placeholder = { Text("Вставьте токен, пароль, реквизиты или ссылку на изображение…") },
                            trailingIcon = {
                                IconButton(onClick = onPasteClipboard) { Icon(Icons.Rounded.ContentPaste, "Вставить из буфера") }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = onPasteClipboard, shape = RoundedCornerShape(14.dp)) {
                                Icon(Icons.Rounded.ContentPaste, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Из буфера")
                            }
                            OutlinedButton(onClick = { picker.launch("*/*") }, shape = RoundedCornerShape(14.dp)) {
                                Icon(Icons.Rounded.AttachFile, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Файл")
                            }
                            OutlinedButton(onClick = { picker.launch("image/*") }, shape = RoundedCornerShape(14.dp)) {
                                Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            if (state.images.isNotEmpty() || state.imageUrl != null) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    state.attachmentName ?: state.imageUrl ?: "Изображение",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = onClearAttachments) { Icon(Icons.Rounded.Close, "Убрать") }
                            }
                            state.images.firstOrNull()?.let { bytes ->
                                val bitmap = remember(bytes) {
                                    runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
                                }
                                if (bitmap != null) {
                                    Spacer(Modifier.height(10.dp))
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onRun,
                    enabled = !state.running,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    if (state.running) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(12.dp))
                        Text(state.status ?: "Агент работает…")
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, null)
                        Spacer(Modifier.width(10.dp))
                        Text("Проанализировать", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onClear,
                    enabled = hasData && !state.running,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.DeleteSweep, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Очистить данные анализа")
                }
            }

            state.error?.let { error ->
                item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp)) {
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp))
                    }
                }
            }

            state.result?.let { result ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Результат агента", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(
                                    if (result.source == AgentSource.AI) "ИИ · ${(result.confidence * 100).toInt()}%" else "офлайн · ${(result.confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TypeBadge(result.type)
                                result.brandName?.let {
                                    Spacer(Modifier.width(8.dp))
                                    Text("Бренд: $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            SmallField("Название", title) { title = it }
                            Spacer(Modifier.height(10.dp))
                            SmallField("Описание", description) { description = it }
                            Spacer(Modifier.height(10.dp))
                            SmallField("Примечание", note) { note = it }
                        }
                    }
                }

                item {
                    Column {
                        Text("Категория", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (result.isNewCategory) {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    leadingIcon = { Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(16.dp)) },
                                    label = { Text("Создать «${result.categoryName}»") },
                                )
                            }
                            categories.forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category.id,
                                    onClick = { selectedCategory = category.id },
                                    leadingIcon = { CategoryAvatar(category, 20.dp) },
                                    label = { Text(category.name) },
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            when {
                                selectedCategory != null -> "Будет добавлено в «${categories.firstOrNull { it.id == selectedCategory }?.name ?: ""}»"
                                result.isNewCategory -> "Агент предлагает создать новую категорию «${result.categoryName}»"
                                else -> "Выберите категорию"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    DetailExpander(
                        title = "Подробное описание от агента",
                        text = result.detailedInfo,
                        links = result.links,
                        initiallyExpanded = true,
                        onOpen = onOpenLink,
                    )
                }

                if (result.warnings.isNotEmpty()) {
                    item {
                        result.warnings.forEach { warning ->
                            Text("• $warning", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onSave(
                                    selectedCategory,
                                    if (selectedCategory == null) result.categoryName else null,
                                )
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Добавить в сейф", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(onClick = { onCopy(result.value) }, modifier = Modifier.height(52.dp), shape = RoundedCornerShape(16.dp)) {
                            Text("Копировать")
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        ModelPickerDialog(
            models = models.orEmpty(),
            selected = ai.model,
            loading = modelsLoading,
            error = modelsError,
            onPick = { id ->
                onPickModel(id)
                showPicker = false
            },
            onDismiss = { showPicker = false },
            onOpenSettings = onOpenAiSettings,
        )
    }
}

@Composable
private fun SmallField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}
