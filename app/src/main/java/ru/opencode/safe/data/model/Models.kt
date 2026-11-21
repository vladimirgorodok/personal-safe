package ru.opencode.safe.data.model

import org.json.JSONArray
import org.json.JSONObject
import ru.opencode.safe.data.ai.AiDefaults
import java.util.UUID

/** Тип секрета: определяет категорию по умолчанию и способ отображения. */
enum class CategoryType(val label: String) {
    TOKEN("Токен"),
    PASSWORD("Пароль"),
    API_KEY("API-ключ"),
    ID_CARD("ID-карта"),
    BANK_CARD("Банковская карта"),
    PASSPORT("Паспорт"),
    ACCOUNT("Аккаунт"),
    WIFI("Wi-Fi"),
    PIN("PIN-код"),
    LICENSE("Лицензия"),
    CERTIFICATE("Сертификат"),
    NOTE("Заметка"),
    OTHER("Другое");

    companion object {
        fun fromName(value: String?): CategoryType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
    }
}

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: CategoryType = CategoryType.OTHER,
    val description: String = "",
    val colorSeed: Long = 0L,
    /** Публичная ссылка на найденный в интернете логотип бренда. */
    val iconUrl: String? = null,
    /** Имя локально закэшированного файла иконки в filesDir/icons. */
    val iconFile: String? = null,
    /** Домен бренда, по которому искали логотип (yandex.ru и т.п.). */
    val brandDomain: String? = null,
    val isSystem: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

data class SecretLink(val title: String, val url: String)

/**
 * Дополнительное поле секрета: подпись и значение. Нужно, когда в одной записи лежит
 * несколько важных значений (например, скриншот с логином, паролем и кодом восстановления).
 * Значение по умолчанию скрыто маской и открывается глазом.
 */
data class SecretField(val label: String = "", val value: String = "")

data class Secret(
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val type: CategoryType = CategoryType.OTHER,
    val title: String,
    /** Сам секрет: токен, номер карты, пароль и т.д. */
    val value: String,
    val description: String = "",
    val note: String = "",
    /** Дополнительные скрытые значения внутри одной записи. */
    val fields: List<SecretField> = emptyList(),
    val imageUrl: String? = null,
    val brandDomain: String? = null,
    /** Подробное описание, которое агент нашёл в интернете. */
    val detailedInfo: String = "",
    val links: List<SecretLink> = emptyList(),
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val foundByAgent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class AiConfig(
    val providerId: String = AiDefaults.PROVIDER_ID,
    val providerName: String = AiDefaults.PROVIDER_NAME,
    val baseUrl: String = AiDefaults.BASE_URL,
    val apiKey: String = "",
    val model: String = AiDefaults.MODEL,
    val visionModel: String = "",
    val temperature: Double = 0.2,
    val enabled: Boolean = true,
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
    val effectiveVisionModel: String get() = visionModel.ifBlank { model }
}

data class UserProfile(
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val provider: String = "google",
    val isDemo: Boolean = true,
)

data class VaultData(
    val categories: List<Category> = emptyList(),
    val secrets: List<Secret> = emptyList(),
    val ai: AiConfig = AiConfig(),
    val profile: UserProfile? = null,
    /** Кэш иконок брендов: домен -> имя файла в filesDir/icons. */
    val brandIcons: Map<String, String> = emptyMap(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("version", 1)
        put("ai", aiToJson(ai))
        profile?.let { put("profile", profileToJson(it)) }
        put("categories", JSONArray().apply { categories.forEach { put(categoryToJson(it)) } })
        put("secrets", JSONArray().apply { secrets.forEach { put(secretToJson(it)) } })
        put("brandIcons", JSONObject().apply { brandIcons.forEach { (k, v) -> put(k, v) } })
    }

    companion object {
        fun fromJson(json: JSONObject): VaultData {
            val categories = json.optJSONArray("categories").mapObj { categoryFromJson(it) }
            val secrets = json.optJSONArray("secrets").mapObj { secretFromJson(it) }
            val ai = json.optJSONObject("ai")?.let { aiFromJson(it) } ?: AiConfig()
            val profile = json.optJSONObject("profile")?.let { profileFromJson(it) }
            val brandIcons = json.optJSONObject("brandIcons")?.let { obj ->
                buildMap { obj.keys().forEach { key -> obj.optString(key).ifBlank { null }?.let { put(key, it) } } }
            } ?: emptyMap()
            return VaultData(categories, secrets, ai, profile, brandIcons)
        }
    }
}

// ---------- (де)сериализация ----------

fun categoryToJson(c: Category): JSONObject = JSONObject().apply {
    put("id", c.id)
    put("name", c.name)
    put("type", c.type.name)
    put("description", c.description)
    put("colorSeed", c.colorSeed)
    c.iconUrl?.let { put("iconUrl", it) }
    c.iconFile?.let { put("iconFile", it) }
    c.brandDomain?.let { put("brandDomain", it) }
    put("isSystem", c.isSystem)
    put("createdAt", c.createdAt)
}

fun categoryFromJson(o: JSONObject): Category = Category(
    id = o.getString("id"),
    name = o.optString("name"),
    type = CategoryType.fromName(o.optString("type")),
    description = o.optString("description"),
    colorSeed = o.optLong("colorSeed"),
    iconUrl = o.optString("iconUrl").ifBlank { null },
    iconFile = o.optString("iconFile").ifBlank { null },
    brandDomain = o.optString("brandDomain").ifBlank { null },
    isSystem = o.optBoolean("isSystem"),
    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
)

fun secretToJson(s: Secret): JSONObject = JSONObject().apply {
    put("id", s.id)
    put("categoryId", s.categoryId)
    put("type", s.type.name)
    put("title", s.title)
    put("value", s.value)
    put("description", s.description)
    put("note", s.note)
    put("fields", JSONArray().apply {
        s.fields.forEach { put(JSONObject().put("label", it.label).put("value", it.value)) }
    })
    s.imageUrl?.let { put("imageUrl", it) }
    s.brandDomain?.let { put("brandDomain", it) }
    put("detailedInfo", s.detailedInfo)
    put("links", JSONArray().apply {
        s.links.forEach { put(JSONObject().put("title", it.title).put("url", it.url)) }
    })
    put("tags", JSONArray().apply { s.tags.forEach { put(it) } })
    put("isFavorite", s.isFavorite)
    put("foundByAgent", s.foundByAgent)
    put("createdAt", s.createdAt)
    put("updatedAt", s.updatedAt)
}

fun secretFromJson(o: JSONObject): Secret = Secret(
    id = o.getString("id"),
    categoryId = o.optString("categoryId"),
    type = CategoryType.fromName(o.optString("type")),
    title = o.optString("title"),
    value = o.optString("value"),
    description = o.optString("description"),
    note = o.optString("note"),
    fields = o.optJSONArray("fields").mapObj {
        SecretField(it.optString("label"), it.optString("value"))
    },
    imageUrl = o.optString("imageUrl").ifBlank { null },
    brandDomain = o.optString("brandDomain").ifBlank { null },
    detailedInfo = o.optString("detailedInfo"),
    links = o.optJSONArray("links").mapObj {
        SecretLink(it.optString("title"), it.optString("url"))
    },
    tags = o.optJSONArray("tags").stringList(),
    isFavorite = o.optBoolean("isFavorite"),
    foundByAgent = o.optBoolean("foundByAgent"),
    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
)

fun aiToJson(a: AiConfig): JSONObject = JSONObject().apply {
    put("providerId", a.providerId)
    put("providerName", a.providerName)
    put("baseUrl", a.baseUrl)
    put("apiKey", a.apiKey)
    put("model", a.model)
    put("visionModel", a.visionModel)
    put("temperature", a.temperature)
    put("enabled", a.enabled)
}

fun aiFromJson(o: JSONObject): AiConfig = AiConfig(
    providerId = o.optString("providerId", AiDefaults.PROVIDER_ID),
    providerName = o.optString("providerName", AiDefaults.PROVIDER_NAME),
    baseUrl = o.optString("baseUrl", AiDefaults.BASE_URL),
    apiKey = o.optString("apiKey"),
    model = o.optString("model", AiDefaults.MODEL),
    visionModel = o.optString("visionModel"),
    temperature = o.optDouble("temperature", 0.2),
    enabled = o.optBoolean("enabled", true),
)

fun profileToJson(p: UserProfile): JSONObject = JSONObject().apply {
    put("name", p.name)
    put("email", p.email)
    p.avatarUrl?.let { put("avatarUrl", it) }
    put("provider", p.provider)
    put("isDemo", p.isDemo)
}

fun profileFromJson(o: JSONObject): UserProfile = UserProfile(
    name = o.optString("name"),
    email = o.optString("email"),
    avatarUrl = o.optString("avatarUrl").ifBlank { null },
    provider = o.optString("provider", "google"),
    isDemo = o.optBoolean("isDemo", true),
)

inline fun <T> JSONArray?.mapObj(map: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    val out = ArrayList<T>(length())
    for (i in 0 until length()) optJSONObject(i)?.let { out.add(map(it)) }
    return out
}

fun JSONArray?.stringList(): List<String> {
    if (this == null) return emptyList()
    val out = ArrayList<String>(length())
    for (i in 0 until length()) out.add(optString(i))
    return out
}
