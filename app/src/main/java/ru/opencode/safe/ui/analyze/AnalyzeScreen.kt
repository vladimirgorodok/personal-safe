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
import androidx.compose.material.icons.rounded.ContentCopy
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.opencode.safe.data.ai.AgentSource
import ru.opencode.safe.data.ai.AiProviders
import ru.opencode.safe.data.model.AiConfig
import ru.opencode.safe.data.model.Category
import ru.opencode.safe.data.model.FieldTemplates
import ru.opencode.safe.data.model.SecretField
import ru.opencode.safe.ui.AnalyzeState
import ru.opencode.safe.ui.components.CategoryAvatar
import ru.opencode.safe.ui.components.DetailExpander
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.components.ModelPickerDialog
import ru.opencode.safe.ui.components.SafeTopBar
import ru.opencode.safe.ui.components.SectionTitle
import ru.opencode.safe.ui.components.SecretFieldRow
import ru.opencode.safe.ui.components.TypeBadge
import ru.opencode.safe.ui.components.copyableFieldsText

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
    onSave: (categoryId: String?, newCategoryName: String?, fields: List<SecretField>) -> Unit,
    onOpenLink: (String) -> Unit,
    onCopy: (String) -> Unit,
    onOpenAiSettings: () -> Unit,
    onLoadModels: (AiConfig, (Result<List<String>>) -> Unit) -> Unit,
    onApplyAi: (AiConfig) -> Unit,
    onBack: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(onPickedUri)
    }

    val aiConfigured = ai.isConfigured && ai.enabled
    var showPicker by remember { mutableStateOf(false) }
    var dialogProviderId by remember(showPicker) { mutableStateOf(ai.providerId) }
    var models by remember { mutableStateOf<List<String>?>(null) }
    var modelsLoading by remember { mutableStateOf(false) }
    var modelsError by remember { mutableStateOf<String?>(null) }

    fun loadModelsFor(providerId: String) {
        val candidate = AiProviders.switchTo(ai, providerId)
        models = null
        modelsError = null
        modelsLoading = true
        onLoadModels(candidate) { result ->
            modelsLoading = false
            result
                .onSuccess { models = it }
                .onFailure { modelsError = it.message ?: "Не удалось получить список" }
        }
    }

    fun openPicker() {
        showPicker = true
        if (models == null && !modelsLoading) loadModelsFor(dialogProviderId)
    }

    val hasData = state.input.isNotBlank() || state.images.isNotEmpty() ||
        state.imageUrl != null || state.result != null || state.error != null

    var selectedCategory by remember(state.result) { mutableStateOf(state.result?.matchedCategoryId) }
    var title by remember(state.result) { mutableStateOf(state.result?.title.orEmpty()) }
    var description by remember(state.result) { mutableStateOf(state.result?.description.orEmpty()) }
    var note by remember(state.result) { mutableStateOf(state.result?.note.orEmpty()) }
    // Типовые поля типа: значения агента подставлены, остальные подписи ждут заполнения.
    var fields by remember(state.result) {
        mutableStateOf(state.result?.let { FieldTemplates.merge(it.type, it.fields) }.orEmpty())
    }
    var revealedFields by remember(state.result) { mutableStateOf(emptySet<Int>()) }

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
                    onClick = { if (aiConfigured) openPicker() else onOpenAiSettings() },
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
                                    "${AiProviders.displayName(ai)} · ${ai.model.ifBlank { "модель не выбрана" }}"
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

                if (fields.isNotEmpty()) {
                    item {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SectionTitle("Дополнительные значения", Modifier.weight(1f))
                                if (fields.any { it.value.isNotBlank() }) {
                                    TextButton(onClick = { onCopy(copyableFieldsText(fields)) }) {
                                        Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Копировать всё")
                                    }
                                }
                            }
                            Text(
                                "Агент разложил находку по полям. Заполните пустые, лишние удалите — значения " +
                                    "скрыты глазом и копируются одной кнопкой.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            fields.forEachIndexed { index, field ->
                                SecretFieldRow(
                                    field = field,
                                    revealed = index in revealedFields,
                                    onRevealedChange = { open ->
                                        revealedFields = if (open) revealedFields + index else revealedFields - index
                                    },
                                    onChange = { updated ->
                                        fields = fields.toMutableList().also { it[index] = updated }
                                    },
                                    onRemove = {
                                        fields = fields.toMutableList().also { it.removeAt(index) }
                                        revealedFields = revealedFields
                                            .filter { it != index }
                                            .map { if (it > index) it - 1 else it }
                                            .toSet()
                                    },
                                    onCopy = { onCopy(field.value) },
                                    labelPlaceholder = "Название поля",
                                )
                                if (index != fields.lastIndex) Spacer(Modifier.height(14.dp))
                            }
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
                                    fields.filter { it.label.isNotBlank() || it.value.isNotBlank() },
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
        val pickerConfig = AiProviders.switchTo(ai, dialogProviderId)
        ModelPickerDialog(
            models = models.orEmpty(),
            selected = pickerConfig.model,
            title = "Провайдер и модель",
            loading = modelsLoading,
            error = modelsError,
            providers = AiProviders.options(ai),
            selectedProviderId = dialogProviderId,
            onPickProvider = { id ->
                if (id != dialogProviderId) {
                    dialogProviderId = id
                    // Сразу применяем провайдера: его адрес, модель по умолчанию и сохранённый ключ.
                    onApplyAi(AiProviders.switchTo(ai, id))
                    loadModelsFor(id)
                }
            },
            onPick = { id ->
                onApplyAi(AiProviders.withModel(pickerConfig, id))
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
