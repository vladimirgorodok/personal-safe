package ru.opencode.safe.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSafetyTest {

    @Test
    fun `пустой сейф не затирает копию с данными`() {
        assertFalse(BackupSafety.safeToOverwrite(localSecrets = 0, remoteSecrets = 12))
    }

    @Test
    fun `равное число секретов перезаписать можно`() {
        assertTrue(BackupSafety.safeToOverwrite(localSecrets = 5, remoteSecrets = 5))
    }

    @Test
    fun `своих секретов больше — можно`() {
        assertTrue(BackupSafety.safeToOverwrite(localSecrets = 7, remoteSecrets = 3))
    }

    @Test
    fun `пустая удалённая копия не мешает`() {
        assertTrue(BackupSafety.safeToOverwrite(localSecrets = 3, remoteSecrets = 0))
    }
}
