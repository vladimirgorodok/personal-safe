package ru.opencode.safe.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleErrorHintTest {

    @Test
    fun `отмена входа распознаётся`() {
        val hint = googleErrorHint(IllegalStateException("User cancelled the credential request"))
        assertTrue(hint.contains("отменён", ignoreCase = true))
    }

    @Test
    fun `developer error подсказывает про Android-клиент`() {
        val hint = googleErrorHint(IllegalStateException("10: Developer console is not set up correctly"))
        assertTrue(hint.contains("SHA-1"))
    }

    @Test
    fun `ошибка 28444 распознаётся как неверный developer console`() {
        val hint = googleErrorHint(IllegalStateException("[28444] Developer console is not set up correctly."))
        assertTrue(hint.contains("28444"))
        assertTrue(hint.contains("Android-клиент"))
    }

    @Test
    fun `ошибка про веб-клиент подсказывает про Web application`() {
        val hint = googleErrorHint(IllegalStateException("serverClientId must be a web client ID"))
        assertTrue(hint.contains("Web application"))
    }

    @Test
    fun `отсутствие аккаунтов подсказывает про аккаунт, Test users и VPN`() {
        val hint = googleErrorHint(IllegalStateException("No credentials available"))
        assertTrue(hint.contains("Test users"))
        assertTrue(hint.contains("Google-аккаунт"))
        assertTrue(hint.contains("VPN"))
    }

    @Test
    fun `блокировка входа подсказывает про consent screen`() {
        val hint = googleErrorHint(IllegalStateException("Access blocked: this app's request is invalid"))
        assertTrue(hint.contains("consent screen", ignoreCase = true))
    }

    @Test
    fun `неизвестная ошибка не теряет технический текст`() {
        val described = describeGoogleError(RuntimeException("something odd"))
        assertTrue(described.contains("Технически:"))
        assertTrue(described.contains("something odd"))
        assertTrue(described.contains("RuntimeException"))
    }
}
