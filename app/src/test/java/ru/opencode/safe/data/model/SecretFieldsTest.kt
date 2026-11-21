package ru.opencode.safe.data.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class SecretFieldsTest {

    @Test
    fun extraFieldsSurviveJsonRoundTrip() {
        val secret = Secret(
            categoryId = "cat",
            title = "Банк",
            value = "1111 2222",
            fields = listOf(
                SecretField("Логин", "user@example.test"),
                SecretField("Код восстановления", "0000"),
            ),
        )
        val restored = secretFromJson(JSONObject(secretToJson(secret).toString()))
        assertEquals(2, restored.fields.size)
        assertEquals("Логин", restored.fields[0].label)
        assertEquals("user@example.test", restored.fields[0].value)
        assertEquals("Код восстановления", restored.fields[1].label)
        assertEquals("0000", restored.fields[1].value)
    }

    @Test
    fun oldRecordsWithoutFieldsReadAsEmpty() {
        val json = JSONObject(secretToJson(Secret(categoryId = "c", title = "t", value = "v")).toString())
        json.remove("fields")
        assertEquals(emptyList<SecretField>(), secretFromJson(json).fields)
    }
}
