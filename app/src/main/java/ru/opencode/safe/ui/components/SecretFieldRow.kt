package ru.opencode.safe.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.opencode.safe.data.model.SecretField

/**
 * Одно дополнительное значение записи: подпись, скрытое значение с глазом и копированием,
 * кнопка удаления. Используется и в карточке секрета, и на экране анализа.
 */
@Composable
fun SecretFieldRow(
    field: SecretField,
    revealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    onChange: (SecretField) -> Unit,
    onRemove: () -> Unit,
    onCopy: () -> Unit,
    labelPlaceholder: String = "Например, «Код восстановления»",
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = field.label,
                onValueChange = { onChange(field.copy(label = it)) },
                label = { Text("Название поля") },
                placeholder = { Text(labelPlaceholder) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors(),
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

/** Общие цвета полей маски и подписей. */
@Composable
internal fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
    focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    unfocusedIndicatorColor = Color.Transparent,
)

/** Все заполненные значения одной строкой: «Подпись: значение» через перевод строки. */
fun copyableFieldsText(fields: List<SecretField>): String =
    fields
        .filter { it.value.isNotBlank() }
        .joinToString("\n") { "${it.label.ifBlank { "Значение" }}: ${it.value}" }
