package ru.opencode.safe.data.ai

import okhttp3.Headers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiErrorsTest {

    @Test
    fun parsesCompoundDuration() {
        assertEquals(150L, AiErrors.parseDuration("2m30s"))
        assertEquals(180L, AiErrors.parseDuration("2m59.56s"))
    }

    @Test
    fun parsesSingleUnitDuration() {
        assertEquals(45L, AiErrors.parseDuration("45s"))
        assertEquals(45L, AiErrors.parseDuration("45"))
        assertEquals(3600L, AiErrors.parseDuration("1h"))
    }

    @Test
    fun retryAfterIsUsedWhenPresent() {
        val message = AiErrors.tooManyRequests(Headers.headersOf("Retry-After", "120"), "rate limit")
        assertTrue(message.contains("Лимит бесплатной модели исчерпан"))
        assertTrue(message.contains("2 мин."))
        assertTrue(message.contains("rate limit"))
    }

    @Test
    fun shortResetHeaderMeansPerMinuteLimit() {
        val message = AiErrors.tooManyRequests(Headers.headersOf("x-ratelimit-reset-requests", "30s"), "")
        assertTrue(message.contains("30 сек."))
        assertTrue(message.contains("дневной лимит ещё не исчерпан"))
    }

    @Test
    fun withoutHeadersMentionsRenewalTime() {
        val message = AiErrors.tooManyRequests(Headers.headersOf(), "")
        assertTrue(message.contains("03:00"))
    }
}
