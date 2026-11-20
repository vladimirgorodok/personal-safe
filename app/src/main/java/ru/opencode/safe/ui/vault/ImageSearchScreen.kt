package ru.opencode.safe.ui.vault

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.opencode.safe.data.ai.AgentSource
import ru.opencode.safe.data.model.Category
import ru.opencode.safe.data.model.Defaults
import ru.opencode.safe.data.model.Secret
import ru.opencode.safe.ui.ImageSearchState
import ru.opencode.safe.ui.components.Appear
import ru.opencode.safe.ui.components.BrandAvatar
import ru.opencode.safe.ui.components.CategoryAvatar
import ru.opencode.safe.ui.components.DetailExpander
import ru.opencode.safe.ui.components.EmptyState
import ru.opencode.safe.ui.components.GlassCard
import ru.opencode.safe.ui.components.RemoteImage
import ru.opencode.safe.ui.components.SafeTopBar
import ru.opencode.safe.ui.components.SectionTitle
import ru.opencode.safe.ui.components.TypeBadge
import ru.opencode.safe.ui.components.mask

@Composable
fun ImageSearchScreen(
    state: ImageSearchState,
    matches: List<Secret>,
    gallery: List<Secret>,
    categories: List<Category>,
    brandIcons: Map<String, String>,
    aiConfigured: Boolean,
    onAnalyze: (ByteArray) -> Unit,
    onClear: () -> Unit,
    onOpenSecret: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenAiSettings: () -> Unit,
    onEnsureBrandIcon: (String, String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val categoryById = remember(categories) { categories.associateBy { it.id } }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var preview by remember { mutableStateOf<ByteArray?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pendingUri = uri
    }

    LaunchedEffect(pendingUri) {
        val uri = pendingUri ?: return@LaunchedEffect
        val bytes = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        }
        pendingUri = null
        if (bytes != null) {
            preview = bytes
            onAnalyze(bytes)
        }
    }

    val result = state.result
    val brandDomain = result?.brandDomain
    LaunchedEffect(brandDomain) {
        if (!brandDomain.isNullOrBlank() && brandIcons[brandDomain] == null) {
            onEnsureBrandIcon(brandDomain, result?.brandName ?: brandDomain)
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
                    title = "Поиск по изображению",
                    subtitle = "Агент узнает бренд и найдёт похожие секреты",
                    onBack = onBack,
                    actions = { IconButton(onClick = onOpenAiSettings) { Icon(Icons.Rounded.Settings, "Настройки ИИ") } },
                )
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                                        ),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.ImageSearch, null, tint = Color.White) }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Что на картинке?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    "Скриншот карты, страница настроек или логотип — агент определит сервис и покажет секреты этого бренда.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (preview == null && result == null && !state.running && state.error == null) {
                            Spacer(Modifier.height(16.dp))
                            Text("Как это работает", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Step(1, "Выберите изображение из галереи или файлов")
                            Step(2, "Агент определит бренд, тип и категорию")
                            Step(3, "Откройте найденный секрет или добавьте новый")
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { picker.launch("image/*") }, shape = RoundedCornerShape(14.dp)) {
                                Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Выбрать изображение")
                            }
                            if (preview != null || result != null || state.error != null) {
                                OutlinedButton(onClick = { preview = null; onClear() }, shape = RoundedCornerShape(14.dp)) {
                                    Text("Сбросить")
                                }
                            }
                        }

                        preview?.let { bytes ->
                            val bitmap = remember(bytes) {
                                runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
                            }
                            if (bitmap != null) {
                                Spacer(Modifier.height(14.dp))
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
            }

            if (state.running) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Агент распознаёт изображение…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            state.error?.let { error ->
                item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp)) {
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp))
                    }
                }
            }

            if (result != null) {
                item {
                    Appear {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BrandAvatar(
                                        result.brandName ?: "?",
                                        brandIcons[brandDomain],
                                        Defaults.seedFor(result.brandName ?: "brand"),
                                        54.dp,
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            result.brandName ?: "Бренд не определён",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            brandDomain ?: (if (result.source == AgentSource.AI) "распознано ИИ" else "офлайн-эвристика"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                LinearProgressIndicator(
                                    progress = { result.confidence.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Уверенность ${(result.confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TypeBadge(result.type)
                                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                                        Text(
                                            result.categoryName,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                if (result.description.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(result.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                if (result.detailedInfo.isNotBlank()) {
                    item {
                        Appear(index = 1) {
                            DetailExpander(
                                title = "Подробное описание от агента",
                                text = result.detailedInfo,
                                links = result.links,
                                onOpen = onOpenLink,
                            )
                        }
                    }
                }

                item { SectionTitle(if (matches.isEmpty()) "Совпадений нет" else "Найдено в сейфе: ${matches.size}") }
                if (matches.isEmpty()) {
                    item {
                        EmptyState(
                            Icons.Rounded.ImageSearch,
                            "Похожих секретов нет",
                            "Секреты этого бренда ещё не сохранены. Добавьте их через «Анализ».",
                        )
                    }
                } else {
                    itemsIndexed(matches, key = { _, secret -> secret.id }) { index, secret ->
                        Appear(index = index) {
                            SecretMatchRow(secret, categoryById[secret.categoryId]) { onOpenSecret(secret.id) }
                        }
                    }
                }
            }

            item { SectionTitle("Изображения в сейфе: ${gallery.size}") }
            if (gallery.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.PhotoLibrary,
                        "Изображений нет",
                        "Ссылки на картинки попадают в сейф, когда агент находит их в тексте.",
                    )
                }
            } else {
                itemsIndexed(gallery.chunked(2)) { index, rowItems ->
                    Appear(index = index) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { secret ->
                                GalleryCard(secret, categoryById[secret.categoryId], Modifier.weight(1f)) { onOpenSecret(secret.id) }
                            }
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SecretMatchRow(secret: Secret, category: Category?, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (category != null) {
                CategoryAvatar(category, 42.dp)
            } else {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.VpnKey, null, tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(secret.title.ifBlank { "Без названия" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(mask(secret.value), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TypeBadge(secret.type)
        }
    }
}

@Composable
private fun GalleryCard(secret: Secret, category: Category?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box {
                RemoteImage(
                    url = secret.imageUrl,
                    modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(14.dp)),
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                ) {
                    Text(
                        category?.name ?: secret.type.label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(secret.title.ifBlank { "Изображение" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                secret.brandDomain ?: secret.type.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
