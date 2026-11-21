package ru.opencode.safe.data.ai

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import ru.opencode.safe.data.model.AiConfig
import java.util.concurrent.TimeUnit

/**
 * Клиент к любому OpenAI-совместимому эндпоинту (aitunnel, OpenAI, OpenRouter и др.).
 * Один и тот же код работает со всеми: меняется только baseUrl/ключ/модель.
 */
class AiClient(private val http: OkHttpClient = defaultClient()) : ChatModel {

    override suspend fun complete(
        config: AiConfig,
        system: String,
        userText: String,
        images: List<ByteArray>,
        jsonMode: Boolean,
    ): String = withContext(Dispatchers.IO) {
        val model = if (images.isNotEmpty()) config.effectiveVisionModel else config.model
        val payload = JSONObject().apply {
            put("model", model)
            put("temperature", config.temperature)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", system))
                put(JSONObject().put("role", "user").put("content", userContent(userText, images)))
            })
            if (jsonMode) put("response_format", JSONObject().put("type", "json_object"))
        }

        val request = Request.Builder()
            .url(endpoint(config.baseUrl, "chat/completions"))
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON))
            .build()

        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = extractError(body)
                val message = if (response.code == 429) {
                    AiErrors.tooManyRequests(response.headers, detail)
                } else {
                    buildString {
                        append("(${response.code}) $detail")
                        AiErrors.hintFor(response.code, detail)?.let { append("\n\n").append(it) }
                    }
                }
                throw AiException(message, response.code)
            }
            parseContent(body)
        }
    }

    override suspend fun models(config: AiConfig): List<String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(endpoint(config.baseUrl, "models"))
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .get()
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = extractError(body)
                val message = if (response.code == 429) {
                    AiErrors.tooManyRequests(response.headers, detail)
                } else {
                    "(${response.code}) $detail"
                }
                throw AiException(message, response.code)
            }
            val data = JSONObject(body).optJSONArray("data") ?: JSONArray()
            (0 until data.length())
                .mapNotNull { data.optJSONObject(it)?.optString("id")?.ifBlank { null } }
                .sorted()
        }
    }

    private fun userContent(text: String, images: List<ByteArray>): Any {
        if (images.isEmpty()) return text
        return JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", text))
            images.forEach { bytes ->
                val dataUrl = "data:${sniffMime(bytes)};base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                put(
                    JSONObject().put("type", "image_url")
                        .put("image_url", JSONObject().put("url", dataUrl))
                )
            }
        }
    }

    private fun parseContent(body: String): String {
        val choices = JSONObject(body).optJSONArray("choices")
        val message = choices?.optJSONObject(0)?.optJSONObject("message")
        val content = message?.opt("content")
        return when (content) {
            is String -> content
            is JSONArray -> (0 until content.length())
                .mapNotNull { content.optJSONObject(it)?.optString("text")?.ifBlank { null } }
                .joinToString("\n")
            else -> ""
        }
    }

    private fun extractError(body: String): String = runCatching {
        val obj = JSONObject(body)
        obj.optJSONObject("error")?.optString("message")?.ifBlank { null } ?: obj.optString("message")
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: body.take(300)

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        fun endpoint(baseUrl: String, path: String): String {
            val base = baseUrl.trim().trimEnd('/')
            val suffix = path.trim('/')
            // У Gemini база заканчивается на /v1beta/openai — там путь идёт сразу после /openai.
            return if (base.endsWith("/v1") || base.endsWith("/openai/v1") || base.endsWith("/openai")) {
                "$base/$suffix"
            } else {
                "$base/v1/$suffix"
            }
        }

        fun sniffMime(bytes: ByteArray): String = when {
            bytes.size >= 4 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() -> "image/png"
            bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
            bytes.size >= 3 && bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() -> "image/gif"
            bytes.size >= 12 && String(bytes, 0, 4) == "RIFF" -> "image/webp"
            else -> "image/png"
        }
    }
}
