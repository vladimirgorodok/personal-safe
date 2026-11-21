package ru.opencode.safe.ui

import android.content.Context
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/** Состояние всего, от чего зависит вход через Google на этом устройстве. */
data class GoogleSupport(
    val playServicesStatus: Int,
    val playServicesMessage: String,
    val playServicesVersion: String?,
) {
    val available: Boolean get() = playServicesStatus == ConnectionResult.SUCCESS
}

fun checkGoogleSupport(context: Context): GoogleSupport {
    val availability = GoogleApiAvailability.getInstance()
    val status = runCatching { availability.isGooglePlayServicesAvailable(context) }
        .getOrDefault(ConnectionResult.SERVICE_MISSING)
    val message = runCatching { availability.getErrorString(status) }
        .getOrDefault("неизвестная ошибка")
    val version = runCatching {
        context.packageManager.getPackageInfo("com.google.android.gms", 0).versionName
    }.getOrNull()
    return GoogleSupport(status, message, version)
}

/**
 * Человеческая подсказка к ошибке Credential Manager: чаще всего дело в конфигурации
 * Google Cloud, а не в самом приложении.
 */
fun googleErrorHint(error: Throwable): String {
    val type = (error as? GetCredentialException)?.type.orEmpty()
    val haystack = (error::class.java.name + " " + type + " " + (error.message ?: "")).lowercase()
    return when {
        haystack.contains("cancel") ->
            "Вход отменён — окно выбора аккаунта закрыто."

        haystack.contains("no credential") || haystack.contains("nocredential") ->
            "Google не предложил ни одного аккаунта. Проверьте по порядку: " +
                "(1) на устройстве добавлен Google-аккаунт; " +
                "(2) установлены Google Play Services (на китайских прошивках MIUI/HyperOS их обычно нет); " +
                "(3) на экране согласия OAuth ваш аккаунт добавлен в «Test users», пока приложение в статусе «Testing»."

        haystack.contains("provider_configuration") || haystack.contains("configuration") ->
            "Провайдер входа не настроен. Обычно это значит, что на устройстве нет Google Play Services, " +
                "либо установлены слишком старые."

        haystack.contains("10:") || haystack.contains("developer_error") || haystack.contains("api: 10") ->
            "DEVELOPER_ERROR: Google не узнал приложение. Проверьте Android-клиент — имя пакета и SHA-1 " +
                "должны совпадать с показанными в приложении до символа."

        haystack.contains("web client") || haystack.contains("audience") || haystack.contains("client id") ->
            "Этот Client ID не подходит. В приложение нужен Client ID именно веб-записи " +
                "(Application type «Web application»), а не Android-записи."

        haystack.contains("access blocked") || haystack.contains("invalid_request") ||
            haystack.contains("403") || haystack.contains("invalid client") ->
            "Google заблокировал вход. Заполните «OAuth consent screen» и добавьте свой аккаунт в Test users."

        else -> "Вход не удался. Ниже — технический текст, пришлите его, разберём."
    }
}

/** Подсказка + технический текст, чтобы ошибку можно было показать и переслать. */
fun describeGoogleError(error: Throwable): String {
    val type = (error as? GetCredentialException)?.type
    return buildString {
        append(googleErrorHint(error))
        append("\n\nТехнически: ")
        append(error::class.java.simpleName)
        if (!type.isNullOrBlank()) {
            append(" (")
            append(type)
            append(")")
        }
        error.message?.let {
            append(": ")
            append(it)
        }
    }
}
