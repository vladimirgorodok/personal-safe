package ru.opencode.safe.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {

    private val passphrase = "тестовая фраза".toCharArray()

    @Test
    fun `шифрование и расшифровка возвращают исходные данные`() {
        val plain = """{"secrets":[{"title":"Тест","value":"abc123"}]}""".toByteArray()
        val blob = BackupCrypto.encrypt(plain, passphrase)
        assertArrayEquals(plain, BackupCrypto.decrypt(blob, passphrase))
    }

    @Test
    fun `копия не содержит исходный текст`() {
        val plain = "совершенно секретный токен".toByteArray()
        val blob = BackupCrypto.encrypt(plain, passphrase)
        assertFalse(String(blob, Charsets.ISO_8859_1).contains("совершенно"))
    }

    @Test
    fun `неверная фраза не расшифровывает копию`() {
        val blob = BackupCrypto.encrypt("данные".toByteArray(), passphrase)
        assertThrows(Exception::class.java) {
            BackupCrypto.decrypt(blob, "другая фраза".toCharArray())
        }
    }

    @Test
    fun `испорченный файл отвергается`() {
        val blob = BackupCrypto.encrypt("данные".toByteArray(), passphrase)
        blob[blob.size - 1] = (blob[blob.size - 1] + 1).toByte()
        assertThrows(Exception::class.java) { BackupCrypto.decrypt(blob, passphrase) }
    }

    @Test
    fun `чужие байты не считаются резервной копией`() {
        assertFalse(BackupCrypto.looksLikeBackup("обычный текст".toByteArray()))
        assertTrue(BackupCrypto.looksLikeBackup(BackupCrypto.encrypt("данные".toByteArray(), passphrase)))
    }

    @Test
    fun `короткая фраза не принимается`() {
        assertThrows(Exception::class.java) {
            BackupCrypto.encrypt("данные".toByteArray(), "123".toCharArray())
        }
    }

    // ---------------- Копия с ключом (файл ключа лежит рядом в Google Drive) ----------------

    @Test
    fun `копия с ключом расшифровывается тем же ключом`() {
        val key = BackupCrypto.newKey()
        val plain = """{"secrets":[{"title":"Тест","value":"abc123"}]}""".toByteArray()
        val blob = BackupCrypto.encryptWithKey(plain, key)
        assertTrue(BackupCrypto.isKeyBackup(blob))
        assertFalse(BackupCrypto.isPassphraseBackup(blob))
        assertArrayEquals(plain, BackupCrypto.decryptWithKey(blob, key))
    }

    @Test
    fun `другой ключ не расшифровывает копию`() {
        val blob = BackupCrypto.encryptWithKey("данные".toByteArray(), BackupCrypto.newKey())
        assertThrows(Exception::class.java) {
            BackupCrypto.decryptWithKey(blob, BackupCrypto.newKey())
        }
    }

    @Test
    fun `копия с ключом не содержит исходный текст`() {
        val blob = BackupCrypto.encryptWithKey("совершенно секретный токен".toByteArray(), BackupCrypto.newKey())
        assertFalse(String(blob, Charsets.ISO_8859_1).contains("совершенно"))
    }

    @Test
    fun `испорченная копия с ключом отвергается`() {
        val key = BackupCrypto.newKey()
        val blob = BackupCrypto.encryptWithKey("данные".toByteArray(), key)
        blob[blob.size - 1] = (blob[blob.size - 1] + 1).toByte()
        assertThrows(Exception::class.java) { BackupCrypto.decryptWithKey(blob, key) }
    }

    @Test
    fun `ключ неверной длины не принимается`() {
        assertThrows(Exception::class.java) {
            BackupCrypto.encryptWithKey("данные".toByteArray(), ByteArray(8))
        }
    }

    @Test
    fun `форматы различаются по заголовку`() {
        val withPhrase = BackupCrypto.encrypt("данные".toByteArray(), passphrase)
        val withKey = BackupCrypto.encryptWithKey("данные".toByteArray(), BackupCrypto.newKey())
        assertFalse(BackupCrypto.isKeyBackup(withPhrase))
        assertFalse(BackupCrypto.isPassphraseBackup(withKey))
        assertTrue(BackupCrypto.looksLikeBackup(withPhrase))
        assertTrue(BackupCrypto.looksLikeBackup(withKey))
    }
}
