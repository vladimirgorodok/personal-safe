package ru.opencode.safe.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.opencode.safe.data.model.CategoryType
import java.util.Base64

class HeuristicsTest {

    @Test
    fun luhnAcceptsValidCard() {
        assertTrue(Heuristics.luhn("4111111111111111"))
        assertTrue(Heuristics.luhn("5555555555554444"))
    }

    @Test
    fun luhnRejectsInvalidCard() {
        assertEquals(false, Heuristics.luhn("4111111111111112"))
    }

    @Test
    fun detectsVisaCard() {
        val guess = Heuristics.classify("4111 1111 1111 1111")
        assertEquals(CategoryType.BANK_CARD, guess.type)
        assertEquals("Visa", guess.brand?.name)
    }

    @Test
    fun detectsMirCard() {
        val guess = Heuristics.classify("2202200000000008")
        assertEquals(CategoryType.BANK_CARD, guess.type)
        assertEquals("МИР", guess.brand?.name)
    }

    @Test
    fun detectsPin() {
        assertEquals(CategoryType.PIN, Heuristics.classify("4821").type)
    }

    @Test
    fun detectsGithubToken() {
        val guess = Heuristics.classify("ghp_TESTONLYTESTONLYTESTONLYTESTONLY")
        assertEquals(CategoryType.TOKEN, guess.type)
        assertEquals("github.com", guess.brand?.domain)
    }

    @Test
    fun detectsOpenAiApiKey() {
        val guess = Heuristics.classify("sk-TESTONLYTESTONLYTESTONLYTESTONLY")
        assertEquals(CategoryType.API_KEY, guess.type)
        assertEquals("openai.com", guess.brand?.domain)
    }

    @Test
    fun detectsTelegramBotToken() {
        val token = "111111111:TESTONLYTESTONLYTESTONLYTESTONLY"
        val guess = Heuristics.classify(token)
        assertEquals("telegram.org", guess.brand?.domain)
    }

    @Test
    fun detectsGoogleOAuthToken() {
        val guess = Heuristics.classify("ya29.TESTONLYTESTONLY")
        assertEquals("accounts.google.com", guess.brand?.domain)
        assertEquals(CategoryType.TOKEN, guess.type)
    }

    @Test
    fun detectsJwtIssuer() {
        val payload = """{"iss":"https://accounts.google.com","sub":"12345"}"""
        val jwt = encode("""{"alg":"HS256","typ":"JWT"}""") + "." + encode(payload) + ".signature"
        val guess = Heuristics.classify(jwt)
        assertEquals(CategoryType.TOKEN, guess.type)
        assertEquals("accounts.google.com", guess.brand?.domain)
    }

    @Test
    fun detectsImageUrl() {
        assertTrue(Heuristics.isImageUrl("https://example.com/pic.png?size=2"))
        assertTrue(Heuristics.isImageUrl("https://site.ru/favicon.ico".replace(".ico", ".png")))
        assertEquals(false, Heuristics.isImageUrl("https://example.com/page.html"))
    }

    @Test
    fun extractsLinks() {
        val links = Heuristics.extractLinks("Смотри https://a.com/x и https://b.ru/y?z=1 конец")
        assertEquals(2, links.size)
    }

    @Test
    fun detectsPassport() {
        assertEquals(CategoryType.PASSPORT, Heuristics.classify("Паспорт 4510 123456 выдан").type)
    }

    @Test
    fun detectsSberCardByBin() {
        val guess = Heuristics.classify("4276010012345672")
        assertEquals(CategoryType.BANK_CARD, guess.type)
        assertEquals("Сбербанк", guess.brand?.name)
        assertTrue(guess.description.contains("Visa"))
    }

    @Test
    fun detectsTbankCardByBin() {
        val guess = Heuristics.classify("5536910012345676")
        assertEquals("Т-Банк (Тинькофф)", guess.brand?.name)
    }

    @Test
    fun detectsShopifyToken() {
        assertEquals("shopify.com", Heuristics.classify("shpat_TESTONLYTESTONLYTESTONLY").brand?.domain)
    }

    @Test
    fun detectsMailgunKey() {
        assertEquals("mailgun.com", Heuristics.classify("key-TESTONLYTESTONLYTESTONLYTESTONLY").brand?.domain)
    }

    @Test
    fun detectsHuggingFaceToken() {
        assertEquals(CategoryType.API_KEY, Heuristics.classify("hf_TESTONLYTESTONLYTESTONLYTESTONLY").type)
    }

    @Test
    fun detectsAlibabaKey() {
        assertEquals("alibabacloud.com", Heuristics.classify("LTAI5tTESTONLYTESTONLYTESTONLY").brand?.domain)
    }

    @Test
    fun detectsGitlabDeployToken() {
        assertEquals("gitlab.com", Heuristics.classify("glrt-TESTONLYTESTONLYTESTONLY").brand?.domain)
    }

    @Test
    fun detectsRussianBrandAlias() {
        assertEquals("ozon.ru", BrandCatalog.detect("пароль от озон")?.domain)
        assertEquals("wildberries.ru", BrandCatalog.detect("wildberries аккаунт")?.domain)
    }

    @Test
    fun knowsBankGuide() {
        val issuer = BankBin.issuerFor("4276010012345672")
        assertNotNull(issuer)
        val guide = KnowledgeBase.guideFor(Brand(issuer!!.name, issuer.domain), CategoryType.BANK_CARD, "4276010012345672")
        assertNotNull(guide)
        assertTrue(guide!!.summary.contains("Сбербанк"))
    }

    private fun encode(json: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
}
