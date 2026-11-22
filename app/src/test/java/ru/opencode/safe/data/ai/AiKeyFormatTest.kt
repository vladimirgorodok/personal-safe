package ru.opencode.safe.data.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiKeyFormatTest {

    @Test
    fun acceptsTypicalKeys() {
        assertTrue(AiKeyFormat.looksLikeApiKey("gsk_abcdefghijklmnopqrstuvwxyz012345"))
        assertTrue(AiKeyFormat.looksLikeApiKey("sk-or-v1-0123456789abcdef0123456789abcdef"))
        assertTrue(AiKeyFormat.looksLikeApiKey("AIzaSyA1234567890abcdefghijklmnopqrst"))
        assertTrue(AiKeyFormat.looksLikeApiKey("  AIzaSyA1234567890abcdefghijklmnopqrst  "))
    }

    @Test
    fun rejectsNoise() {
        assertFalse(AiKeyFormat.looksLikeApiKey(null))
        assertFalse(AiKeyFormat.looksLikeApiKey(""))
        assertFalse(AiKeyFormat.looksLikeApiKey("короткий"))
        assertFalse(AiKeyFormat.looksLikeApiKey("это не ключ, а просто текст с пробелами"))
        assertFalse(AiKeyFormat.looksLikeApiKey("ключ-из-кириллицы-длиной-больше-двадцати"))
    }

    @Test
    fun detectsVisionModelsByName() {
        assertTrue(AiKeyFormat.looksLikeVisionModel("deepseek-v4-flash-vision-exp"))
        assertTrue(AiKeyFormat.looksLikeVisionModel("qwen2-vl-72b-instruct"))
        assertTrue(AiKeyFormat.looksLikeVisionModel("gemini-4-hydrogen"))
        assertTrue(AiKeyFormat.looksLikeVisionModel("gpt-4o-mini"))
    }

    @Test
    fun leavesTextOnlyModelsAlone() {
        assertFalse(AiKeyFormat.looksLikeVisionModel("deepseek-v4-flash"))
        assertFalse(AiKeyFormat.looksLikeVisionModel("text-embedding-3-small"))
        assertFalse(AiKeyFormat.looksLikeVisionModel("whisper-1"))
    }
}
