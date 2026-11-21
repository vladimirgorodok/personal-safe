package ru.opencode.safe.ui

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import ru.opencode.safe.data.model.UserProfile

/** Реальный вход через Google (Credential Manager). Без Client ID не вызывается. */
class GoogleSignIn(private val activity: Activity) {

    /**
     * Сначала пробуем войти молча — тем аккаунтом, которым в приложении уже пользовались:
     * Google подставит его сам, без выбора и лишних подтверждений. Если сохранённых аккаунтов
     * нет (первый вход или после очистки данных), показываем обычный выбор аккаунта.
     */
    suspend fun signIn(serverClientId: String): UserProfile {
        return try {
            credential(serverClientId, silent = true)
        } catch (e: GetCredentialCancellationException) {
            throw e
        } catch (_: Exception) {
            credential(serverClientId, silent = false)
        }
    }

    private suspend fun credential(serverClientId: String, silent: Boolean): UserProfile {
        val manager = CredentialManager.create(activity)
        // Тихий вход возможен только среди аккаунтов, которыми уже входили в приложение.
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(silent)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(silent)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = manager.getCredential(activity, request)
        val credential = response.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val token = GoogleIdTokenCredential.createFrom(credential.data)
            return UserProfile(
                name = token.displayName ?: "Google",
                email = token.id,
                avatarUrl = token.profilePictureUri?.toString(),
                provider = "google",
                isDemo = false,
            )
        }
        throw IllegalStateException("Не удалось получить Google-токен")
    }
}
