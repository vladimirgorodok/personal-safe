package ru.opencode.safe.data.ai

import ru.opencode.safe.data.model.CategoryType
import ru.opencode.safe.data.model.SecretField

/**
 * Офлайн-разбор значений по типовым полям.
 *
 * Нужен, когда модель недоступна или не вернула поля: из текста достаём то, что видно однозначно —
 * срок действия, CVV, PIN, держателя карты, серию паспорта, SSID и пароль Wi-Fi. Пустых полей
 * здесь не бывает: поле появляется только при явном совпадении.
 */
object FieldExtractor {

    // Границы слова: \b в Java не считает кириллицу словом, поэтому используем Unicode-классы.
    private const val START = """(?<![\p{L}\p{N}])"""
    private const val END = """(?![\p{L}\p{N}])"""

    private val cardDigits = Regex("""\d[\d\s\-]{11,23}\d""")
    private val expiry = Regex("""\b(0[1-9]|1[0-2])[/\-.](\d{2}|\d{4})\b""")
    private val cvv = Regex("""(?iu)(?:cvv2?|cvc2?|cid)\D{0,4}(\d{3,4})(?!\d)""")
    private val pin = Regex("""(?iu)${START}pin\D{0,4}(\d{4,6})(?!\d)""")
    private val holder = Regex("""${START}([A-ZА-ЯЁ]{2,}(?:\s+[A-ZА-ЯЁ]{2,}){1,3})${END}""")
    private val passportRf = Regex("""\b\d{2}\s?\d{2}\s?\d{6}\b""")
    private val subdivision = Regex("""\b\d{3}-\d{3}\b""")
    private val dateRu = Regex("""\b\d{2}[.\-/]\d{2}[.\-/]\d{4}\b""")
    private val ssid = Regex("""(?iu)${START}(?:ssid|сеть)\s*[:=]?\s*"?([^"\n;]{2,})""")
    private val wifiPassword = Regex("""(?iu)${START}(?:password|пароль|psk|ключ)\s*[:=]?\s*"?([^\s"\n;,]{3,})""")
    private val loginField = Regex("""(?iu)${START}(?:login|логин|user(?:name)?)\s*[:=]?\s*([^\s,;]+)""")
    private val passwordField = Regex("""(?iu)${START}(?:password|пароль|pass)\s*[:=]?\s*([^\s,;]+)""")
    private val email = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
    private val url = Regex("""https?://[^\s"'<>)\]]+""")

    /** Подписи, которые не могут быть именем держателя карты. */
    private val holderNoise = setOf(
        "CVV", "CVV2", "CVC", "CVC2", "CID", "PIN", "PUK", "PSK", "SSID", "WPA", "WPA2", "WEP",
        "API", "URL", "HTTP", "HTTPS", "СРОК", "ДЕЙСТВИЯ", "ПАРОЛЬ", "ЛОГИН", "СЕРИЯ", "НОМЕР",
        "БАНК", "КАРТА", "КАРТЫ", "ИТОГО", "ВЛАДЕЛЕЦ", "ДЕРЖАТЕЛЬ",
    )

    /** Заполненные типовые поля, найденные в тексте. */
    fun extract(type: CategoryType, text: String): List<SecretField> {
        if (text.isBlank()) return emptyList()
        return when (type) {
            CategoryType.BANK_CARD -> bankCard(text)
            CategoryType.PASSPORT, CategoryType.ID_CARD -> passport(text)
            CategoryType.WIFI -> wifi(text)
            CategoryType.ACCOUNT, CategoryType.PASSWORD -> credentials(text)
            CategoryType.API_KEY, CategoryType.TOKEN -> apiKey(text)
            else -> emptyList()
        }
    }

    private fun bankCard(text: String): List<SecretField> {
        val card = cardDigits.findAll(text)
            .map { Heuristics.digitsOnly(it.value) }
            .filter { it.length in 13..19 }
            .let { runs -> runs.firstOrNull { Heuristics.luhn(it) } ?: runs.firstOrNull() }

        return buildList {
            expiry.find(text)?.let { match ->
                add(SecretField("Срок действия", match.value.replace('-', '/').replace('.', '/')))
            }
            cvv.find(text)?.let { add(SecretField("CVV/CVC", it.groupValues[1])) }
            pin.find(text)?.let { add(SecretField("PIN", it.groupValues[1])) }
            holder.find(text)?.groupValues?.get(1)?.trim()?.takeIf { isHolderName(it) }?.let {
                add(SecretField("Держатель", it))
            }
            if (card != null) {
                BankBin.issuerFor(card)?.let { add(SecretField("Банк", it.name)) }
                Heuristics.detectCardBrand(card)?.let { add(SecretField("Платёжная система", it)) }
            }
        }
    }

    private fun isHolderName(value: String): Boolean {
        val words = value.split(Regex("\\s+")).filter { it.isNotBlank() }
        return words.size >= 2 && words.none { it.uppercase() in holderNoise }
    }

    private fun passport(text: String): List<SecretField> = buildList {
        passportRf.find(text)?.let { add(SecretField("Серия и номер", it.value)) }
        subdivision.find(text)?.let { add(SecretField("Код подразделения", it.value)) }
        dateRu.findAll(text).firstOrNull()?.let { add(SecretField("Дата выдачи", it.value)) }
    }

    private fun wifi(text: String): List<SecretField> = buildList {
        ssid.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
            ?.let { add(SecretField("Сеть (SSID)", it)) }
        wifiPassword.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
            ?.let { add(SecretField("Пароль", it)) }
    }

    private fun credentials(text: String): List<SecretField> = buildList {
        val mail = email.find(text)?.value
        val explicitLogin = loginField.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
        when {
            explicitLogin != null -> {
                add(SecretField("Логин", explicitLogin))
                mail?.let { add(SecretField("E-mail", it)) }
            }
            mail != null -> add(SecretField("Логин", mail))
        }
        passwordField.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
            ?.let { add(SecretField("Пароль", it)) }
    }

    private fun apiKey(text: String): List<SecretField> = buildList {
        url.find(text)?.let { add(SecretField("Endpoint", it.value)) }
    }
}
