package ru.opencode.safe.ui.drive

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

    /** Скрытая папка приложения — файл не виден в «Моём диске». */
    val SCOPE = Scope("https://www.googleapis.com/auth/drive.appdata")

    /** Google хочет показать экран согласия: PendingIntent нужно запустить и вернуть результат. */
    class ApprovalRequired(val pendingIntent: PendingIntent) :
        Exception("Требуется подтверждение доступа к Google Drive")

    suspend fun requestToken(activity: Activity): String {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(SCOPE))
            .build()
        val result = try {
            Identity.getAuthorizationClient(activity).authorize(request).awaitTask()
        } catch (e: ApiException) {
            val intent = e.status.resolution
            if (e.status.hasResolution() && intent != null) throw ApprovalRequired(intent)
            throw IllegalStateException(describe(e))
        }
        if (result.hasResolution() && result.pendingIntent != null) {
            throw ApprovalRequired(result.pendingIntent!!)
        }
        ensureScope(result)
        return result.accessToken ?: throw IllegalStateException("Google не выдал доступ к Drive")
    }

    suspend fun tokenFromIntent(activity: Activity, data: Intent): String {
        val result = try {
            Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
        } catch (e: ApiException) {
            throw IllegalStateException(describe(e))
        }
        ensureScope(result)
        return result.accessToken ?: throw IllegalStateException("Google не выдал доступ к Drive")
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
