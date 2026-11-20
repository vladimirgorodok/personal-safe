package ru.opencode.safe.data.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.opencode.safe.data.ai.BrandCatalog
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class IconResult(
    val file: File?,
    val remoteUrl: String?,
    val brandDomain: String?,
    val brandName: String?,
)

/**
 * Иконки категорий: сначала ищем логотип бренда в интернете (Clearbit → Google favicon →
 * DuckDuckGo), иначе рисуем детерминированную «стеклянную» иконку с инициалами.
 */
class IconRepository(
    private val dir: File,
    private val http: OkHttpClient = defaultClient(),
) {

    init {
        if (!dir.exists()) dir.mkdirs()
    }

    val directory: File get() = dir

    fun fileFor(name: String?): File? {
        if (name.isNullOrBlank()) return null
        val f = File(dir, name)
        return if (f.exists()) f else null
    }

    /** Ищет логотип известного бренда по названию или домену. */
    suspend fun resolveForQuery(query: String): IconResult = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext IconResult(null, null, null, null)

        val brand = BrandCatalog.detect(q) ?: BrandCatalog.search(q).firstOrNull()
        if (brand != null) return@withContext resolveForBrand(brand.domain).copy(brandName = brand.name)

        val domain = when {
            q.contains('.') -> q.substringAfter("://").substringBefore('/').lowercase()
            else -> listOf("$q.com", "$q.ru", "$q.io").firstOrNull { tld -> downloadLogo(tld) != null } ?: "$q.com"
        }
        val file = downloadLogo(domain)
        IconResult(file, logoUrl(domain), domain, q)
    }

    suspend fun resolveForBrand(domain: String): IconResult = withContext(Dispatchers.IO) {
        if (domain.isBlank()) return@withContext IconResult(null, null, null, null)
        val clean = domain.substringAfter("://").substringBefore('/').lowercase()
        val file = downloadLogo(clean)
        val brand = BrandCatalog.byDomain(clean)
        IconResult(file, logoUrl(clean), clean, brand?.name ?: clean.substringBefore('.'))
    }

    private fun downloadLogo(domain: String): File? {
        if (domain.isBlank()) return null
        val target = File(dir, "logo_" + sanitize(domain) + ".png")

        for (candidate in candidateUrls(domain)) {
            val bytes = fetch(candidate) ?: continue
            if (bytes.size < 200) continue
            return runCatching {
                target.writeBytes(bytes)
                target
            }.getOrNull()
        }
        return null
    }

    private fun candidateUrls(domain: String): List<String> = listOf(
        "https://logo.clearbit.com/$domain?size=256",
        "https://www.google.com/s2/favicons?domain=$domain&sz=128",
        "https://icons.duckduckgo.com/ip3/$domain.png",
    )

    private fun fetch(url: String): ByteArray? = runCatching {
        val request = Request.Builder().url(url).header("User-Agent", "SafeApp/1.0").get().build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            val bytes = resp.body?.bytes() ?: return@use null
            bytes
        }
    }.getOrNull()

    /** Рисует иконку-заглушку: градиент по seed + инициалы. */
    fun ensureGenerated(name: String, seed: Long): File {
        val target = File(dir, "gen_" + sanitize(name) + ".png")
        if (target.exists()) return target
        val bitmap = drawGenerated(name, seed)
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return target
    }

    private fun drawGenerated(name: String, seed: Long): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rnd = abs(seed) or 1L

        val hue = (rnd % 360).toFloat()
        val start = Color.HSVToColor(floatArrayOf(hue, 0.62f, 0.95f))
        val end = Color.HSVToColor(floatArrayOf((hue + 42f) % 360f, 0.78f, 0.62f))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), start, end, Shader.TileMode.CLAMP)
        val radius = size * 0.28f
        canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), radius, radius, paint)

        // стеклянный блик
        val gloss = Paint(Paint.ANTI_ALIAS_FLAG)
        gloss.shader = LinearGradient(0f, 0f, 0f, size * 0.5f, Color.argb(90, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), radius, radius, gloss)

        val initials = initialsOf(name)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * if (initials.length > 1) 0.40f else 0.48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(6f, 0f, 3f, Color.argb(80, 0, 0, 0))
        }
        val y = size / 2f - (text.descent() + text.ascent()) / 2f
        canvas.drawText(initials, size / 2f, y, text)

        return bitmap
    }

    private fun initialsOf(name: String): String {
        val cleaned = name.trim().removePrefix("Категория ").ifBlank { "?" }
        val words = cleaned.split(Regex("[\\s-]+")).filter { it.isNotBlank() }
        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(2).uppercase()
            else -> (words[0].take(1) + words[1].take(1)).uppercase()
        }
    }

    private fun sanitize(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9._-]"), "_").take(48)

    companion object {
        fun logoUrl(domain: String): String = "https://logo.clearbit.com/$domain?size=256"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
