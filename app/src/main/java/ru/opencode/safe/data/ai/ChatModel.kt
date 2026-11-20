package ru.opencode.safe.data.ai

import ru.opencode.safe.data.model.AiConfig

/**
 * Абстракция над чат-моделью. Реальная реализация — [AiClient],
 * в тестах подменяется заглушкой.
 */
interface ChatModel {
    suspend fun complete(
        config: AiConfig,
        system: String,
        userText: String,
        images: List<ByteArray>,
        jsonMode: Boolean,
    ): String

    suspend fun models(config: AiConfig): List<String>
}

class AiException(message: String, val code: Int? = null) : Exception(message)
