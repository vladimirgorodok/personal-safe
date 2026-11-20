package ru.opencode.safe.data.ai

import org.json.JSONObject
import ru.opencode.safe.data.model.CategoryType
import ru.opencode.safe.data.model.Defaults
import java.util.Base64

/**
 * Локальная эвристика: определяет тип секрета по формату без обращения к ИИ.
 * Работает офлайн и служит запасным вариантом для агента, а также подсказкой при
 * анализе изображений, когда модель недоступна.
 */
object Heuristics {

    data class Guess(
        val type: CategoryType,
        val categoryName: String,
        val title: String,
        val description: String,
        val brand: Brand?,
        val confidence: Float,
        val tags: List<String> = emptyList(),
    )

    private val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val urlRegex = Regex("""https?://[^\s"'<>)\]]+""")
    private val passportRfRegex = Regex("""\b\d{2}\s?\d{2}\s?\d{6}\b""")
    private val telegramTokenRegex = Regex("""^\d{6,12}:[A-Za-z0-9_-]{30,}$""")

    fun classify(rawInput: String): Guess {
        val raw = rawInput.trim().trim('"', '\'', '`')
        val value = raw.removePrefix("Bearer ").trim()
        val lower = value.lowercase()

        // PIN-код
        val digits = digitsOnly(value)
        if (value.length in 4..8 && digits.length == value.length && digits.isNotEmpty()) {
            return Guess(CategoryType.PIN, Defaults.systemCategoryName(CategoryType.PIN), "PIN-код", "Короткий числовой код", null, 0.55f)
        }

        // Банковская карта
        if (looksLikeCard(value) && digits.length in 13..19 && luhn(digits)) {
            val network = detectCardBrand(digits)
            val issuer = BankBin.issuerFor(digits)
            val brand = issuer?.let { Brand(it.name, it.domain) } ?: network?.let { Brand(it, cardDomain(it)) }
            val title = issuer?.let { "Карта ${it.name}" }
                ?: network?.let { "Карта $it" }
                ?: "Банковская карта"
            val description = buildList {
                network?.let { add("Платёжная система: $it") }
                issuer?.let { add("Эмитент (предположительно): ${it.name}") }
            }.joinToString(" · ").ifBlank { "Платёжная карта" }
            return Guess(
                CategoryType.BANK_CARD, Defaults.systemCategoryName(CategoryType.BANK_CARD),
                title,
                description,
                brand, 0.9f, listOfNotNull(network, issuer?.name),
            )
        }

        // Паспорт РФ (серия + номер)
        if (passportRfRegex.containsMatchIn(value) || lower.contains("паспорт") || lower.contains("серия")) {
            if (passportRfRegex.containsMatchIn(value) || lower.contains("паспорт")) {
                return Guess(CategoryType.PASSPORT, Defaults.systemCategoryName(CategoryType.PASSPORT), "Паспорт РФ", "Паспортные данные", null, 0.7f)
            }
        }

        // MRZ / удостоверение
        if (value.contains("<<<") || Regex("""P<[A-Z]{3}""").containsMatchIn(value)) {
            return Guess(CategoryType.ID_CARD, Defaults.systemCategoryName(CategoryType.ID_CARD), "Удостоверение (MRZ)", "Машиночитаемая зона документа", null, 0.75f)
        }

        // Wi-Fi
        if (lower.contains("ssid") || lower.contains("wpa") || (lower.contains("wi-fi") && lower.contains("парол"))) {
            val brand = BrandCatalog.detect(value)
            return Guess(CategoryType.WIFI, Defaults.systemCategoryName(CategoryType.WIFI), "Wi-Fi сеть", "Данные доступа к Wi-Fi", brand, 0.8f)
        }

        // SSH / сертификаты
        if (value.startsWith("ssh-rsa") || value.startsWith("ssh-ed25519") || value.contains("BEGIN OPENSSH") || value.contains("-----BEGIN")) {
            val isCert = value.contains("CERTIFICATE")
            return Guess(
                if (isCert) CategoryType.CERTIFICATE else CategoryType.LICENSE,
                Defaults.systemCategoryName(if (isCert) CategoryType.CERTIFICATE else CategoryType.LICENSE),
                if (isCert) "Сертификат" else "Ключ SSH",
                if (isCert) "Цифровой сертификат" else "Приватный ключ SSH",
                null, 0.7f,
            )
        }

        // JWT
        if (isJwt(value)) {
            val brand = brandFromJwt(value)
            return Guess(
                CategoryType.TOKEN, Defaults.systemCategoryName(CategoryType.TOKEN),
                brand?.let { "Токен ${it.name}" } ?: "JWT токен",
                "JSON Web Token", brand, 0.85f, listOf("JWT"),
            )
        }

        // Токены/ключи по префиксу
        val (tokenBrand, isApiKey) = brandFromPrefix(value)
        if (tokenBrand != null || isApiKey) {
            val type = if (isApiKey) CategoryType.API_KEY else CategoryType.TOKEN
            return Guess(
                type, Defaults.systemCategoryName(type),
                tokenBrand?.let { "${if (isApiKey) "API-ключ" else "Токен"} ${it.name}" } ?: if (isApiKey) "API-ключ" else "Токен доступа",
                if (isApiKey) "Ключ доступа к API" else "Токен авторизации",
                tokenBrand, 0.8f,
            )
        }

        // Аккаунт: логин/пароль
        val email = emailRegex.find(value)?.value
        if (email != null) {
            val domain = email.substringAfter('@')
            val brand = BrandCatalog.byDomain(domain) ?: BrandCatalog.detect(domain)
            val looksLikeCreds = lower.contains("парол") || lower.contains("pass") || value.contains(":")
            val type = if (looksLikeCreds) CategoryType.ACCOUNT else CategoryType.ACCOUNT
            return Guess(
                type, Defaults.systemCategoryName(type),
                brand?.let { "Аккаунт ${it.name}" } ?: "Аккаунт",
                "Данные входа: $email", brand, 0.6f, listOf(email),
            )
        }

        // Ссылка
        val url = urlRegex.find(value)?.value
        if (url != null) {
            val domain = runCatching { java.net.URI(url).host ?: "" }.getOrDefault("")
            val brand = BrandCatalog.byDomain(domain) ?: BrandCatalog.detect(domain)
            return Guess(CategoryType.NOTE, Defaults.systemCategoryName(CategoryType.NOTE), brand?.let { "Ссылка ${it.name}" } ?: "Ссылка", url, brand, 0.5f)
        }

        // Пароль-однострочник
        if (value.length in 6..64 && !value.contains(' ') && hasMixedClasses(value)) {
            return Guess(CategoryType.PASSWORD, Defaults.systemCategoryName(CategoryType.PASSWORD), "Пароль", "Сохранённый пароль", null, 0.5f)
        }

        return Guess(CategoryType.NOTE, Defaults.systemCategoryName(CategoryType.NOTE), "Заметка", value.take(80), null, 0.3f)
    }

    fun digitsOnly(s: String): String = s.filter { it.isDigit() }

    private fun looksLikeCard(s: String): Boolean = Regex("""^[\d\s\-]{13,25}$""").matches(s.trim())

    fun luhn(number: String): Boolean {
        if (number.length < 12) return false
        var sum = 0
        var alt = false
        for (i in number.indices.reversed()) {
            var n = number[i] - '0'
            if (n < 0 || n > 9) return false
            if (alt) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alt = !alt
        }
        return sum % 10 == 0
    }

    fun detectCardBrand(digits: String): String? {
        val n2 = digits.take(2).toIntOrNull() ?: return null
        val n4 = digits.take(4).toIntOrNull() ?: return null
        return when {
            digits.startsWith("4") -> "Visa"
            n2 in 51..55 -> "Mastercard"
            n4 in 2221..2720 -> "Mastercard"
            n4 in 2200..2204 -> "МИР"
            digits.startsWith("34") || digits.startsWith("37") -> "American Express"
            digits.startsWith("35") -> "JCB"
            digits.startsWith("62") -> "UnionPay"
            digits.startsWith("6011") || digits.startsWith("65") || digits.take(3).toIntOrNull() in 644..649 -> "Discover"
            digits.startsWith("2") -> "МИР"
            else -> null
        }
    }

    private fun cardDomain(brand: String): String = when (brand) {
        "Visa" -> "visa.com"
        "Mastercard" -> "mastercard.com"
        "МИР" -> "mironline.ru"
        "American Express" -> "americanexpress.com"
        "JCB" -> "jcb.co.jp"
        "UnionPay" -> "unionpayintl.com"
        "Discover" -> "discover.com"
        else -> "bank"
    }

    fun isJwt(value: String): Boolean {
        val parts = value.split('.')
        return parts.size == 3 && parts[0].startsWith("eyJ") && parts.all { it.length >= 2 }
    }

    fun jwtPayload(token: String): JSONObject? = runCatching {
        val payload = token.split('.')[1]
        JSONObject(String(decodeBase64Url(payload)))
    }.getOrNull()

    private fun brandFromJwt(token: String): Brand? {
        val payload = jwtPayload(token) ?: return null
        val issuer = listOf("iss", "aud", "azp", "client_id", "sub")
            .joinToString(" ") { payload.optString(it) }
            .lowercase()
        return when {
            issuer.contains("google") || issuer.contains("accounts.google") -> Brand("Google", "accounts.google.com")
            issuer.contains("yandex") -> BrandCatalog.yandex
            issuer.contains("github") -> BrandCatalog.github
            issuer.contains("gitlab") -> BrandCatalog.gitlab
            issuer.contains("auth0") -> Brand("Auth0", "auth0.com")
            issuer.contains("okta") -> Brand("Okta", "okta.com")
            issuer.contains("slack") -> BrandCatalog.slack
            else -> BrandCatalog.detect(issuer)
        }
    }

    /** Возвращает бренд и признак API-ключа по префиксу токена. */
    fun brandFromPrefix(value: String): Pair<Brand?, Boolean> = when {
        value.startsWith("ya29.") -> Brand("Google", "accounts.google.com") to false
        value.startsWith("1//0") -> Brand("Google", "accounts.google.com") to false
        value.startsWith("GOCSPX-") -> Brand("Google Cloud", "cloud.google.com") to true
        value.startsWith("AIza") -> Brand("Google Cloud", "cloud.google.com") to true
        value.startsWith("ghp_") || value.startsWith("gho_") || value.startsWith("ghu_") ||
            value.startsWith("ghs_") || value.startsWith("ghr_") || value.startsWith("github_pat_") ->
            BrandCatalog.github to false
        value.startsWith("glpat-") || value.startsWith("glrt-") || value.startsWith("gldt-") || value.startsWith("glsoat-") ->
            BrandCatalog.gitlab to false
        value.startsWith("sk-ant-") -> BrandCatalog.anthropic to true
        value.startsWith("sk-or-") -> Brand("OpenRouter", "openrouter.ai") to true
        value.startsWith("sk-proj-") -> BrandCatalog.openai to true
        value.startsWith("sk-") -> BrandCatalog.openai to true
        value.startsWith("xoxb-") || value.startsWith("xoxp-") || value.startsWith("xoxa-") ||
            value.startsWith("xoxc-") || value.startsWith("xoxd-") || value.startsWith("xapp-") ->
            BrandCatalog.slack to false
        value.startsWith("AKIA") || value.startsWith("ASIA") || value.startsWith("A3T") || value.startsWith("ABIA") ->
            BrandCatalog.aws to true
        value.startsWith("LTAI") -> Brand("Alibaba Cloud", "alibabacloud.com") to true
        value.startsWith("SG.") -> Brand("SendGrid", "sendgrid.com") to true
        value.startsWith("shpat_") || value.startsWith("shpca_") || value.startsWith("shppa_") || value.startsWith("shpss_") ->
            Brand("Shopify", "shopify.com") to true
        value.startsWith("sq0atp-") || value.startsWith("sq0csp-") -> Brand("Square", "squareup.com") to true
        value.startsWith("dop_v1_") -> Brand("DigitalOcean", "digitalocean.com") to true
        value.startsWith("key-") -> Brand("Mailgun", "mailgun.com") to true
        value.startsWith("pypi-") -> Brand("PyPI", "pypi.org") to true
        value.startsWith("sk_live_") || value.startsWith("sk_test_") ||
            value.startsWith("pk_live_") || value.startsWith("pk_test_") ||
            value.startsWith("rk_live_") || value.startsWith("rk_test_") -> BrandCatalog.stripe to true
        value.startsWith("npm_") -> Brand("npm", "npmjs.com") to true
        value.startsWith("EAA") || value.startsWith("IGQ") || value.startsWith("EAACEdEose0cBA") ->
            Brand("Meta", "meta.com") to false
        value.startsWith("vk1.a.") -> BrandCatalog.vk to false
        value.startsWith("hf_") -> Brand("Hugging Face", "huggingface.co") to true
        value.startsWith("ddapi") || value.startsWith("ddsdk") -> Brand("Datadog", "datadoghq.com") to true
        telegramTokenRegex.matches(value) -> BrandCatalog.telegram to false
        else -> null to false
    }

    fun isUrl(value: String): Boolean = urlRegex.matches(value.trim())

    fun isImageUrl(url: String): Boolean {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
            path.endsWith(".webp") || path.endsWith(".gif") || path.endsWith(".bmp") ||
            path.endsWith(".heic") || path.endsWith(".svg") || path.contains("/image") ||
            path.contains("favicon")
    }

    fun extractLinks(text: String): List<String> = urlRegex.findAll(text).map { it.value }.toList()

    private fun hasMixedClasses(s: String): Boolean {
        val hasDigit = s.any { it.isDigit() }
        val hasLetter = s.any { it.isLetter() }
        val hasSymbol = s.any { !it.isLetterOrDigit() }
        return (hasDigit && hasLetter) || (hasLetter && hasSymbol)
    }

    private fun decodeBase64Url(input: String): ByteArray {
        val normalized = input.replace('-', '+').replace('_', '/')
        val padded = when (normalized.length % 4) {
            2 -> "$normalized=="
            3 -> "$normalized="
            else -> normalized
        }
        return Base64.getDecoder().decode(padded)
    }
}
