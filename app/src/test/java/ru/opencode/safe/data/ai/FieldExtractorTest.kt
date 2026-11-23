package ru.opencode.safe.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.opencode.safe.data.model.CategoryType

class FieldExtractorTest {

    @Test
    fun readsBankCardFromText() {
        val text = """
            Карта 4276 3800 1234 5678
            Срок действия 12/28
            CVV: 123
            PIN: 4321
            IVAN IVANOV
        """.trimIndent()

        val fields = FieldExtractor.extract(CategoryType.BANK_CARD, text).associate { it.label to it.value }

        assertEquals("12/28", fields["Срок действия"])
        assertEquals("123", fields["CVV/CVC"])
        assertEquals("4321", fields["PIN"])
        assertEquals("IVAN IVANOV", fields["Держатель"])
        assertEquals("Сбербанк", fields["Банк"])
        assertEquals("Visa", fields["Платёжная система"])
    }

    @Test
    fun doesNotInventFieldsForPlainCardNumber() {
        val fields = FieldExtractor.extract(CategoryType.BANK_CARD, "4276 3800 1234 5678")
        val labels = fields.map { it.label }
        assertTrue(labels.contains("Платёжная система"))
        assertFalse(labels.contains("Держатель"))
        assertFalse(labels.contains("PIN"))
    }

    @Test
    fun neverReturnsEmptyValues() {
        val text = "срок действия CVV PIN держатель банк"
        assertTrue(FieldExtractor.extract(CategoryType.BANK_CARD, text).all { it.value.isNotBlank() })
        assertTrue(FieldExtractor.extract(CategoryType.PASSPORT, text).all { it.value.isNotBlank() })
        assertFalse(FieldExtractor.extract(CategoryType.BANK_CARD, "").any { it.value.isNotBlank() })
    }

    @Test
    fun readsPassportFields() {
        val text = "Паспорт 45 08 123456 выдан 12.05.2019 код подразделения 770-001"
        val fields = FieldExtractor.extract(CategoryType.PASSPORT, text).associate { it.label to it.value }
        assertEquals("45 08 123456", fields["Серия и номер"])
        assertEquals("770-001", fields["Код подразделения"])
        assertEquals("12.05.2019", fields["Дата выдачи"])
    }

    @Test
    fun readsWifiFields() {
        val text = "SSID: HomeNet\nПароль: super-secret-1"
        val fields = FieldExtractor.extract(CategoryType.WIFI, text).associate { it.label to it.value }
        assertEquals("HomeNet", fields["Сеть (SSID)"])
        assertEquals("super-secret-1", fields["Пароль"])
    }

    @Test
    fun readsAccountFields() {
        val text = "Логин: ivan@example.com\nПароль: qwerty123"
        val fields = FieldExtractor.extract(CategoryType.ACCOUNT, text).associate { it.label to it.value }
        assertEquals("ivan@example.com", fields["Логин"])
        assertEquals("qwerty123", fields["Пароль"])
    }

    @Test
    fun readsEndpointForApiKey() {
        val fields = FieldExtractor.extract(CategoryType.API_KEY, "Ключ sk-test\nhttps://api.example.com/v1")
        assertEquals("https://api.example.com/v1", fields.first { it.label == "Endpoint" }.value)
    }
}
