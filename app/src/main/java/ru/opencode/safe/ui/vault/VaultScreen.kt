package ru.opencode.safe.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.opencode.safe.data.ai.BrandGroup
import ru.opencode.safe.data.ai.BrandGrouper
import ru.opencode.safe.data.model.Category
import ru.opencode.safe.data.model.Defaults
import ru.opencode.safe.data.model.Secret
import ru.opencode.safe.data.model.UserProfile
import ru.opencode.safe.ui.components.BrandAvatar
import ru.opencode.safe.ui.components.Appear
import ru.opencode.safe.ui.components.CategoryAvatar
import ru.opencode.safe.ui.components.ContourCounter
import ru.opencode.safe.ui.components.EmptyState
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.components.ProfileAvatar
import ru.opencode.safe.ui.components.SectionTitle
import ru.opencode.safe.ui.components.mask

@Composable
fun VaultScreen(
    profile: UserProfile?,
    categories: List<Category>,
    secrets: List<Secret>,
    brandIcons: Map<String, String>,
    onOpenSettings: () -> Unit,
    onOpenAnalyze: () -> Unit,
    onOpenImageSearch: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenSecret: (String) -> Unit,
    onOpenBrand: (String, String) -> Unit,
    onCreateCategory: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onEnsureBrandIcon: (String, String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var brandMode by remember { mutableStateOf(false) }
    val categoryById = remember(categories) { categories.associateBy { it.id } }

    val filteredSecrets = remember(secrets, query) {
        if (query.isBlank()) secrets
        else secrets.filter {
            it.title.contains(query, true) || it.value.contains(query, true) ||
                it.description.contains(query, true) || it.tags.any { t -> t.contains(query, true) }
        }
    }
    val visibleCategories = remember(categories, query) {
        if (query.isBlank()) categories
        else categories.filter { cat ->
            cat.name.contains(query, true) || filteredSecrets.any { it.categoryId == cat.id }
        }
    }
    val brandGroups = remember(secrets, categories, query) {
        val all = BrandGrouper.group(secrets, categories)
        if (query.isBlank()) all else all.filter { group ->
            group.name.contains(query, true) || group.domain?.contains(query, true) == true
        }
    }

    LaunchedEffect(brandMode, brandGroups) {
        if (brandMode) {
            brandGroups.forEach { group ->
                group.domain?.let { domain ->
                    if (brandIcons[domain] == null) onEnsureBrandIcon(domain, group.name)
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = onCreateCategory,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) { Icon(Icons.Rounded.Add, "Новая категория") }
                ExtendedFloatingActionButton(
                    onClick = onOpenAnalyze,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Rounded.AutoAwesome, null) },
                    text = { Text("Анализ") },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(profile, secrets.size, categories.size, onOpenSettings, onOpenImageSearch) }
            item { SearchBar(query) { query = it } }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !brandMode,
                        onClick = { brandMode = false },
                        leadingIcon = { Icon(Icons.Rounded.Category, null, Modifier.size(16.dp)) },
                        label = { Text("Категории") },
                    )
                    FilterChip(
                        selected = brandMode,
                        onClick = { brandMode = true },
                        leadingIcon = { Icon(Icons.Rounded.Storefront, null, Modifier.size(16.dp)) },
                        label = { Text("Бренды") },
                    )
                }
            }

            if (!brandMode) {
                if (query.isBlank()) {
                    item { SectionTitle("Категории", Modifier.padding(top = 4.dp)) }
                    itemsIndexed(visibleCategories.chunked(2)) { index, rowItems ->
                        Appear(index = index) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                rowItems.forEach { category ->
                                    CategoryCard(
                                        category = category,
                                        count = secrets.count { it.categoryId == category.id },
                                        modifier = Modifier.weight(1f),
                                        onClick = { onOpenCategory(category.id) },
                                    )
                                }
                                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    item { SectionTitle("Найдено: ${filteredSecrets.size}", Modifier.padding(top = 4.dp)) }
                }
            } else {
                item { SectionTitle("Бренды: ${brandGroups.size}", Modifier.padding(top = 4.dp)) }
                if (brandGroups.isEmpty()) {
                    item {
                        EmptyState(
                            Icons.Rounded.Storefront,
                            "Бренды не найдены",
                            "Агент распознаёт бренд по формату токена, домену или названию. Добавьте секреты через «Анализ».",
                        )
                    }
                } else {
                    itemsIndexed(brandGroups.chunked(2)) { index, rowItems ->
                        Appear(index = index) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                rowItems.forEach { group ->
                                    BrandCard(
                                        group = group,
                                        iconFile = group.domain?.let { brandIcons[it] },
                                        modifier = Modifier.weight(1f),
                                        onClick = { onOpenBrand(group.domain ?: group.key, group.name) },
                                    )
                                }
                                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle(
                    if (query.isBlank()) "Недавние" else "Секреты",
                    Modifier.padding(top = 12.dp),
                )
            }
            if (filteredSecrets.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.VpnKey,
                        if (secrets.isEmpty()) "Сейф пока пуст" else "Ничего не найдено",
                        if (secrets.isEmpty()) "Нажмите «Анализ» и вставьте токен, пароль или файл — агент разложит всё по категориям."
                        else "Попробуйте другой запрос.",
                    )
                }
            } else {
                itemsIndexed(filteredSecrets.take(60), key = { _, secret -> secret.id }) { index, secret ->
                    Appear(index = index) {
                        SecretRow(
                            secret = secret,
                            category = categoryById[secret.categoryId],
                            onClick = { onOpenSecret(secret.id) },
                            onToggleFavorite = { onToggleFavorite(secret.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    profile: UserProfile?,
    secretCount: Int,
    categoryCount: Int,
    onOpenSettings: () -> Unit,
    onOpenImageSearch: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Мой сейф", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "$secretCount секретов · $categoryCount категорий",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenImageSearch) { Icon(Icons.Rounded.ImageSearch, "Поиск по изображению") }
            ProfileAvatar(profile, 40.dp)
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, "Настройки") }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Поиск по названию, значению, тегам") },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, "Очистить") }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun CategoryCard(category: Category, count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryAvatar(category, 40.dp)
                Spacer(Modifier.width(10.dp))
                ContourCounter("$count")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                category.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BrandCard(group: BrandGroup, iconFile: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.padding(14.dp)) {
            BrandAvatar(group.name, iconFile, Defaults.seedFor(group.name), 44.dp)
            Spacer(Modifier.height(10.dp))
            Text(group.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                "${group.secrets.size} секретов${group.domain?.let { " · $it" } ?: ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SecretRow(secret: Secret, category: Category?, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (category != null) {
                CategoryAvatar(category, 42.dp)
            } else {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.VpnKey, null, tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(secret.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    mask(secret.value).ifBlank { secret.description },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (secret.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    "В избранное",
                    tint = if (secret.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
