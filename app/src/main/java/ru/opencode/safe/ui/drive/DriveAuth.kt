package ru.opencode.safe.ui.drive

import android.accounts.Account
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.opencode.safe.data.backup.DriveErrors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Доступ к Google Drive. Play Services спрашивает разрешение у пользователя и
 * возвращает короткоживущий токен, которым подписываются запросы к Drive REST API.
 */
object DriveAuth {

    private const val GOOGLE_ACCOUNT_TYPE = "com.google"

    /** Токен доступа живёт час. Держим его в памяти, чтобы не спрашивать разрешение на каждый экран. */
    private const val TOKEN_TTL_MS = 50 * 60 * 1000L

    private var cachedToken: String? = null
    private var cachedUntil = 0L

    /** Скрытая папка приложения — файл не виден в «Моём диске». */
    val SCOPE = Scope("https://www.googleapis.com/auth/drive.appdata")

    /** Google хочет показать экран согласия: PendingIntent нужно запустить и вернуть результат. */
    class ApprovalRequired(val pendingIntent: PendingIntent) :
        Exception("Требуется подтверждение доступа к Google Drive")

    /** Токен, полученный ранее в этой сессии и ещё не истёкший. */
    fun cachedToken(): String? = cachedToken?.takeIf { System.currentTimeMillis() < cachedUntil }

    /** Забыть токен — например, при выходе из аккаунта. */
    fun forget() {
        cachedToken = null
        cachedUntil = 0L
    }

    /**
     * @param account почта Google-аккаунта. Если её передать, Google не станет спрашивать,
     * каким аккаунтом входить, — подставит этот. Если он больше недоступен, спросит обычным образом.
     */
    suspend fun requestToken(activity: Activity, account: String? = null): String {
        cachedToken()?.let { return it }
        if (!account.isNullOrBlank()) {
            try {
                return authorize(activity, Account(account, GOOGLE_ACCOUNT_TYPE))
            } catch (approval: ApprovalRequired) {
                throw approval
            } catch (e: ApiException) {
                val intent = e.status.resolution
                if (e.status.hasResolution() && intent != null) throw ApprovalRequired(intent)
                // Аккаунт не подошёл — повторим без него, Google покажет выбор.
            }
        }
        return try {
            authorize(activity, null)
        } catch (e: ApiException) {
            val intent = e.status.resolution
            if (e.status.hasResolution() && intent != null) throw ApprovalRequired(intent)
            throw IllegalStateException(describe(e))
        }
    }

    suspend fun tokenFromIntent(activity: Activity, data: Intent): String {
        val result = try {
            Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
        } catch (e: ApiException) {
            throw IllegalStateException(describe(e))
        }
        ensureScope(result)
        return remember(result.accessToken ?: throw IllegalStateException("Google не выдал доступ к Drive"))
    }

    private suspend fun authorize(activity: Activity, account: Account?): String {
        val builder = AuthorizationRequest.builder().setRequestedScopes(listOf(SCOPE))
        if (account != null) builder.setAccount(account)
        val result = Identity.getAuthorizationClient(activity).authorize(builder.build()).awaitTask()
        if (result.hasResolution() && result.pendingIntent != null) {
            throw ApprovalRequired(result.pendingIntent!!)
        }
        ensureScope(result)
        return remember(result.accessToken ?: throw IllegalStateException("Google не выдал доступ к Drive"))
    }

    private fun remember(token: String): String {
        cachedToken = token
        cachedUntil = System.currentTimeMillis() + TOKEN_TTL_MS
        return token
    }

    /** Если Google сообщил выданные области и нужной среди них нет — говорим об этом прямо. */
    private fun ensureScope(result: AuthorizationResult) {
        val granted = result.grantedScopes ?: return
        if (granted.isNotEmpty() && granted.none { it.contains("drive.appdata") }) {
            throw IllegalStateException(
                "Google выдал доступ без нужной области. Разрешено: ${granted.joinToString(", ")}. " +
                    "Добавьте область ${DriveErrors.SCOPE} на экране согласия OAuth (Data access → Scopes) " +
                    "и войдите тем же аккаунтом.",
            )
        }
    }

    private fun describe(e: ApiException): String = when (e.statusCode) {
        12501 -> "Доступ к Google Drive не предоставлен"
        else -> "Google вернул ошибку доступа (${e.statusCode}): ${e.statusMessage ?: "без описания"}"
    }
}

internal suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { value -> if (cont.isActive) cont.resume(value) }
    addOnFailureListener { error -> if (cont.isActive) cont.resumeWithException(error) }
    addOnCanceledListener { cont.cancel() }
}
