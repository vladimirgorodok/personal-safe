package ru.opencode.safe.ui.vault

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import ru.opencode.safe.data.model.CategoryType
import ru.opencode.safe.ui.components.CategoryAvatar
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.components.SafeTopBar
import kotlin.random.Random

@Composable
fun CategoryEditScreen(
    category: Category?,
    onBack: () -> Unit,
    onSave: (Category) -> Unit,
    onDelete: (() -> Unit)?,
    onSearchLogo: (String) -> Unit,
) {
    val base = category ?: Category(name = "", type = CategoryType.OTHER, colorSeed = Random.nextLong(0, 360))
    var name by remember(base.id) { mutableStateOf(base.name) }
    var description by remember(base.id) { mutableStateOf(base.description) }
    var brandDomain by remember(base.id) { mutableStateOf(base.brandDomain.orEmpty()) }
    var type by remember(base.id) { mutableStateOf(base.type) }
    var seed by remember(base.id) { mutableStateOf(base.colorSeed.takeIf { it != 0L } ?: Random.nextLong(0, 360)) }
    var confirmDelete by remember { mutableStateOf(false) }

    val preview = base.copy(name = name.ifBlank { "Категория" }, type = type, colorSeed = seed, brandDomain = brandDomain.ifBlank { null })

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        onSave(
                            base.copy(
                                name = name.trim(),
                                type = type,
                                description = description.trim(),
                                brandDomain = brandDomain.trim().ifBlank { null },
                                colorSeed = seed,
                                isSystem = base.isSystem,
                            )
                        )
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(if (category == null) "Создать категорию" else "Сохранить", fontWeight = FontWeight.SemiBold) }
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
                    title = if (category == null) "Новая категория" else "Категория",
                    onBack = onBack,
                    actions = {
                        if (onDelete != null) {
                            IconButton(onClick = { confirmDelete = true }) {
                                Icon(Icons.Rounded.DeleteOutline, "Удалить", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                )
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(preview, 72.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name.ifBlank { "Без названия" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название категории") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors(),
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors(),
                )
            }

            item {
                OutlinedTextField(
                    value = brandDomain,
                    onValueChange = { brandDomain = it },
                    label = { Text("Домен бренда (для поиска логотипа)") },
                    placeholder = { Text("например yandex.ru") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors(),
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onSearchLogo(brandDomain.ifBlank { name }) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Rounded.TravelExplore, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Найти логотип")
                    }
                    TextButton(onClick = { seed = Random.nextLong(0, 360) }) {
                        Icon(Icons.Rounded.Palette, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Цвет")
                    }
                }
            }

            item {
                Column {
                    Text("Тип заметки", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CategoryType.entries.forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t.label) },
                                leadingIcon = { Icon(ru.opencode.safe.ui.components.typeIcon(t), null, Modifier.size(16.dp)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Подсказка: для известного бренда назовите категорию брендом — иконка подтянется автоматически.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить категорию?") },
            text = { Text("Все секреты этой категории тоже будут удалены.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete?.invoke() }) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
    focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    unfocusedIndicatorColor = Color.Transparent,
)
