package ru.opencode.safe.data.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Шифрование резервной копии.
 *
 * Два режима:
 *  - **парольная фраза** (`SAFEBAK1`) — ключ выводится из фразы, копию можно расшифровать
 *    на другом устройстве, зная фразу;
 *  - **ключ рядом с копией** (`SAFEKEY1`) — случайный ключ AES-256 лежит отдельным файлом
 *    в скрытой папке приложения на Google Диске, поэтому фразу вводить не нужно и копия
 *    делается автоматически. Плата за удобство — копию защищает доступ к Google-аккаунту.
 *
 * Формат с фразой: MAGIC (9) | salt (16) | iv (12) | ciphertext+tag
 * Формат с ключом: KEY_MAGIC (8) | iv (12) | ciphertext+tag
 */
object BackupCrypto {

    const val MIN_PASSPHRASE = 6

    /** Длина ключа автоматической копии, байт (AES-256). */
    const val KEY_LEN = 32

    private val MAGIC = "SAFEBAK1".toByteArray(Charsets.US_ASCII)
    private val KEY_MAGIC = "SAFEKEY1".toByteArray(Charsets.US_ASCII)
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val ITERATIONS = 120_000
    private val HEADER = MAGIC.size + SALT_LEN + IV_LEN
    private val KEY_HEADER = KEY_MAGIC.size + IV_LEN

    private val random = SecureRandom()

    /** true, если это похоже на нашу резервную копию (проверяем только заголовок). */
    fun looksLikeBackup(blob: ByteArray): Boolean = isPassphraseBackup(blob) || isKeyBackup(blob)

    /** Копия, зашифрованная фразой (старый режим). */
    fun isPassphraseBackup(blob: ByteArray): Boolean =
        blob.size > HEADER && blob.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    /** Копия, зашифрованная ключом, который лежит рядом в Google Drive. */
    fun isKeyBackup(blob: ByteArray): Boolean =
        blob.size > KEY_HEADER && blob.copyOfRange(0, KEY_MAGIC.size).contentEquals(KEY_MAGIC)

    /** Случайный ключ для автоматической копии. */
    fun newKey(): ByteArray = ByteArray(KEY_LEN).also(random::nextBytes)

    fun encryptWithKey(plain: ByteArray, key: ByteArray): ByteArray {
        requireKey(key)
        val iv = ByteArray(IV_LEN).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return KEY_MAGIC + iv + cipher.doFinal(plain)
    }

    fun decryptWithKey(blob: ByteArray, key: ByteArray): ByteArray {
        requireKey(key)
        require(isKeyBackup(blob)) { "Копия сделана в другом формате" }
        val iv = blob.copyOfRange(KEY_MAGIC.size, KEY_HEADER)
        val body = blob.copyOfRange(KEY_HEADER, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(body)
    }

    fun encrypt(plain: ByteArray, passphrase: CharArray): ByteArray {
        validate(passphrase)
        val salt = ByteArray(SALT_LEN).also(random::nextBytes)
        val iv = ByteArray(IV_LEN).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
        return MAGIC + salt + iv + cipher.doFinal(plain)
    }

    fun decrypt(blob: ByteArray, passphrase: CharArray): ByteArray {
        require(blob.size > HEADER) { "Файл резервной копии повреждён" }
        require(blob.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            "Это не резервная копия «Личного сейфа»"
        }
        val salt = blob.copyOfRange(MAGIC.size, MAGIC.size + SALT_LEN)
        val iv = blob.copyOfRange(MAGIC.size + SALT_LEN, HEADER)
        val body = blob.copyOfRange(HEADER, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(body)
    }

    private fun key(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun validate(passphrase: CharArray) {
        require(passphrase.size >= MIN_PASSPHRASE) {
            "Фраза слишком короткая: нужно минимум $MIN_PASSPHRASE символов"
        }
    }

    private fun requireKey(key: ByteArray) {
        require(key.size == KEY_LEN) { "Ключ копии повреждён" }
    }

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
}
