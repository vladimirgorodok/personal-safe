package ru.opencode.safe.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.opencode.safe.data.model.AiConfig
import ru.opencode.safe.data.model.aiFromJson
import ru.opencode.safe.data.model.aiToJson

class AiProvidersTest {

    private val groqKey = "gsk_abcdefghijklmnopqrstuvwxyz012345"
    private val tunnelKey = "sk-or-v1-0123456789abcdef0123456789abcdef"

    @Test
    fun listsKnownProvidersWithKeysLinks() {
        assertTrue(AiProviders.all.any { it.id == AiDefaults.PROVIDER_ID && it.keysUrl.isNotBlank() })
        assertEquals(AiProviders.all.size, AiProviders.all.map { it.id }.distinct().size)
    }

    @Test
    fun switchingRemembersKeyOfPreviousProvider() {
        val start = AiConfig(
            providerId = AiDefaults.PROVIDER_ID,
            providerName = "Groq — бесплатно",
            baseUrl = AiDefaults.BASE_URL,
            apiKey = groqKey,
            model = AiDefaults.MODEL,
        )

        val toTunnel = AiProviders.switchTo(start, "aitunnel")
        assertEquals("aitunnel", toTunnel.providerId)
        assertEquals("https://api.aitunnel.ru/v1", toTunnel.baseUrl)
        assertEquals("gpt-4o-mini", toTunnel.model)
        assertEquals("", toTunnel.apiKey)
        assertEquals(groqKey, toTunnel.apiKeys[AiDefaults.PROVIDER_ID])

        // Вернулись обратно — ключ Groq на месте, менять ничего не нужно.
        val back = AiProviders.switchTo(toTunnel.copy(apiKey = tunnelKey), AiDefaults.PROVIDER_ID)
        assertEquals(groqKey, back.apiKey)
        assertEquals(tunnelKey, back.apiKeys["aitunnel"])
        assertEquals(AiDefaults.MODEL, back.model)
    }

    @Test
    fun customProviderKeepsManualAddress() {
        val current = AiConfig(
            providerId = "aitunnel",
            providerName = "aitunnel",
            baseUrl = "https://api.aitunnel.ru/v1",
            apiKey = tunnelKey,
            model = "deepseek-v4-flash",
        )
        val custom = AiProviders.switchTo(current, AiProviders.CUSTOM_ID)
        assertEquals("https://api.aitunnel.ru/v1", custom.baseUrl)
        assertEquals("deepseek-v4-flash", custom.model)
        assertEquals(tunnelKey, custom.apiKeys["aitunnel"])
    }

    @Test
    fun hasKeyLooksAtActiveAndRememberedKeys() {
        val config = AiConfig(
            providerId = AiDefaults.PROVIDER_ID,
            apiKey = groqKey,
            apiKeys = mapOf("aitunnel" to tunnelKey),
        )
        assertTrue(AiProviders.hasKey(config, AiDefaults.PROVIDER_ID))
        assertTrue(AiProviders.hasKey(config, "aitunnel"))
        assertFalse(AiProviders.hasKey(config, "openai"))
    }

    @Test
    fun normalizedKeepsActiveKeyInMemory() {
        val config = AiConfig(providerId = "openai", apiKey = groqKey)
        assertEquals(groqKey, AiProviders.normalized(config).apiKeys["openai"])
    }

    @Test
    fun optionsMarkProvidersWithSavedKey() {
        val config = AiConfig(providerId = AiDefaults.PROVIDER_ID, apiKey = groqKey)
        val options = AiProviders.options(config)
        assertTrue(options.first { it.id == AiDefaults.PROVIDER_ID }.hasKey)
        assertFalse(options.first { it.id == "openai" }.hasKey)
        assertEquals(AiProviders.all.size, options.size)
    }

    @Test
    fun jsonRoundTripKeepsPerProviderKeys() {
        val config = AiProviders.normalized(
            AiConfig(providerId = "aitunnel", apiKey = tunnelKey, apiKeys = mapOf(AiDefaults.PROVIDER_ID to groqKey)),
        )
        val restored = aiFromJson(aiToJson(config))
        assertEquals(config.apiKey, restored.apiKey)
        assertEquals(groqKey, restored.apiKeys[AiDefaults.PROVIDER_ID])
        assertEquals(tunnelKey, restored.apiKeys["aitunnel"])
    }
}
