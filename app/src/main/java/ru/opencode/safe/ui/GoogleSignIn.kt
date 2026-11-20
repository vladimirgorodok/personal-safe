package ru.opencode.safe.ui

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import ru.opencode.safe.data.model.UserProfile

/** Реальный вход через Google (Credential Manager). Без Client ID не вызывается. */
class GoogleSignIn(private val activity: Activity) {

    suspend fun signIn(serverClientId: String): UserProfile {
        val manager = CredentialManager.create(activity)
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
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
