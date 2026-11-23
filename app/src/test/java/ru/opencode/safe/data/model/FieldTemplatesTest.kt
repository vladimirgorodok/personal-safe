package ru.opencode.safe.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldTemplatesTest {

    @Test
    fun bankCardHasCopyableStandards() {
        val labels = FieldTemplates.labels(CategoryType.BANK_CARD)
        assertTrue(labels.contains("Срок действия"))
        assertTrue(labels.contains("CVV/CVC"))
        assertTrue(labels.contains("PIN"))
        assertTrue(labels.contains("Держатель"))
    }

    @Test
    fun passportAndWifiHaveTheirOwnFields() {
        assertTrue(FieldTemplates.labels(CategoryType.PASSPORT).contains("Код подразделения"))
        assertTrue(FieldTemplates.labels(CategoryType.WIFI).contains("Сеть (SSID)"))
    }

    @Test
    fun noteHasNoTemplate() {
        assertFalse(FieldTemplates.hasTemplate(CategoryType.NOTE))
        assertFalse(FieldTemplates.hasTemplate(CategoryType.OTHER))
        assertTrue(FieldTemplates.seed(CategoryType.NOTE).isEmpty())
    }

    @Test
    fun seedFillsLabelsOnly() {
        val seeded = FieldTemplates.seed(CategoryType.BANK_CARD)
        assertEquals(FieldTemplates.labels(CategoryType.BANK_CARD).size, seeded.size)
        assertTrue(seeded.all { it.label.isNotBlank() && it.value.isBlank() })
    }

    @Test
    fun suggestedSkipsLabelsAlreadyPresent() {
        val existing = listOf(SecretField("cvv/cvc", "123"), SecretField("  Срок действия  ", "12/28"))
        val suggested = FieldTemplates.suggested(CategoryType.BANK_CARD, existing)
        assertFalse(suggested.contains("CVV/CVC"))
        assertFalse(suggested.contains("Срок действия"))
        assertTrue(suggested.contains("PIN"))
    }

    @Test
    fun suggestedCoversWholeTemplateForEmptyRecord() {
        val suggested = FieldTemplates.suggested(CategoryType.ACCOUNT, emptyList())
        assertEquals(FieldTemplates.labels(CategoryType.ACCOUNT), suggested)
    }

    @Test
    fun filledSummaryCountsOnlyFilledFields() {
        assertNull(FieldTemplates.filledSummary(emptyList()))
        assertNull(FieldTemplates.filledSummary(listOf(SecretField("PIN"))))
        assertEquals("+1 значение", FieldTemplates.filledSummary(listOf(SecretField("PIN", "1234"))))
        assertEquals(
            "+3 значения",
            FieldTemplates.filledSummary(
                listOf(SecretField("a", "1"), SecretField("b", "2"), SecretField("c", "3"), SecretField("d")),
            ),
        )
        assertEquals("+5 значений", FieldTemplates.filledSummary(List(5) { SecretField("f$it", "v") }))
        assertEquals("+11 значений", FieldTemplates.filledSummary(List(11) { SecretField("f$it", "v") }))
    }
}
