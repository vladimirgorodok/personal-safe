package ru.opencode.safe.ui.vault

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.opencode.safe.data.model.Category
import ru.opencode.safe.data.model.Secret
import ru.opencode.safe.ui.components.Appear
import ru.opencode.safe.ui.components.CategoryAvatar
import ru.opencode.safe.ui.components.EmptyState
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.components.SafeTopBar
import ru.opencode.safe.ui.components.TypeBadge
import ru.opencode.safe.ui.components.mask

@Composable
fun CategoryDetailScreen(
    category: Category,
    secrets: List<Secret>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRegenerateIcon: () -> Unit,
    onOpenSecret: (String) -> Unit,
    onAddSecret: () -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            Button(onClick = onAddSecret, shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Добавить вручную")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SafeTopBar(category.name, onBack = onBack, subtitle = "${secrets.size} секретов") }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryAvatar(category, 68.dp)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(category.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                TypeBadge(category.type)
                            }
                        }
                        if (category.description.isNotBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Text(category.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!category.brandDomain.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Домен бренда: ${category.brandDomain}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onEdit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                                Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Изменить")
                            }
                            OutlinedButton(onClick = onRegenerateIcon, shape = RoundedCornerShape(14.dp)) {
                                Icon(Icons.Rounded.AutoFixHigh, null, Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            if (secrets.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.VpnKey,
                        "В категории пусто",
                        "Добавьте секрет вручную или найдите его через «Анализ» — агент положит находку сюда.",
                    )
                }
            } else {
                itemsIndexed(secrets, key = { _, secret -> secret.id }) { index, secret ->
                    Appear(index = index) {
                        GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onOpenSecret(secret.id) }) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Rounded.VpnKey, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(secret.title.ifBlank { "Без названия" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(2.dp))
                                    Text(mask(secret.value), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { onToggleFavorite(secret.id) }) {
                                    Icon(
                                        if (secret.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                        "Избранное",
                                        tint = if (secret.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
