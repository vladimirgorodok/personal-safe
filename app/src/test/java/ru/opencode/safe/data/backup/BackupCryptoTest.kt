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
}
