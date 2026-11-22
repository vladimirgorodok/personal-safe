package ru.opencode.safe.data.ai

import ru.opencode.safe.data.model.AiConfig

/** Провайдер ИИ: адрес, модель по умолчанию, где взять ключ и что важно знать про тариф. */
data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val model: String,
    /** Где взять ключ. Пусто — ссылку не показываем. */
    val keysUrl: String = "",
    /** Что важно знать про бесплатный тариф. */
    val freeHint: String = "",
)

/**
 * Список известных провайдеров и переключение между ними.
 *
 * У каждого провайдера свой ключ, поэтому активный ключ запоминается в [AiConfig.apiKeys]:
 * можно переключиться с Groq на aitunnel и обратно, не вводя ключ заново. Логика вынесена
 * сюда, чтобы одинаково работать и в настройках, и прямо на экране анализа.
 */
object AiProviders {

    const val CUSTOM_ID = "custom"

    val all: List<AiProvider> = listOf(
        AiProvider(
            id = AiDefaults.PROVIDER_ID,
            name = "Groq — бесплатно",
            baseUrl = AiDefaults.BASE_URL,
            model = AiDefaults.MODEL,
            keysUrl = AiDefaults.KEYS_URL,
            freeHint = "Ключ бесплатный и без карты: 1000 запросов в сутки, есть модели с картинками.",
        ),
        AiProvider(
            "openrouter", "OpenRouter",
            "https://openrouter.ai/api/v1", "google/gemma-4-31b-it:free",
            keysUrl = "https://openrouter.ai/keys",
            freeHint = "Бесплатные модели помечены «:free». Суточный лимит обновляется в 03:00 по Москве.",
        ),
        AiProvider(
            "gemini", "Google Gemini",
            "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-4-hydrogen",
            keysUrl = "https://aistudio.google.com/app/apikey",
            freeHint = "Бесплатный тариф без карты, лимиты смотрите в AI Studio. Данные могут использоваться Google.",
        ),
        AiProvider("aitunnel", "aitunnel", "https://api.aitunnel.ru/v1", "gpt-4o-mini"),
        AiProvider(
            "openai", "OpenAI",
            "https://api.openai.com/v1", "gpt-4o-mini",
            keysUrl = "https://platform.openai.com/api-keys",
            freeHint = "Платный провайдер: ключ и оплата — на стороне OpenAI.",
        ),
        AiProvider(CUSTOM_ID, "Другой", "", ""),
    )

    /** Строка выбора провайдера: имя и есть ли для него сохранённый ключ. */
    data class Option(val id: String, val name: String, val hasKey: Boolean)

    fun byId(id: String): AiProvider? = all.firstOrNull { it.id == id }

    /** Имя провайдера для показа: сохранённое в конфиге или взятое из пресета. */
    fun displayName(config: AiConfig): String =
        config.providerName.ifBlank { byId(config.providerId)?.name ?: "Провайдер" }

    /** Есть ли ключ для этого провайдера: активный сейчас или сохранённый ранее. */
    fun hasKey(config: AiConfig, providerId: String): Boolean =
        (providerId == config.providerId && config.apiKey.isNotBlank()) ||
            config.apiKeys[providerId].orEmpty().isNotBlank()

    fun options(config: AiConfig): List<Option> = all.map { Option(it.id, it.name, hasKey(config, it.id)) }

    /**
     * Переключение на другого провайдера. Адрес и модель берём у пресета, ключ — из памяти
     * ключей: сначала сохраняем текущий, затем подставляем ключ выбранного провайдера.
     * Для «Другого» оставляем введённые вручную адрес и модель.
     */
    fun switchTo(current: AiConfig, providerId: String): AiConfig {
        val preset = byId(providerId) ?: return current
        val keys = current.rememberActiveKey()
        return if (providerId == CUSTOM_ID) {
            current.copy(providerId = providerId, providerName = preset.name, apiKeys = keys)
        } else {
            current.copy(
                providerId = providerId,
                providerName = preset.name,
                baseUrl = preset.baseUrl,
                model = preset.model,
                apiKey = keys[providerId].orEmpty(),
                apiKeys = keys,
            )
        }
    }

    /** Выбор модели у текущего провайдера. */
    fun withModel(current: AiConfig, model: String): AiConfig =
        current.copy(model = model, apiKeys = current.rememberActiveKey())

    /** Конфиг с запоминанием активного ключа — применяется при сохранении. */
    fun normalized(config: AiConfig): AiConfig =
        config.copy(apiKeys = config.rememberActiveKey())

    /** Активный ключ + память ключей, чтобы ключ не терялся при переключении провайдера. */
    private fun AiConfig.rememberActiveKey(): Map<String, String> =
        if (providerId.isBlank() || apiKey.isBlank()) apiKeys else apiKeys + (providerId to apiKey)
}
