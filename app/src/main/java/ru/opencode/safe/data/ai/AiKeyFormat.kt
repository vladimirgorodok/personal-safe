package ru.opencode.safe.data.ai

/**
 * Эвристики для подключения ИИ «своим ключом» (BYOK).
 *
 * Приложение не вшивает ключи: у каждого пользователя свой бесплатный ключ. Чтобы этот шаг
 * не превращался в «скопировал, переключился, не понял, что вставлять», здесь собраны две
 * простые проверки: похоже ли значение на API-ключ и умеет ли модель смотреть картинки.
 */
object AiKeyFormat {

    /**
     * Похоже ли значение на API-ключ. Ключи — это длинная строка без пробелов из латинских
     * букв, цифр, дефиса, подчёркивания и точки (например `gsk_...`, `sk-or-v1-...`, `AIza...`).
     * Проверка нужна, чтобы предложить вставить ключ из буфера, а не тащить туда произвольный текст.
     */
    fun looksLikeApiKey(value: String?): Boolean {
        val text = value?.trim().orEmpty()
        if (text.length !in 20..400) return false
        return text.all { (it.isLetterOrDigit() && it.code < 128) || it == '-' || it == '_' || it == '.' }
    }

    /**
     * Слова-признаки того, что модель понимает изображения. Это подсказка по названию, а не
     * гарантия: точный ответ даёт только документация провайдера.
     */
    private val VISION_TOKENS = listOf(
        "vision", "omni", "multimodal", "image", "pixtral", "llava", "internvl", "minicpm",
        "-vl", "vl-", "_vl", "gpt-4o", "gpt-4.1", "gpt-5", "gemini", "claude-3", "claude-4",
        "glm-4v", "qwen2-vl", "qwen3-vl",
    )

    /** Умеет ли модель, судя по названию, смотреть изображения (нужно для скриншотов). */
    fun looksLikeVisionModel(name: String): Boolean {
        val n = name.lowercase()
        return VISION_TOKENS.any { n.contains(it) }
    }
}
