package ru.opencode.safe.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ru.opencode.safe.R
import ru.opencode.safe.ui.theme.LocalThemeSet

@Composable
fun LockScreen(
    activity: FragmentActivity?,
    onUnlocked: () -> Unit,
    onFallback: () -> Unit,
) {
    val theme = LocalThemeSet.current

    fun authenticate() {
        val act = activity
        if (act == null) {
            onFallback()
            return
        }
        val manager = BiometricManager.from(act)
        val canAuth = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            onFallback()
            return
        }
        val prompt = BiometricPrompt(
            act,
            ContextCompat.getMainExecutor(act),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Разблокировать сейф")
            .setSubtitle("Подтвердите личность, чтобы открыть секреты")
            .setNegativeButtonText("Отмена")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        runCatching { prompt.authenticate(info) }.onFailure { onFallback() }
    }

    LaunchedEffect(Unit) { authenticate() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.scheme.background, theme.gradient.first().copy(alpha = 0.25f), theme.scheme.background))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Image(painterResource(R.drawable.ic_logo), null, modifier = Modifier.size(120.dp))
            Spacer(Modifier.height(20.dp))
            Text("Сейф заблокирован", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Разблокируйте с помощью отпечатка пальца или лица.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { authenticate() },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(52.dp),
            ) {
                Icon(Icons.Rounded.Fingerprint, null)
                Spacer(Modifier.height(0.dp))
                Text("  Разблокировать", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onFallback) { Text("Войти по-другому") }
        }
    }
}
