package ru.opencode.safe.data.ai

import ru.opencode.safe.data.model.CategoryType

data class TokenGuide(
    val summary: String,
    val manageUrl: String? = null,
    val manageLabel: String = "Управлять доступом",
)

/**
 * Офлайн-база знаний о форматах секретов. Используется, когда модель недоступна,
 * и как дополнение к ответу агента: что это за секрет, где он применяется и где его
 * перевыпустить.
 */
object KnowledgeBase {

    fun guideFor(brand: Brand?, type: CategoryType, value: String): TokenGuide? {
        if (brand != null) {
            guides[brand.domain]?.let { return it }
            val label = brand.domain.substringBefore('.')
            guides.entries.firstOrNull { it.key.substringBefore('.') == label }?.let { return it.value }
        }
        return when (type) {
            CategoryType.BANK_CARD -> bankFallback(value)
            CategoryType.PASSPORT -> TokenGuide(
                "Паспортные данные. Используются для бронирования, оформления документов и подтверждения личности. Храните отдельно от фотографий.",
            )
            CategoryType.WIFI -> TokenGuide(
                "Данные доступа к беспроводной сети: имя сети (SSID) и пароль. Дайте гостям отдельную сеть, чтобы не раскрывать основную.",
            )
            CategoryType.PIN -> TokenGuide(
                "Короткий числовой PIN-код. Не используйте одинаковые PIN для карт и SIM.",
            )
            CategoryType.PASSWORD -> TokenGuide(
                "Пароль. Используйте уникальный пароль для каждого сервиса и менеджер паролей.",
            )
            CategoryType.NOTE -> null
            else -> null
        }
    }

    private fun bankFallback(value: String): TokenGuide {
        val issuer = BankBin.issuerFor(value)
        if (issuer != null) return bankGuide(issuer)
        return TokenGuide(
            "Номер платёжной карты. Никому не сообщайте полный номер, срок действия и CVC — этих данных достаточно для оплаты. " +
                "Банк-эмитент можно определить по первым цифрам номера (BIN).",
        )
    }

    private fun bankGuide(issuer: BankBin.Issuer) = TokenGuide(
        "Карта ${issuer.name}. Эмитент предположительно определён по BIN. Полный номер, срок действия и CVC никому не сообщайте. " +
            "Управление картами и лимитами: ${issuer.cardUrl}",
        issuer.manageUrl,
        "Интернет-банк ${issuer.name}",
    )

    fun genericGuide(type: CategoryType): String = when (type) {
        CategoryType.TOKEN -> "Токен доступа подтверждает вашу личность без пароля. Если токен утёк — немедленно отзовите его в настройках сервиса."
        CategoryType.API_KEY -> "API-ключ даёт программный доступ к сервису и часто тарифицируется. Храните его в секретах и не публикуйте в репозиториях."
        CategoryType.CERTIFICATE -> "Цифровой сертификат используется для подписи и шифрования. Следите за сроком действия."
        CategoryType.LICENSE -> "Лицензионный или приватный ключ. Не передавайте третьим лицам."
        CategoryType.ACCOUNT -> "Данные входа в аккаунт. Включите двухфакторную аутентификацию."
        else -> "Секретные данные. Храните в сейфе и не пересылайте в открытых каналах."
    }

    private val guides: Map<String, TokenGuide> = buildMap {
        // ---------- Банки ----------
        put(
            "sberbank.ru",
            TokenGuide(
                "Карта Сбербанка или токен Сбер ID. Полные реквизиты и CVC не сообщайте. Управление картами: https://www.sberbank.ru/ru/person/bank_cards",
                "https://online.sberbank.ru/", "Сбербанк Онлайн",
            ),
        )
        put(
            "tinkoff.ru",
            TokenGuide(
                "Данные Т-Банка (Тинькофф). Управление картами и лимитами: https://www.tbank.ru/cards/",
                "https://www.tinkoff.ru/mybank/", "Т-Банк: личный кабинет",
            ),
        )
        put(
            "alfabank.ru",
            TokenGuide(
                "Данные Альфа-Банка. Выпуск и управление картами: https://alfabank.ru/get-money/credit-cards/",
                "https://ib.alfabank.ru/", "Альфа-Онлайн",
            ),
        )
        put(
            "vtb.ru",
            TokenGuide(
                "Данные ВТБ. Карты и лимиты: https://www.vtb.ru/personal/karty/",
                "https://www.vtb.ru/lk/", "ВТБ Онлайн",
            ),
        )
        put(
            "gazprombank.ru",
            TokenGuide(
                "Данные Газпромбанка. Карты: https://www.gazprombank.ru/personal/cards/",
                "https://web.gazprombank.ru/", "Газпромбанк: интернет-банк",
            ),
        )

        // ---------- ИИ и разработка ----------
        put(
            "accounts.google.com",
            TokenGuide(
                "OAuth-токен Google (формат ya29.…). Выдаётся приложениям для доступа к Gmail, Диску и Календарю. Постоянный доступ даёт refresh_token (1//0…).",
                "https://myaccount.google.com/permissions", "Отозвать доступ",
            ),
        )
        put(
            "cloud.google.com",
            TokenGuide(
                "Ключ Google Cloud (AIza…, GOCSPX-…). Используется для Maps, Gemini, Translate и других API. Обязательно ограничьте по IP и API.",
                "https://console.cloud.google.com/apis/credentials", "Открыть API-ключи",
            ),
        )
        put(
            "github.com",
            TokenGuide(
                "Токен GitHub (ghp_/gho_/ghu_/ghs_/ghr_/github_pat_…). Даёт доступ к репозиториям по HTTPS. Выдавайте минимальные права и срок жизни.",
                "https://github.com/settings/tokens", "Создать новый токен",
            ),
        )
        put(
            "gitlab.com",
            TokenGuide(
                "Personal Access Token GitLab (glpat-…, glrt-…, gldt-…). Доступ к репозиториям и API.",
                "https://gitlab.com/-/profile/personal_access_tokens", "Создать новый токен",
            ),
        )
        put(
            "openai.com",
            TokenGuide(
                "API-ключ OpenAI (sk-…, sk-proj-…). Оплачивается по факту использования. Не публикуйте в клиентских приложениях.",
                "https://platform.openai.com/api-keys", "Открыть ключи",
            ),
        )
        put(
            "anthropic.com",
            TokenGuide(
                "API-ключ Anthropic (sk-ant-…). Используется для моделей Claude.",
                "https://console.anthropic.com/settings/keys", "Открыть ключи",
            ),
        )
        put(
            "openrouter.ai",
            TokenGuide(
                "API-ключ OpenRouter (sk-or-…). Доступ к множеству моделей через единый шлюз.",
                "https://openrouter.ai/keys", "Открыть ключи",
            ),
        )
        put(
            "aws.amazon.com",
            TokenGuide(
                "Ключ доступа AWS (AKIA…, ASIA…, A3T…). Пара Access Key ID + Secret. При утечке деактивируйте ключ в IAM.",
                "https://console.aws.amazon.com/iam/home#/security_credentials", "Открыть IAM",
            ),
        )
        put(
            "alibabacloud.com",
            TokenGuide(
                "Ключ Alibaba Cloud (LTAI…). Даёт доступ к API облака и биллингу.",
                "https://ram.console.aliyun.com/manage/ak", "Открыть AccessKey",
            ),
        )
        put(
            "stripe.com",
            TokenGuide(
                "Ключ Stripe (sk_live_/pk_live_/rk_live_…). Секретный ключ sk_ даёт полный доступ к платежам.",
                "https://dashboard.stripe.com/apikeys", "Открыть ключи",
            ),
        )
        put(
            "sendgrid.com",
            TokenGuide(
                "API-ключ SendGrid (SG.…). Используется для отправки писем.",
                "https://app.sendgrid.com/settings/api_keys", "Открыть ключи",
            ),
        )
        put(
            "mailgun.com",
            TokenGuide(
                "API-ключ Mailgun (key-…). Используется для отправки и аналитики писем.",
                "https://app.mailgun.com/settings/api_security", "Открыть ключи",
            ),
        )
        put(
            "npmjs.com",
            TokenGuide(
                "Токен npm (npm_…). Позволяет публиковать пакеты от вашего имени.",
                "https://www.npmjs.com/settings/~/tokens", "Открыть токены",
            ),
        )
        put(
            "pypi.org",
            TokenGuide(
                "Токен PyPI (pypi-…). Используется для публикации пакетов Python.",
                "https://pypi.org/manage/account/token/", "Открыть токены",
            ),
        )
        put(
            "digitalocean.com",
            TokenGuide(
                "Токен DigitalOcean (dop_v1_…). Управляет дроплетами и биллингом.",
                "https://cloud.digitalocean.com/account/api/tokens", "Открыть токены",
            ),
        )
        put(
            "cloudflare.com",
            TokenGuide(
                "API-токен Cloudflare. Даёт доступ к DNS и настройкам домена.",
                "https://dash.cloudflare.com/profile/api-tokens", "Открыть токены",
            ),
        )
        put(
            "atlassian.com",
            TokenGuide(
                "API-токен Atlassian (ATATT…). Используется для Jira и Confluence вместо пароля.",
                "https://id.atlassian.com/manage-profile/security/api-tokens", "Открыть токены",
            ),
        )
        put(
            "huggingface.co",
            TokenGuide(
                "Токен Hugging Face (hf_…). Даёт доступ к моделям и датасетам, при write-правах — к публикации.",
                "https://huggingface.co/settings/tokens", "Открыть токены",
            ),
        )
        put(
            "datadoghq.com",
            TokenGuide(
                "Ключ Datadog. Разделяйте API- и APP-ключи; API-ключ отправляет метрики.",
                "https://app.datadoghq.com/organization-settings/api-keys", "Открыть ключи",
            ),
        )
        put(
            "sentry.io",
            TokenGuide(
                "Токен Sentry. DSN-ключ только пишет события, auth-токен управляет проектом.",
                "https://sentry.io/settings/auth-tokens/", "Открыть токены",
            ),
        )

        // ---------- Мессенджеры и соцсети ----------
        put(
            "telegram.org",
            TokenGuide(
                "Токен Telegram-бота (123456:AA…). Полностью управляет ботом. Перевыпускается в @BotFather командой /revoke.",
                "https://t.me/BotFather", "Открыть BotFather",
            ),
        )
        put(
            "slack.com",
            TokenGuide(
                "Токен Slack (xoxb-/xoxp-/xapp-…). Даёт боту доступ к рабочему пространству.",
                "https://api.slack.com/apps", "Открыть приложения",
            ),
        )
        put(
            "vk.com",
            TokenGuide(
                "Токен VK (vk1.a.…). Используется приложениями и ботами ВКонтакте.",
                "https://vk.com/apps?act=manage", "Открыть приложения",
            ),
        )
        put(
            "meta.com",
            TokenGuide(
                "Токен Meta (EAA…, IGQ…). Доступ к Graph API Facebook и Instagram.",
                "https://developers.facebook.com/apps/", "Открыть приложения",
            ),
        )
        put(
            "discord.com",
            TokenGuide(
                "Токен Discord-бота. Даёт полный контроль над ботом — при утечке перевыпустите его в Developer Portal.",
                "https://discord.com/developers/applications", "Открыть приложения",
            ),
        )
        put("instagram.com", TokenGuide("Пароль Instagram. Включите двухфакторную аутентификацию и проверяйте активные сессии.", "https://www.instagram.com/accounts/privacy_and_security/", "Безопасность"))
        put("facebook.com", TokenGuide("Пароль Facebook. Проверьте активные сессии и двухфакторную аутентификацию.", "https://www.facebook.com/settings?tab=security", "Безопасность"))
        put("x.com", TokenGuide("Пароль X (Twitter) или API-токен. Проверьте подключённые приложения.", "https://x.com/settings/connected_apps", "Подключённые приложения"))
        put("tiktok.com", TokenGuide("Пароль TikTok. Управляйте активными устройствами в настройках безопасности.", "https://www.tiktok.com/setting/security", "Безопасность"))
        put("linkedin.com", TokenGuide("Пароль LinkedIn. Проверьте приложения с доступом к аккаунту.", "https://www.linkedin.com/psettings/permitted-services", "Доступ приложений"))
        put("reddit.com", TokenGuide("Пароль Reddit или токен приложения. Отзовите доступ у ненужных приложений.", "https://www.reddit.com/prefs/apps", "Открыть приложения"))
        put("twitch.tv", TokenGuide("Токен Twitch (или пароль). Проверьте подключения и OAuth-приложения.", "https://www.twitch.tv/settings/connections", "Подключения"))

        // ---------- Россия, сервисы ----------
        put(
            "yandex.ru",
            TokenGuide(
                "OAuth-токен Яндекс. Даёт доступ к Почте, Диску и API. Управляйте выданными доступами в аккаунте.",
                "https://oauth.yandex.ru/", "Управлять доступами",
            ),
        )
        put("cloud.yandex.ru", TokenGuide("IAM-ключ или OAuth-токен Yandex Cloud. Даёт доступ к облачным ресурсам.", "https://console.yandex.cloud/", "Открыть консоль"))
        put("mail.ru", TokenGuide("Пароль Mail.ru. Включите двухфакторную аутентификацию и проверьте внешние приложения.", "https://account.mail.ru/user/security", "Безопасность"))
        put("gosuslugi.ru", TokenGuide("Пароль Госуслуг. Даёт доступ к юридически значимым действиям — включите двухфакторную аутентификацию.", "https://esia.gosuslugi.ru/profile/user/security", "Безопасность ЕСИА"))
        put("ozon.ru", TokenGuide("Пароль Ozon. Проверьте платёжные методы и активные сессии.", "https://www.ozon.ru/my/security", "Безопасность"))
        put("wildberries.ru", TokenGuide("Пароль Wildberries. Проверьте привязанные карты и устройства.", "https://www.wildberries.ru/lk/details", "Личный кабинет"))
        put("avito.ru", TokenGuide("Пароль Avito. Включите двухфакторную аутентификацию — аккаунт часто используют для мошенничества.", "https://www.avito.ru/profile/security", "Безопасность"))
        put("habr.com", TokenGuide("Пароль Хабр. Проверьте активные сессии в настройках.", "https://habr.com/ru/settings/security/", "Безопасность"))
        put("mts.ru", TokenGuide("Пароль МТС. Даёт доступ к балансу, тарифу и услугам — включите двухфакторную аутентификацию.", "https://login.mts.ru/", "Личный кабинет МТС"))
        put("megafon.ru", TokenGuide("Пароль МегаФон. Проверьте подключённые услуги и устройства.", "https://lk.megafon.ru/", "Личный кабинет МегаФон"))
        put("beeline.ru", TokenGuide("Пароль Билайн. Проверьте услуги и активные сессии.", "https://my.beeline.ru/", "Личный кабинет Билайн"))
        put("t2.ru", TokenGuide("Пароль Т2 (Tele2). Проверьте услуги и устройства в личном кабинете.", "https://t2.ru/", "Личный кабинет Т2"))

        // ---------- Хранилище и продуктивность ----------
        put("dropbox.com", TokenGuide("Токен или пароль Dropbox. Даёт доступ к файлам — при утечке отзовите приложение.", "https://www.dropbox.com/account/connected_apps", "Подключённые приложения"))
        put("notion.so", TokenGuide("Токен Notion (secret_…, ntn_…). Даёт доступ к рабочим пространствам через API.", "https://www.notion.so/my-integrations", "Открыть интеграции"))
        put("figma.com", TokenGuide("Токен Figma (figd_…). Даёт доступ к файлам и командам.", "https://www.figma.com/settings", "Открыть настройки"))
        put("spotify.com", TokenGuide("Пароль Spotify. Проверьте приложения с доступом к аккаунту.", "https://www.spotify.com/account/apps/", "Доступ приложений"))
        put("netflix.com", TokenGuide("Пароль Netflix. Проверьте устройства и смените пароль, если он был в открытом виде.", "https://www.netflix.com/account", "Аккаунт"))
        put("steampowered.com", TokenGuide("Пароль Steam или API-ключ. Включите Steam Guard и не вводите код на сторонних сайтах.", "https://store.steampowered.com/account/", "Аккаунт Steam"))
        put("epicgames.com", TokenGuide("Пароль Epic Games. Включите двухфакторную аутентификацию.", "https://www.epicgames.com/account/password", "Аккаунт Epic"))

        // ---------- Крипта ----------
        put("binance.com", TokenGuide("API-ключ Binance. Разрешайте только чтение, отключите вывод средств для ключа.", "https://www.binance.com/en/my/settings/api-management", "API-ключи Binance"))
        put("coinbase.com", TokenGuide("API-ключ или пароль Coinbase. Ограничьте права ключа только просмотром.", "https://www.coinbase.com/settings/api", "API Coinbase"))
        put("bybit.com", TokenGuide("API-ключ Bybit. Никогда не включайте вывод средств для API-ключа.", "https://www.bybit.com/app/user/api-management", "API Bybit"))
        put("metamask.io", TokenGuide("Сид-фраза или приватный ключ MetaMask. Никогда не вводите её нигде, кроме самого кошелька — это полный доступ к средствам.", null, "Управлять доступом"))
        put("kraken.com", TokenGuide("API-ключ Kraken. Ограничьте права и включите двухфакторную аутентификацию.", "https://pro.kraken.com/app/settings/api", "API Kraken"))
        put("okx.com", TokenGuide("API-ключ OKX. Отключите вывод средств и привяжите IP.", "https://www.okx.com/account/my-api", "API OKX"))

        // ---------- Прочее ----------
        put("shopify.com", TokenGuide("Токен Shopify (shpat_…). Даёт доступ к магазину через Admin API.", "https://admin.shopify.com/", "Открыть админку"))
        put("zoom.us", TokenGuide("Токен Zoom или пароль. Проверьте подключённые приложения.", "https://marketplace.zoom.us/user/build", "Приложения Zoom"))
    }
}
