package ru.opencode.safe.ui.vault

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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.opencode.safe.data.model.Category
import ru.opencode.safe.data.model.Secret
import ru.opencode.safe.data.model.SecretLink
import ru.opencode.safe.ui.components.CategoryAvatar
import ru.opencode.safe.ui.components.DetailExpander
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.components.MaskedValue
import ru.opencode.safe.ui.components.SafeTopBar
import ru.opencode.safe.ui.components.TypeBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecretDetailScreen(
    secret: Secret,
    category: Category?,
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
    var confirmDelete by remember { mutableStateOf(false) }

    val dirty = title != secret.title || value != secret.value ||
        description != secret.description || note != secret.note

    val links: List<SecretLink> = secret.links
    val detailedInfo = secret.detailedInfo

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                Button(
                    onClick = {
                        onSave(secret.copy(title = title, value = value, description = description, note = note))
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
                    subtitle = category?.name,
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
                            if (category != null) {
                                CategoryAvatar(category, 48.dp)
                                Spacer(Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(category?.name ?: "Без категории", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                TypeBadge(secret.type)
                            }
                            if (secret.foundByAgent) {
                                Icon(Icons.Rounded.AutoAwesome, "Найдено агентом", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    LabeledField("Значение", value, { value = it }, minLines = 2, mono = true)
                    Spacer(Modifier.height(10.dp))
                    MaskedValue(value = value, onCopy = { onCopy(value) })
                }
            }

            item { LabeledField("Название", title, { title = it }) }
            item { LabeledField("Описание", description, { description = it }) }
            item { LabeledField("Примечание", note, { note = it }, minLines = 3) }

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

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    minLines: Int = 1,
    mono: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        textStyle = if (mono) {
            MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        } else MaterialTheme.typography.bodyLarge,
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
