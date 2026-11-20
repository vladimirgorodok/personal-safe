package ru.opencode.safe.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.opencode.safe.data.model.Category
import ru.opencode.safe.data.model.CategoryType
import ru.opencode.safe.data.model.SecretLink
import java.io.File
import kotlin.math.abs

val LocalIconDir = staticCompositionLocalOf<String?> { null }

fun typeIcon(type: CategoryType): ImageVector = when (type) {
    CategoryType.TOKEN -> Icons.Rounded.VpnKey
    CategoryType.PASSWORD -> Icons.Rounded.Lock
    CategoryType.API_KEY -> Icons.Rounded.Code
    CategoryType.ID_CARD -> Icons.Rounded.Badge
    CategoryType.BANK_CARD -> Icons.Rounded.CreditCard
    CategoryType.PASSPORT -> Icons.Rounded.MenuBook
    CategoryType.ACCOUNT -> Icons.Rounded.Person
    CategoryType.WIFI -> Icons.Rounded.Wifi
    CategoryType.PIN -> Icons.Rounded.Pin
    CategoryType.LICENSE -> Icons.Rounded.Description
    CategoryType.CERTIFICATE -> Icons.Rounded.Verified
    CategoryType.NOTE -> Icons.Rounded.StickyNote2
    CategoryType.OTHER -> Icons.Rounded.Folder
}

fun seedBrush(seed: Long): Brush {
    val hue = (abs(seed) % 360).toFloat()
    val a = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.62f, 0.92f)))
    val b = Color(android.graphics.Color.HSVToColor(floatArrayOf((hue + 44f) % 360f, 0.78f, 0.62f)))
    return Brush.linearGradient(listOf(a, b))
}

@Composable
fun rememberFileBitmap(path: String?): ImageBitmap? {
    val bitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap.value = withContext(Dispatchers.IO) {
            path?.let { p ->
                val f = File(p)
                if (f.exists()) BitmapFactory.decodeFile(p)?.asImageBitmap() else null
            }
        }
    }
    return bitmap.value
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val base = if (MaterialTheme.colorScheme.background.luminanceIsDark()) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.White.copy(alpha = 0.75f)
    }
    Surface(
        modifier = modifier
            .clip(shape)
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .border(1.dp, SafeGlassStrokeColor(), shape),
        color = base,
        shape = shape,
    ) { content() }
}

@Composable
fun SafeGlassStrokeColor(): Color =
    if (MaterialTheme.colorScheme.background.luminanceIsDark()) Color.White.copy(alpha = 0.14f)
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)

private fun Color.luminanceIsDark(): Boolean = (red * 0.299f + green * 0.587f + blue * 0.114f) < 0.5f

@Composable
fun CategoryAvatar(category: Category, size: Dp = 48.dp) {
    val dir = LocalIconDir.current
    val path = remember(category.iconFile, dir) {
        category.iconFile?.let { name -> dir?.let { File(it, name).absolutePath } }
    }
    val bitmap = rememberFileBitmap(path)
    val shape = RoundedCornerShape(size * 0.32f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(seedBrush(category.colorSeed.takeIf { it != 0L } ?: abs(category.name.hashCode()).toLong())),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = category.name, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Icon(typeIcon(category.type), contentDescription = category.name, tint = Color.White, modifier = Modifier.size(size * 0.5f))
        }
    }
}

@Composable
fun BrandAvatar(name: String, iconFile: String?, seed: Long, size: Dp = 48.dp) {
    val dir = LocalIconDir.current
    val path = remember(iconFile, dir) {
        iconFile?.let { n -> dir?.let { File(it, n).absolutePath } }
    }
    val bitmap = rememberFileBitmap(path)
    val shape = RoundedCornerShape(size * 0.32f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(seedBrush(seed)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Text(
                initialsOf(name),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("[\\s-]+")).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

@Composable
fun TypeBadge(type: CategoryType) {
    val color = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Icon(typeIcon(type), null, tint = color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(type.label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
fun MaskedValue(
    value: String,
    onCopy: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var revealed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (revealed) value else mask(value),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { revealed = !revealed }) {
            Icon(if (revealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Показать")
        }
        if (onCopy != null) {
            IconButton(onClick = onCopy) { Icon(Icons.Rounded.ContentCopy, "Копировать") }
        }
    }
}

fun mask(value: String): String {
    if (value.isBlank()) return ""
    if (value.length <= 6) return "•".repeat(value.length)
    return value.take(4) + "•".repeat(minOf(14, value.length - 6)) + value.takeLast(2)
}

@Composable
fun DetailExpander(
    title: String,
    text: String,
    links: List<SecretLink>,
    initiallyExpanded: Boolean = false,
    onOpen: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Article, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(if (expanded) "Свернуть" else "Подробно", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            androidx.compose.material3.Icon(
                Icons.Rounded.KeyboardArrowDown,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.androidxRotate(arrow),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                if (text.isNotBlank()) {
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (links.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    links.forEach { link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpen(link.url) }
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Article, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(link.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(link.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

private fun Modifier.androidxRotate(degrees: Float): Modifier = this.rotate(degrees)

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
