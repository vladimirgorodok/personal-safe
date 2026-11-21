package ru.opencode.safe.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import ru.opencode.safe.data.model.Category
import ru.opencode.safe.data.model.CategoryType
import ru.opencode.safe.data.model.Secret
import ru.opencode.safe.data.model.SecretField
import ru.opencode.safe.data.model.SecretLink
import ru.opencode.safe.ui.components.CategoryAvatar
import ru.opencode.safe.ui.components.DetailExpander
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.components.SafeTopBar
import ru.opencode.safe.ui.components.SectionTitle
import ru.opencode.safe.ui.components.SecretValueField
import ru.opencode.safe.ui.components.TypeBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecretDetailScreen(
    secret: Secret,
    category: Category?,
    categories: List<Category>,
    onBack: () -> Unit,
    onSave: (Secret) -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenLink: (String) -> Unit,
    onCopy: (String) -> Unit,
) {
    var title by remember(secret.id) { mutableStateOf(secret.title) }
    var value by remember(secret.id) { mutableStateOf(secret.value) }
    var description by remember(secret.id) { mutableStateOf(secret.description) }
    var note by remember(secret.id) { mutableStateOf(secret.note) }
    var fields by remember(secret.id) { mutableStateOf(secret.fields) }
    var categoryId by remember(secret.id) { mutableStateOf(secret.categoryId) }
    var revealed by remember(secret.id) { mutableStateOf(false) }
    var revealedFields by remember(secret.id) { mutableStateOf(emptySet<Int>()) }
    var showMove by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val currentCategory = categories.firstOrNull { it.id == categoryId } ?: category
    val movedToAnother = categoryId != secret.categoryId
    val type: CategoryType = if (movedToAnother) currentCategory?.type ?: secret.type else secret.type

    val dirty = title != secret.title || value != secret.value ||
        description != secret.description || note != secret.note ||
        fields != secret.fields || movedToAnother

    val links: List<SecretLink> = secret.links
    val detailedInfo = secret.detailedInfo

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                Button(
                    onClick = {
                        onSave(
                            secret.copy(
                                title = title,
                                value = value,
                                description = description,
                                note = note,
                                fields = fields.filter { it.label.isNotBlank() || it.value.isNotBlank() },
                                categoryId = categoryId,
                                type = type,
                            )
                        )
                    },
                    enabled = dirty,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(if (dirty) "Сохранить изменения" else "Всё сохранено", fontWeight = FontWeight.SemiBold) }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SafeTopBar(
                    title = title.ifBlank { "Новый секрет" },
                    onBack = onBack,
                    subtitle = currentCategory?.name,
                    actions = {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                if (secret.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                "Избранное",
                                tint = if (secret.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Rounded.DeleteOutline, "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                )
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentCategory != null) {
                                CategoryAvatar(currentCategory, 48.dp)
                                Spacer(Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(currentCategory?.name ?: "Без категории", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                TypeBadge(type)
                            }
                            if (secret.foundByAgent) {
                                Icon(Icons.Rounded.AutoAwesome, "Найдено агентом", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showMove = true },
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.DriveFileMove, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Перенести в другую категорию")
                        }
                    }
                }
            }

            item {
                SecretValueField(
                    value = value,
                    revealed = revealed,
                    onRevealedChange = { revealed = it },
                    onChange = { value = it },
                    onCopy = { onCopy(value) },
                )
            }

            item { LabeledField("Название", title, { title = it }) }
            item { LabeledField("Описание", description, { description = it }, onCopy = { onCopy(description) }) }
            item { LabeledField("Примечание", note, { note = it }, minLines = 3, onCopy = { onCopy(note) }) }

            item { SectionTitle("Дополнительные значения", Modifier.padding(top = 6.dp)) }
            if (fields.isEmpty()) {
                item {
                    Text(
                        "Если в одной записи несколько важных значений (логин, пароль, код восстановления " +
                            "со скриншота) — добавьте их отдельными полями. Каждое поле скрыто глазом и копируется.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            fields.forEachIndexed { index, field ->
                item(key = "field-$index") {
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
                    )
                }
            }
            item {
                OutlinedButton(
                    onClick = { fields = fields + SecretField() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить значение")
                }
            }

            if (detailedInfo.isNotBlank() || links.isNotEmpty()) {
                item {
                    DetailExpander(
                        title = "Подробное описание",
                        text = detailedInfo,
                        links = links,
                        initiallyExpanded = true,
                        onOpen = onOpenLink,
                    )
                }
            }

            item {
                Text(
                    "Создано ${formatDate(secret.createdAt)} · изменено ${formatDate(secret.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showMove) {
        CategoryPickerDialog(
            categories = categories,
            selectedId = categoryId,
            onPick = { id -> categoryId = id; showMove = false },
            onDismiss = { showMove = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить секрет?") },
            text = { Text("Действие нельзя отменить — запись будет удалена из сейфа.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}

/** Одно дополнительное значение: подпись, скрытое значение и кнопка удаления. */
@Composable
private fun SecretFieldRow(
    field: SecretField,
    revealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    onChange: (SecretField) -> Unit,
    onRemove: () -> Unit,
    onCopy: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = field.label,
                onValueChange = { onChange(field.copy(label = it)) },
                label = { Text("Название поля") },
                placeholder = { Text("Например, «Код восстановления»") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.DeleteOutline, "Удалить поле", tint = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(8.dp))
        SecretValueField(
            value = field.value,
            revealed = revealed,
            onRevealedChange = onRevealedChange,
            onChange = { onChange(field.copy(value = it)) },
            onCopy = onCopy,
            label = "Значение поля",
            placeholder = "Введите секрет",
        )
    }
}

@Composable
private fun CategoryPickerDialog(
    categories: List<Category>,
    selectedId: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Перенести в категорию") },
        text = {
            if (categories.isEmpty()) {
                Text("Категорий пока нет — создайте её в сейфе.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(categories.size) { index ->
                        val cat = categories[index]
                        val selected = cat.id == selectedId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onPick(cat.id) }
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CategoryAvatar(cat, 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(cat.type.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (selected) {
                                Icon(Icons.Rounded.Check, "Выбрано", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    minLines: Int = 1,
    onCopy: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        textStyle = MaterialTheme.typography.bodyLarge,
        trailingIcon = onCopy?.let { copy ->
            {
                IconButton(onClick = copy) { Icon(Icons.Rounded.ContentCopy, "Копировать") }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(millis))
