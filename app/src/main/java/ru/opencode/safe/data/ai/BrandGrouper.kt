package ru.opencode.safe.data.ai

import ru.opencode.safe.data.model.Category
import ru.opencode.safe.data.model.Secret

/** Группа секретов, относящихся к одному бренду. */
data class BrandGroup(
    val key: String,
    val name: String,
    val domain: String?,
    val secrets: List<Secret>,
)

/**
 * Группировка секретов по бренду: «Яндекс → все секреты».
 * Бренд берётся из самого секрета, из его категории (по домену или названию)
 * либо распознаётся по тексту.
 */
object BrandGrouper {

    fun group(secrets: List<Secret>, categories: List<Category>): List<BrandGroup> {
        val byId = categories.associateBy { it.id }
        val groups = LinkedHashMap<String, MutableList<Secret>>()
        val meta = HashMap<String, Pair<String, String?>>()

        for (secret in secrets) {
            val category = byId[secret.categoryId]
            val brand = resolveBrand(secret, category) ?: continue
            val key = brand.domain
            groups.getOrPut(key) { mutableListOf() }.add(secret)
            meta[key] = brand.name to brand.domain
        }

        return groups.entries
            .sortedByDescending { it.value.size }
            .map { (key, list) ->
                val (name, domain) = meta[key] ?: key to key
                BrandGroup(key = key, name = name, domain = domain, secrets = list)
            }
    }

    fun groupFor(secret: Secret, category: Category?): BrandGroup? {
        val brand = resolveBrand(secret, category) ?: return null
        return BrandGroup(brand.domain, brand.name, brand.domain, listOf(secret))
    }

    /** Возвращает бренд для секрета или null, если бренд не распознан. */
    fun resolveBrand(secret: Secret, category: Category?): Brand? {
        secret.brandDomain?.takeIf { it.isNotBlank() }?.let { domain ->
            return BrandCatalog.byDomain(domain) ?: Brand(prettyName(domain), domain)
        }
        category?.brandDomain?.takeIf { it.isNotBlank() }?.let { domain ->
            return BrandCatalog.byDomain(domain) ?: Brand(category.name, domain)
        }
        val haystack = buildString {
            append(secret.title).append(' ')
            append(secret.tags.joinToString(" ")).append(' ')
            append(secret.description).append(' ')
            if (category != null) append(category.name)
        }
        BrandCatalog.detect(haystack)?.let { return it }
        return null
    }

    private fun prettyName(domain: String): String =
        domain.substringBefore('.').replace('-', ' ')
            .replaceFirstChar { it.uppercaseChar() }
}
