package ru.opencode.safe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.opencode.safe.data.ai.AiKeyFormat

/**
 * Общий выбор модели провайдера: список бывает длинным, поэтому есть поиск и фильтр
 * «с картинками». Используется и в настройках ИИ, и прямо на экране анализа.
 */
@Composable
fun ModelPickerDialog(
    models: List<String>,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Модели провайдера",
    loading: Boolean = false,
    error: String? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    var onlyVision by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            when {
                loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Загружаем список моделей…", style = MaterialTheme.typography.bodyMedium)
                }

                error != null -> Column {
                    Text(
                        "Не удалось получить список моделей",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(error, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (onOpenSettings != null) {
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { onDismiss(); onOpenSettings() }) { Text("Открыть настройки ИИ") }
                    }
                }

                models.isEmpty() -> Text(
                    "Список пуст",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> Column {
                    val filtered = models
                        .filter { !onlyVision || AiKeyFormat.looksLikeVisionModel(it) }
                        .filter { query.isBlank() || it.contains(query.trim(), ignoreCase = true) }

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Поиск модели") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    FilterChip(
                        selected = onlyVision,
                        onClick = { onlyVision = !onlyVision },
                        label = { Text("С картинками") },
                        leadingIcon = { Icon(Icons.Rounded.Image, null, modifier = Modifier.size(16.dp)) },
                    )
                    Spacer(Modifier.height(8.dp))
                    if (filtered.isEmpty()) {
                        Text(
                            "Ничего не найдено",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                            items(filtered.size) { index ->
                                val id = filtered[index]
                                val isSelected = id.equals(selected, ignoreCase = true)
                                val vision = AiKeyFormat.looksLikeVisionModel(id)
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
                                    if (vision) {
                                        Icon(
                                            Icons.Rounded.Image,
                                            "Распознаёт изображения",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
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
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}
