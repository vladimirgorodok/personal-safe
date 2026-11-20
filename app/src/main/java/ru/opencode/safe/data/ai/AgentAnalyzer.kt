package ru.opencode.safe.data.ai

import org.json.JSONArray
import org.json.JSONObject
import ru.opencode.safe.data.model.AiConfig
import ru.opencode.safe.data.model.Category
import ru.opencode.safe.data.model.CategoryType
import ru.opencode.safe.data.model.Defaults
import ru.opencode.safe.data.model.SecretLink

enum class AgentSource { AI, LOCAL }

data class AgentInput(
    val text: String = "",
    val images: List<ByteArray> = emptyList(),
    val imageUrl: String? = null,
    val sourceLabel: String? = null,
)

data class AgentResult(
    val type: CategoryType,
    val categoryName: String,
    val matchedCategoryId: String?,
    val isNewCategory: Boolean,
    val title: String,
    val value: String,
    val description: String,
    val note: String,
    val brandName: String?,
    val brandDomain: String?,
    val detailedInfo: String,
    val links: List<SecretLink>,
    val confidence: Float,
    val source: AgentSource,
    val warnings: List<String> = emptyList(),
)

/**
 * Агент анализа: принимает текст/изображение, определяет тип заметки,
 * подбирает или предлагает категорию и готовит подробное описание.
 */
class AgentAnalyzer(private val model: ChatModel) {

    suspend fun analyze(input: AgentInput, config: AiConfig, categories: List<Category>): AgentResult {
        val localGuess = Heuristics.classify(input.text.ifBlank { input.imageUrl.orEmpty() })

        if (!config.enabled || !config.isConfigured) {
            return fromLocal(input, localGuess, categories, listOf("ИИ-агент не настроен — сработала офлайн-эвристика."))
        }

        return try {
            val reply = model.complete(
                config = config,
                system = buildSystemPrompt(categories),
                userText = buildUserText(input),
                images = input.images,
                jsonMode = true,
            )
            fromModel(reply, input, localGuess, categories)
        } catch (e: Exception) {
            fromLocal(input, localGuess, categories, listOf("Агент недоступен: ${e.message ?: "ошибка сети"}. Определено локально."))
        }
    }

    // ---------- построение запроса ----------

    private fun buildUserText(input: AgentInput): String = buildString {
        appendLine("Проанализируй следующую находку из буфера обмена или файла и верни JSON по схеме.")
        appendLine("Тип источника: ${input.sourceLabel ?: "текст"}.")
        if (input.imageUrl != null) appendLine("Ссылка на изображение: ${input.imageUrl}")
        if (input.text.isNotBlank()) {
            appendLine()
            appendLine("Содержимое:")
            appendLine("----")
            appendLine(input.text.take(8000))
            appendLine("----")
        }
        if (input.images.isNotEmpty()) appendLine("Также приложены изображения: ${input.images.size} шт.")
    }

    private fun buildSystemPrompt(categories: List<Category>): String {
        val existing = if (categories.isEmpty()) "нет"
        else categories.joinToString("; ") { "${it.id}=${it.name}(${it.type.name})" }
        return PROMPT_TEMPLATE.replace("{CATEGORIES}", existing)
    }

    // ---------- разбор ответа ----------

    private fun fromModel(
        reply: String,
        input: AgentInput,
        localGuess: Heuristics.Guess,
        categories: List<Category>,
    ): AgentResult {
        val json = firstJsonObject(reply)
            ?: return fromLocal(input, localGuess, categories, listOf("Не удалось разобрать ответ агента."))

        val type = CategoryType.fromName(json.optString("type").ifBlank { localGuess.type.name })
        val rawName = json.optString("category_name").ifBlank { json.optString("category") }
        val brandName = json.optString("brand_name").ifBlank { null } ?: localGuess.brand?.name
        val brandDomain = json.optString("brand_domain").ifBlank { null } ?: localGuess.brand?.domain
        val categoryName = rawName.ifBlank {
            brandName?.let { "Токен $it" } ?: Defaults.systemCategoryName(type)
        }

        val matched = matchCategory(
            explicit = json.optString("existing_category").ifBlank { null },
            categoryName = categoryName,
            type = type,
            categories = categories,
        )

        val value = json.optString("value").ifBlank { input.text.ifBlank { input.imageUrl.orEmpty() } }
        val detailedInfo = buildDetailed(json.optString("detailed_info"), type, brandName, brandDomain)
        val links = parseLinks(json.optJSONArray("links")) + detailsLinks(detailedInfo)
        val confidence = json.optDouble("confidence", 0.6).toFloat().coerceIn(0f, 1f)

        return AgentResult(
            type = type,
            categoryName = categoryName,
            matchedCategoryId = matched?.id,
            isNewCategory = matched == null,
            title = json.optString("title").ifBlank { localGuess.title },
            value = value,
            description = json.optString("description").ifBlank { localGuess.description },
            note = json.optString("note"),
            brandName = brandName,
            brandDomain = brandDomain,
            detailedInfo = detailedInfo,
            links = links.distinctBy { it.url },
            confidence = confidence,
            source = AgentSource.AI,
        )
    }

    private fun fromLocal(
        input: AgentInput,
        guess: Heuristics.Guess,
        categories: List<Category>,
        warnings: List<String>,
    ): AgentResult {
        val guide = KnowledgeBase.guideFor(guess.brand, guess.type, input.text)
        val detailed = buildString {
            guide?.summary?.let { appendLine(it) }
            if (guide == null) appendLine(KnowledgeBase.genericGuide(guess.type))
        }.trim()
        val links = buildList {
            guide?.manageUrl?.let { add(SecretLink(guide.manageLabel, it)) }
            addAll(detailsLinks(detailed))
        }
        val baseName = guess.brand?.name ?: guess.categoryName
        val matched = matchCategory(null, baseName, guess.type, categories)
            ?: matchCategory(null, Defaults.systemCategoryName(guess.type), guess.type, categories)
        return AgentResult(
            type = guess.type,
            categoryName = baseName,
            matchedCategoryId = matched?.id,
            isNewCategory = matched == null,
            title = guess.title,
            value = input.text.ifBlank { input.imageUrl.orEmpty() },
            description = guess.description,
            note = "",
            brandName = guess.brand?.name,
            brandDomain = guess.brand?.domain,
            detailedInfo = detailed,
            links = links.distinctBy { it.url },
            confidence = guess.confidence,
            source = AgentSource.LOCAL,
            warnings = warnings,
        )
    }

    private fun buildDetailed(fromModel: String, type: CategoryType, brandName: String?, brandDomain: String?): String {
        val extra = KnowledgeBase.guideFor(brandDomain?.let { BrandCatalog.byDomain(it) ?: Brand(it, it) }, type, "")
        return when {
            fromModel.isNotBlank() && extra != null -> fromModel.trim() + "\n\n" + extra.summary
            fromModel.isNotBlank() -> fromModel.trim()
            extra != null -> extra.summary
            brandName != null -> KnowledgeBase.genericGuide(type) + "\n\nПредположительно принадлежит: $brandName."
            else -> KnowledgeBase.genericGuide(type)
        }
    }

    private fun detailsLinks(text: String): List<SecretLink> =
        Heuristics.extractLinks(text).map { SecretLink(it.substringAfter("//").substringBefore('/'), it) }

    private fun parseLinks(array: JSONArray?): List<SecretLink> {
        if (array == null) return emptyList()
        val out = ArrayList<SecretLink>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val url = o.optString("url")
            if (url.isBlank()) continue
            val title = o.optString("title").ifBlank { url.substringAfter("//").substringBefore('/') }
            out.add(SecretLink(title, url))
        }
        return out
    }

    companion object {
        fun matchCategory(explicit: String?, categoryName: String, type: CategoryType, categories: List<Category>): Category? {
            if (explicit != null) {
                categories.firstOrNull { it.id == explicit }?.let { return it }
            }
            categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }?.let { return it }
            val systemName = Defaults.systemCategoryName(type)
            categories.firstOrNull { it.name.equals(systemName, ignoreCase = true) }?.let { return it }
            categories.firstOrNull { it.type == type }?.let { return it }
            return null
        }

        fun firstJsonObject(text: String): JSONObject? {
            val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)```").find(text)?.groupValues?.get(1)
            val candidate = (fenced ?: text).trim()
            val start = candidate.indexOf('{')
            val end = candidate.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            return runCatching { JSONObject(candidate.substring(start, end + 1)) }.getOrNull()
        }

        private val PROMPT_TEMPLATE = """
            Ты — агент «Личного сейфа». Пользователь передал фрагмент из буфера обмена, файл или ссылку
            на изображение с секретом. Определи, что это за данные, к какому типу заметки они относятся,
            и подготовь подробное описание.

            Типы (поле type): TOKEN, PASSWORD, API_KEY, ID_CARD, BANK_CARD, PASSPORT, ACCOUNT, WIFI, PIN,
            LICENSE, CERTIFICATE, NOTE, OTHER.

            Уже существующие категории пользователя (формат id=название(тип)): {CATEGORIES}

            Правила:
            - Если данные подходят под существующую категорию — укажи её id в existing_category.
            - Если это известный бренд (Яндекс, Google, GitHub, банк и т.п.) — определи его, укажи brand_name
              и brand_domain (домен, например yandex.ru), а category_name сделай равным имени бренда (например «Яндекс»).
            - value — сам секрет в чистом виде.
            - description — короткое понятное название-подпись.
            - note — оставь пустым, это поле пользователя.
            - detailed_info — подробное описание: что это, кому принадлежит, для чего используется,
              как получить доступ на страницу редактирования или создать новый секрет.
            - links — до 3 полезных ссылок вида {"title":"...","url":"..."}.
            - confidence — от 0 до 1.

            Верни ТОЛЬКО JSON без пояснений:
            {"type":"...","category_name":"...","existing_category":"","title":"...","value":"...","description":"...","note":"","brand_name":"","brand_domain":"","confidence":0.0,"detailed_info":"...","links":[]}
        """.trimIndent()
    }
}
