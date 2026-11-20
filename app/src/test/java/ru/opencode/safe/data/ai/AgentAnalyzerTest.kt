package ru.opencode.safe.data.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.opencode.safe.data.model.AiConfig
import ru.opencode.safe.data.model.Category
import ru.opencode.safe.data.model.CategoryType

class AgentAnalyzerTest {

    private val config = AiConfig(
        providerId = "test",
        providerName = "test",
        baseUrl = "https://example.com/v1",
        apiKey = "test-key",
        model = "test-model",
    )

    private class FakeChatModel(
        private val reply: String = "",
        private val failure: Exception? = null,
    ) : ChatModel {
        override suspend fun complete(
            config: AiConfig,
            system: String,
            userText: String,
            images: List<ByteArray>,
            jsonMode: Boolean,
        ): String {
            failure?.let { throw it }
            return reply
        }

        override suspend fun models(config: AiConfig): List<String> = listOf("a", "b")
    }

    @Test
    fun parsesModelJson() = runBlocking {
        val reply = """
            ```json
            {"type":"TOKEN","category_name":"Яндекс","title":"Токен Яндекс","value":"ya29.abc",
            "description":"OAuth-токен","brand_name":"Яндекс","brand_domain":"yandex.ru","confidence":0.9,
            "detailed_info":"Это OAuth-токен Яндекс. Перевыпуск: https://oauth.yandex.ru/",
            "links":[{"title":"OAuth","url":"https://oauth.yandex.ru/"}]}
            ```
        """.trimIndent()

        val analyzer = AgentAnalyzer(FakeChatModel(reply))
        val result = analyzer.analyze(AgentInput(text = "ya29.abc"), config, emptyList())

        assertEquals(AgentSource.AI, result.source)
        assertEquals(CategoryType.TOKEN, result.type)
        assertEquals("yandex.ru", result.brandDomain)
        assertEquals("ya29.abc", result.value)
        assertTrue(result.links.any { it.url.contains("oauth.yandex.ru") })
        assertTrue(result.detailedInfo.contains("OAuth"))
    }

    @Test
    fun matchesExistingCategoryByName() = runBlocking {
        val reply = """{"type":"TOKEN","category_name":"Яндекс","title":"t","value":"v","confidence":0.5}"""
        val categories = listOf(Category(id = "c1", name = "Яндекс", type = CategoryType.TOKEN))
        val result = AgentAnalyzer(FakeChatModel(reply)).analyze(AgentInput(text = "ya29.abc"), config, categories)
        assertEquals("c1", result.matchedCategoryId)
        assertEquals(false, result.isNewCategory)
    }

    @Test
    fun fallsBackToLocalOnError() = runBlocking {
        val analyzer = AgentAnalyzer(FakeChatModel(failure = AiException("boom")))
        val result = analyzer.analyze(AgentInput(text = "4111 1111 1111 1111"), config, emptyList())
        assertEquals(AgentSource.LOCAL, result.source)
        assertEquals(CategoryType.BANK_CARD, result.type)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun runsOfflineWhenNotConfigured() = runBlocking {
        val analyzer = AgentAnalyzer(FakeChatModel("not used"))
        val result = analyzer.analyze(AgentInput(text = "ghp_TESTONLYTESTONLYTESTONLYTESTONLY"), AiConfig(), emptyList())
        assertEquals(AgentSource.LOCAL, result.source)
        assertEquals("github.com", result.brandDomain)
    }

    @Test
    fun extractsJsonFromFences() {
        val obj = AgentAnalyzer.firstJsonObject("text before ```json\n{\"type\":\"NOTE\"}\n``` after")
        assertNotNull(obj)
        assertEquals("NOTE", obj!!.optString("type"))
    }
}
