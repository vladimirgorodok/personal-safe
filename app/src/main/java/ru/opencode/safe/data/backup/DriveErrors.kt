package ru.opencode.safe.data.backup

import org.json.JSONObject

/**
 * Превращает ответ Google об ошибке в понятную подсказку. Google в теле ответа пишет
 * точную причину (`reason`), а мы раньше её теряли и показывали общий текст.
 */
object DriveErrors {

    const val DRIVE_API_URL = "https://console.cloud.google.com/apis/library/drive.googleapis.com"
    const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"

    fun describe(code: Int, body: String, what: String): String {
        val error = runCatching { JSONObject(body).optJSONObject("error") }.getOrNull()
        val message = error?.optString("message").orEmpty()
        val reason = error?.optJSONArray("errors")?.optJSONObject(0)?.optString("reason").orEmpty()
        val status = error?.optString("status").orEmpty()
        val lower = (message + " " + reason + " " + status).lowercase()

        val hint = when {
            reason == "accessNotConfigured" || reason == "SERVICE_DISABLED" ||
                lower.contains("has not been used") || lower.contains("is disabled") ||
                lower.contains("service_disabled") ->
                "В проекте Google Cloud не включён Google Drive API. Включите его здесь: " +
                    "$DRIVE_API_URL — это отдельная настройка, разрешения аккаунта и экран согласия её не заменяют."

            reason == "insufficientPermissions" || reason == "insufficientScopes" ||
                reason == "forbidden" || lower.contains("insufficient") ->
                "Google не выдал доступ к Drive. Проверьте, что на экране согласия OAuth " +
                    "(Data access → Scopes) добавлена область $SCOPE и вход выполнен тем же аккаунтом."

            reason == "rateLimitExceeded" || reason == "userRateLimitExceeded" ->
                "Google временно ограничил число запросов. Подождите минуту и повторите."

            reason == "domainPolicy" ->
                "Политика вашего домена запрещает доступ к Drive для этого приложения."

            reason == "invalid" || code == 400 ->
                "Google не принял запрос приложения: неверный параметр запроса. Это ошибка в приложении, " +
                    "а не в ваших настройках доступа — до Drive мы уже достучались."

            code == 401 -> "Доступ к Google Drive больше не действует — войдите через Google заново."
            code == 404 -> "Копия не найдена в Google Drive."

            else -> "$what (код $code)"
        }
        return if (message.isBlank()) hint else "$hint\n\nОтвет Google: $message"
    }

    /** Нужна ли кнопка «включить Drive API» — по тексту ошибки. */
    fun needsApiEnable(text: String?): Boolean = text?.contains("Drive API") == true
}
