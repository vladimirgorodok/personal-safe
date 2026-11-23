package ru.opencode.safe.data.ai

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.opencode.safe.data.model.CategoryType

class AgentFieldsTest {

    private fun parse(json: String, type: CategoryType = CategoryType.BANK_CARD) =
        AgentAnalyzer.parseFields(JSONObject(json), type)

    @Test
    fun readsArrayOfObjects() {
        val fields = parse("""{"fields":[{"label":"Срок действия","value":"12/28"},{"label":"CVV","value":"123"}]}""")
        val map = fields.associate { it.label to it.value }
        assertEquals("12/28", map["Срок действия"])
        // «CVV» приводится к подписи из шаблона
        assertEquals("123", map["CVV/CVC"])
        assertEquals(2, fields.size)
    }

    @Test
    fun readsObjectDictionary() {
        val map = parse("""{"fields":{"Срок действия":"12/28","Держатель":"IVAN IVANOV"}}""")
            .associate { it.label to it.value }
        assertEquals("12/28", map["Срок действия"])
        assertEquals("IVAN IVANOV", map["Держатель"])
    }

    @Test
    fun readsStringForm() {
        val map = parse("""{"fields":"Срок действия: 12/28\nCVV: 123"}""").associate { it.label to it.value }
        assertEquals("12/28", map["Срок действия"])
        assertEquals("123", map["CVV/CVC"])
    }

    @Test
    fun readsTopLevelFieldKeys() {
        val map = parse("""{"type":"BANK_CARD","Срок действия":"12/28","title":"Карта"}""")
            .associate { it.label to it.value }
        assertEquals("12/28", map["Срок действия"])
    }

    @Test
    fun dropsEmptyValuesAndDuplicates() {
        val fields = parse(
            """{"fields":[{"label":"PIN","value":""},{"label":"PIN","value":"4321"},{"label":"PIN","value":"9999"}]}""",
        )
        assertEquals(1, fields.size)
        assertEquals("4321", fields.first().value)
    }

    @Test
    fun keepsUnknownLabelsFromModel() {
        val map = parse("""{"fields":[{"label":"Кэшбэк","value":"5%"}]}""").associate { it.label to it.value }
        assertEquals("5%", map["Кэшбэк"])
    }

    @Test
    fun emptyAnswerGivesNoFields() {
        assertTrue(parse("""{"type":"NOTE"}""").isEmpty())
        assertTrue(parse("""{"fields":[]}""").isEmpty())
    }

    @Test
    fun promptCatalogMentionsBankCardFields() {
        val catalog = ru.opencode.safe.data.model.FieldTemplates.promptCatalog()
        assertTrue(catalog.contains("BANK_CARD: Срок действия, CVV/CVC"))
        assertTrue(catalog.contains("PASSPORT:"))
    }
}
