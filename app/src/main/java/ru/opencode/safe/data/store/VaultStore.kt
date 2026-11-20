package ru.opencode.safe.data.store

import org.json.JSONObject
import ru.opencode.safe.data.crypto.KeystoreCrypto
import ru.opencode.safe.data.model.VaultData
import java.io.File

/**
 * Постоянное хранилище сейфа: один зашифрованный JSON-файл.
 * Пароли/токены лежат на диске только в зашифрованном виде.
 */
class VaultStore(
    private val file: File,
    private val crypto: KeystoreCrypto,
) {

    fun load(): VaultData {
        val text = crypto.readText(file) ?: return VaultData()
        return runCatching { VaultData.fromJson(JSONObject(text)) }.getOrElse { VaultData() }
    }

    fun save(data: VaultData) {
        crypto.writeText(file, data.toJson().toString())
    }

    fun reset() {
        if (file.exists()) file.delete()
    }
}
