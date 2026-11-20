package ru.opencode.safe.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.opencode.safe.data.model.Category
import ru.opencode.safe.data.model.CategoryType
import ru.opencode.safe.data.model.Secret

class BrandGrouperTest {

    private fun secret(
        id: String,
        title: String = id,
        value: String = "value",
        categoryId: String = "cat",
        brandDomain: String? = null,
        tags: List<String> = emptyList(),
        description: String = "",
    ) = Secret(
        id = id,
        categoryId = categoryId,
        title = title,
        value = value,
        brandDomain = brandDomain,
        tags = tags,
        description = description,
    )

    @Test
    fun groupsByExplicitBrandDomain() {
        val groups = BrandGrouper.group(
            listOf(
                secret("a", brandDomain = "github.com"),
                secret("b", brandDomain = "github.com"),
                secret("c", brandDomain = "yandex.ru"),
            ),
            emptyList(),
        )
        assertEquals(2, groups.size)
        assertEquals("GitHub", groups.first().name)
        assertEquals(2, groups.first().secrets.size)
    }

    @Test
    fun groupsByCategoryBrandDomain() {
        val category = Category(id = "cat", name = "Яндекс", brandDomain = "yandex.ru", type = CategoryType.ACCOUNT)
        val groups = BrandGrouper.group(listOf(secret("a", categoryId = "cat")), listOf(category))
        assertEquals(1, groups.size)
        assertEquals("yandex.ru", groups.first().domain)
        assertEquals("Яндекс", groups.first().name)
    }

    @Test
    fun groupsByTextDetection() {
        val groups = BrandGrouper.group(
            listOf(secret("a", title = "Промокод Ozon", description = "скидка в озоне")),
            emptyList(),
        )
        assertEquals(1, groups.size)
        assertEquals("ozon.ru", groups.first().domain)
    }

    @Test
    fun skipsSecretsWithoutBrand() {
        val groups = BrandGrouper.group(listOf(secret("a", title = "Моя заметка")), emptyList())
        assertTrue(groups.isEmpty())
    }

    @Test
    fun resolveBrandReturnsNullForUnknown() {
        assertNull(BrandGrouper.resolveBrand(secret("a", title = "Просто запись"), null))
    }
}
