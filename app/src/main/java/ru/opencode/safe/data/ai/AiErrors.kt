package ru.opencode.safe.data.ai

import okhttp3.Headers
import kotlin.math.roundToInt

/**
 * Понятные сообщения об ошибках ИИ-провайдера.
 *
 * Главный случай — 429: у бесплатной модели кончился лимит. Вместо технического «(429)
 * rate limit exceeded» показываем, что случилось и когда снова заработает. Если провайдер
 * прислал время ожидания, считаем по нему; иначе подставляем известное время суточного сброса.
 */
object AiErrors {

    fun tooManyRequests(headers: Headers, detail: String): String {
        val retryAfterSeconds = headers["Retry-After"]?.trim()?.toLongOrNull()
        val resetRequests = headers["x-ratelimit-reset-requests"]
        val hint = when {
            retryAfterSeconds != null && retryAfterSeconds > 0 ->
                "Подождите примерно ${humanWait(retryAfterSeconds)} и повторите — лимит обновится."

            resetRequests != null -> {
                val seconds = parseDuration(resetRequests)
                if (seconds != null && seconds in 1..3600) {
                    "Слишком много запросов подряд. Подождите примерно ${humanWait(seconds)} и повторите — " +
                        "дневной лимит ещё не исчерпан."
                } else {
                    "Дневной лимит исчерпан — ${AiDefaults.RENEW_HINT}."
                }
            }

            else -> "Дневной лимит бесплатных запросов исчерпан. Он ${AiDefaults.RENEW_HINT}, " +
                "после чего агент снова заработает."
        }
        val suffix = detail.takeIf { it.isNotBlank() }?.let { "\n\nТехнически: $it" } ?: ""
        return "Лимит бесплатной модели исчерпан. $hint$suffix"
    }

    /** Подсказка для частых ошибок, кроме лимита: чаще всего это неизвестная модель. */
    fun hintFor(code: Int, detail: String): String? {
        val lower = detail.lowercase()
        val modelProblem = lower.contains("model") &&
            (lower.contains("not found") || lower.contains("not exist") || lower.contains("does not exist") ||
                lower.contains("decommission") || lower.contains("unknown") || lower.contains("unsupported"))
        return when {
            code == 404 || (code == 400 && modelProblem) ->
                "Провайдер не знает выбранную модель. Откройте «Провайдер и модель», нажмите стрелку в поле " +
                    "«Модель», посмотрите список доступных моделей и выберите другую."
            else -> null
        }
    }

    /** «2m59.56s» и похожие форматы → секунды. */
    fun parseDuration(value: String): Long? {
        val match = Regex("""(\d+(?:\.\d+)?)(ms|s|m|h)""").findAll(value).toList()
        if (match.isEmpty()) return value.trim().toLongOrNull()
        var seconds = 0.0
        match.forEach { m ->
            val amount = m.groupValues[1].toDoubleOrNull() ?: return@forEach
            seconds += when (m.groupValues[2]) {
                "ms" -> amount / 1000.0
                "s" -> amount
                "m" -> amount * 60
                "h" -> amount * 3600
                else -> 0.0
            }
        }
        return kotlin.math.round(seconds).toLong().takeIf { it > 0 }
    }

    private fun humanWait(seconds: Long): String = when {
        seconds < 60 -> "$seconds сек."
        seconds < 3600 -> "${(seconds / 60.0).roundToInt()} мин."
        else -> "${(seconds / 3600.0).roundToInt()} ч."
    }
}
