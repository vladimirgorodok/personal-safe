package ru.opencode.safe.core

import android.content.Context
import okhttp3.OkHttpClient
import ru.opencode.safe.data.ai.AgentAnalyzer
import ru.opencode.safe.data.ai.AiClient
import ru.opencode.safe.data.crypto.KeystoreCrypto
import ru.opencode.safe.data.icons.IconRepository
import ru.opencode.safe.data.settings.SettingsRepository
import ru.opencode.safe.data.store.VaultStore
import java.io.File
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    val settings: SettingsRepository = SettingsRepository(context.applicationContext)

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val aiClient = AiClient(http)
    val analyzer = AgentAnalyzer(aiClient)
    val icons = IconRepository(File(context.filesDir, "icons"), http)

    private val crypto = KeystoreCrypto()
    val vault = VaultStore(File(context.filesDir, "vault.enc"), crypto)

    val cacheDir: File = File(context.cacheDir, "attachments").apply { mkdirs() }
}
