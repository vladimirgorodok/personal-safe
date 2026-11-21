package ru.opencode.safe

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Сторож: Client ID Google-входа вшивается в сборку, и без него кнопка «Войти через Google»
 * не сработает у обычного пользователя. Тест не даёт выпустить сборку с пустым или битым ID.
 */
class GoogleClientIdTest {

    @Test
    fun defaultClientIdIsBakedIn() {
        val id = BuildConfig.GOOGLE_CLIENT_ID.trim()
        assertTrue("Client ID не задан", id.isNotBlank())
        assertTrue("Client ID не похож на OAuth-идентификатор: $id", id.endsWith(".apps.googleusercontent.com"))
    }
}
