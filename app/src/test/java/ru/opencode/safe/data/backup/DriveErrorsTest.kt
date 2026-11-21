package ru.opencode.safe.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveErrorsTest {

    private fun body(reason: String, message: String) =
        """{"error":{"code":403,"message":"$message","errors":[{"reason":"$reason"}],"status":"PERMISSION_DENIED"}}"""

    @Test
    fun `выключенный Drive API распознаётся и даёт кнопку включения`() {
        val text = DriveErrors.describe(
            403,
            body("accessNotConfigured", "Google Drive API has not been used in project 123 before or it is disabled."),
            "не удалось получить список копий",
        )
        assertTrue(text.contains("Google Drive API"))
        assertTrue(text.contains("library/drive.googleapis.com"))
        assertTrue(DriveErrors.needsApiEnable(text))
    }

    @Test
    fun `недостаточные права подсказывают про область appdata`() {
        val text = DriveErrors.describe(403, body("insufficientPermissions", "Insufficient Permission"), "не удалось")
        assertTrue(text.contains("drive.appdata"))
        assertFalse(DriveErrors.needsApiEnable(text))
    }

    @Test
    fun `лимит запросов просит подождать`() {
        val text = DriveErrors.describe(403, body("rateLimitExceeded", "Rate Limit Exceeded"), "не удалось")
        assertTrue(text.contains("Подождите"))
    }

    @Test
    fun `истёкший доступ просит войти заново`() {
        val text = DriveErrors.describe(401, "", "не удалось")
        assertTrue(text.contains("войдите"))
    }

    @Test
    fun `ответ Google попадает в текст ошибки`() {
        val text = DriveErrors.describe(403, body("whatever", "Something specific happened"), "не удалось")
        assertTrue(text.contains("Something specific happened"))
    }

    @Test
    fun `непонятная ошибка показывает код`() {
        val text = DriveErrors.describe(500, "", "сервер вернул")
        assertTrue(text.contains("500"))
    }

    @Test
    fun `неверный параметр запроса назван ошибкой приложения`() {
        val text = DriveErrors.describe(400, body("invalid", "Invalid Value"), "не удалось получить список копий")
        assertTrue(text.contains("неверный параметр"))
        assertFalse(DriveErrors.needsApiEnable(text))
    }
}
