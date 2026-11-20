package ru.opencode.safe.data.ai

/** Известные бренды: нужны, чтобы найти логотип в интернете и подписать секрет. */
data class Brand(val name: String, val domain: String)

object BrandCatalog {

    val yandex = Brand("Яндекс", "yandex.ru")
    val google = Brand("Google", "google.com")
    val github = Brand("GitHub", "github.com")
    val gitlab = Brand("GitLab", "gitlab.com")
    val openai = Brand("OpenAI", "openai.com")
    val anthropic = Brand("Anthropic", "anthropic.com")
    val telegram = Brand("Telegram", "telegram.org")
    val vk = Brand("VK", "vk.com")
    val slack = Brand("Slack", "slack.com")
    val discord = Brand("Discord", "discord.com")
    val aws = Brand("Amazon AWS", "aws.amazon.com")
    val stripe = Brand("Stripe", "stripe.com")
    val paypal = Brand("PayPal", "paypal.com")
    val sber = Brand("Сбербанк", "sberbank.ru")
    val tinkoff = Brand("Тинькофф", "tinkoff.ru")
    val vtb = Brand("ВТБ", "vtb.ru")
    val alfa = Brand("Альфа-Банк", "alfabank.ru")
    val gosuslugi = Brand("Госуслуги", "gosuslugi.ru")
    val ozon = Brand("Ozon", "ozon.ru")
    val wb = Brand("Wildberries", "wildberries.ru")
    val avito = Brand("Avito", "avito.ru")
    val mail = Brand("Mail.ru", "mail.ru")

    val all: List<Brand> = listOf(
        // ---- Россия ----
        yandex, google, github, gitlab, openai, anthropic, telegram, vk, slack, discord, aws, stripe, paypal,
        sber, tinkoff, vtb, alfa, gosuslugi, ozon, wb, avito, mail,
        Brand("Дзен", "dzen.ru"),
        Brand("Rutube", "rutube.ru"),
        Brand("Кинопоиск", "kinopoisk.ru"),
        Brand("Яндекс Маркет", "market.yandex.ru"),
        Brand("Газпромбанк", "gazprombank.ru"),
        Brand("Росбанк", "rosbank.ru"),
        Brand("Райффайзен", "raiffeisen.ru"),
        Brand("Совкомбанк", "sovcombank.ru"),
        Brand("Почта Банк", "pochtabank.ru"),
        Brand("МТС", "mts.ru"),
        Brand("МегаФон", "megafon.ru"),
        Brand("Билайн", "beeline.ru"),
        Brand("Т2 (Tele2)", "t2.ru"),
        Brand("Почта России", "pochta.ru"),
        Brand("Сбермаркет", "sbermarket.ru"),
        Brand("Самокат", "samokat.ru"),
        Brand("М.Видео", "mvideo.ru"),
        Brand("ДНС", "dns-shop.ru"),
        Brand("Циан", "cian.ru"),
        Brand("Хабр", "habr.com"),
        Brand("Kaspersky", "kaspersky.ru"),
        Brand("1С", "1c.ru"),
        Brand("Битрикс24", "bitrix24.ru"),
        Brand("Тинькофф Инвестиции", "tinkoff.ru"),

        // ---- Мир ----
        Brand("Gmail", "gmail.com"),
        Brand("Apple", "apple.com"),
        Brand("Microsoft", "microsoft.com"),
        Brand("Azure", "azure.microsoft.com"),
        Brand("Meta", "meta.com"),
        Brand("Instagram", "instagram.com"),
        Brand("Facebook", "facebook.com"),
        Brand("WhatsApp", "whatsapp.com"),
        Brand("Threads", "threads.net"),
        Brand("X (Twitter)", "x.com"),
        Brand("TikTok", "tiktok.com"),
        Brand("LinkedIn", "linkedin.com"),
        Brand("Reddit", "reddit.com"),
        Brand("Twitch", "twitch.tv"),
        Brand("Zoom", "zoom.us"),
        Brand("Xbox", "xbox.com"),
        Brand("PlayStation", "playstation.com"),
        Brand("Nintendo", "nintendo.com"),
        Brand("Epic Games", "epicgames.com"),
        Brand("Steam", "steampowered.com"),
        Brand("Spotify", "spotify.com"),
        Brand("Netflix", "netflix.com"),
        Brand("Dropbox", "dropbox.com"),
        Brand("Box", "box.com"),
        Brand("Notion", "notion.so"),
        Brand("Figma", "figma.com"),
        Brand("Canva", "canva.com"),
        Brand("Adobe", "adobe.com"),

        // ---- Dev / Cloud ----
        Brand("Bitbucket", "bitbucket.org"),
        Brand("Cloudflare", "cloudflare.com"),
        Brand("DigitalOcean", "digitalocean.com"),
        Brand("Heroku", "heroku.com"),
        Brand("Vercel", "vercel.com"),
        Brand("Netlify", "netlify.com"),
        Brand("Supabase", "supabase.com"),
        Brand("Firebase", "firebase.google.com"),
        Brand("MongoDB Atlas", "mongodb.com"),
        Brand("Redis", "redis.io"),
        Brand("Datadog", "datadoghq.com"),
        Brand("Sentry", "sentry.io"),
        Brand("Twilio", "twilio.com"),
        Brand("Mailgun", "mailgun.com"),
        Brand("SendGrid", "sendgrid.com"),
        Brand("Atlassian", "atlassian.com"),
        Brand("Docker Hub", "hub.docker.com"),
        Brand("Hetzner", "hetzner.com"),
        Brand("Oracle Cloud", "oracle.com"),
        Brand("Alibaba Cloud", "alibabacloud.com"),
        Brand("OpenRouter", "openrouter.ai"),
        Brand("Hugging Face", "huggingface.co"),
        Brand("npm", "npmjs.com"),
        Brand("PyPI", "pypi.org"),
        Brand("Auth0", "auth0.com"),
        Brand("Okta", "okta.com"),
        Brand("1Password", "1password.com"),
        Brand("Bitwarden", "bitwarden.com"),

        // ---- Крипта ----
        Brand("Binance", "binance.com"),
        Brand("Coinbase", "coinbase.com"),
        Brand("Kraken", "kraken.com"),
        Brand("Bybit", "bybit.com"),
        Brand("OKX", "okx.com"),
        Brand("MetaMask", "metamask.io"),
    )

    private val russianAliases: Map<String, Brand> = mapOf(
        "яндекс" to yandex, "гугл" to google, "гитхаб" to github, "телеграм" to telegram,
        "вконтакте" to vk, "сбер" to sber, "сбербанк" to sber, "тинькофф" to tinkoff, "т-банк" to tinkoff,
        "втб" to vtb, "альфа" to alfa, "альфа-банк" to alfa, "госуслуги" to gosuslugi,
        "озон" to ozon, "вайлдберриз" to wb, "авито" to avito, "майл" to mail,
        "дзен" to Brand("Дзен", "dzen.ru"), "рутуб" to Brand("Rutube", "rutube.ru"),
        "кинопоиск" to Brand("Кинопоиск", "kinopoisk.ru"),
        "газпромбанк" to Brand("Газпромбанк", "gazprombank.ru"),
        "росбанк" to Brand("Росбанк", "rosbank.ru"),
        "райффайзен" to Brand("Райффайзен", "raiffeisen.ru"),
        "совкомбанк" to Brand("Совкомбанк", "sovcombank.ru"),
        "мтс" to Brand("МТС", "mts.ru"), "мегафон" to Brand("МегаФон", "megafon.ru"),
        "билайн" to Brand("Билайн", "beeline.ru"), "теле2" to Brand("Т2 (Tele2)", "t2.ru"),
        "хабр" to Brand("Хабр", "habr.com"), "касперский" to Brand("Kaspersky", "kaspersky.ru"),
        "инстаграм" to Brand("Instagram", "instagram.com"), "фейсбук" to Brand("Facebook", "facebook.com"),
        "вотсап" to Brand("WhatsApp", "whatsapp.com"), "твиттер" to Brand("X (Twitter)", "x.com"),
        "тикток" to Brand("TikTok", "tiktok.com"), "нетфликс" to Brand("Netflix", "netflix.com"),
        "стим" to Brand("Steam", "steampowered.com"), "бинance" to Brand("Binance", "binance.com"),
        "бинанс" to Brand("Binance", "binance.com"), "коинбейс" to Brand("Coinbase", "coinbase.com"),
        "клод" to anthropic, "чатгпт" to openai, "openai" to openai,
        "амазон" to aws, "гугл клауд" to Brand("Google Cloud", "cloud.google.com"),
        "яндекс облако" to Brand("Yandex Cloud", "cloud.yandex.ru"),
        Brand("Yandex Cloud", "cloud.yandex.ru").domain to Brand("Yandex Cloud", "cloud.yandex.ru"),
    )

    /** Автоматические псевдонимы по домену и названию бренда. */
    private val autoAliases: Map<String, Brand> = buildMap {
        all.forEach { b ->
            put(b.domain, b)
            put(b.domain.substringBefore('.'), b)
            put(b.name.lowercase(), b)
        }
    }

    private fun lookup(text: String): Brand? {
        val lower = text.lowercase()
        // 1) точное совпадение домена внутри текста
        for (b in all) {
            if (lower.contains(b.domain)) return b
        }
        // 2) длинные псевдонимы (>= 4 символов, чтобы не ловить случайные совпадения)
        russianAliases.entries.firstOrNull { it.key.length >= 4 && lower.contains(it.key) }?.let { return it.value }
        russianAliases.entries.firstOrNull { lower.contains(it.key) }?.let { return it.value }
        // 3) метка домена
        for (b in all) {
            val key = b.domain.substringBefore('.')
            if (key.length >= 4 && lower.contains(key)) return b
        }
        autoAliases.entries.firstOrNull { it.key.length >= 4 && lower.contains(it.key) }?.let { return it.value }
        // 4) название бренда
        for (b in all) {
            if (b.name.length >= 4 && lower.contains(b.name.lowercase())) return b
        }
        return null
    }

    /** Ищет бренд по свободному тексту (описанию, домену, названию). */
    fun detect(text: String): Brand? {
        if (text.isBlank()) return null
        return lookup(text)
    }

    fun byDomain(domain: String): Brand? {
        if (domain.isBlank()) return null
        val clean = domain.substringAfter("://").substringBefore('/').substringBefore(':').lowercase()
        return all.firstOrNull { it.domain.equals(clean, ignoreCase = true) }
            ?: all.firstOrNull { it.domain.substringBefore('.') == clean.substringBefore('.') }
            ?: autoAliases[clean]
    }

    fun search(query: String): List<Brand> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return all.filter { it.name.lowercase().contains(q) || it.domain.contains(q) }
            .distinctBy { it.domain }
    }

    /** Варианты доменов, по которым искать логотип, если бренд не найден. */
    fun domainGuesses(query: String): List<String> {
        val q = query.trim().lowercase().substringAfter("://").substringBefore('/')
        if (q.isBlank()) return emptyList()
        if (q.contains('.')) return listOf(q)
        return listOf("$q.com", "$q.ru", "$q.io")
    }
}
