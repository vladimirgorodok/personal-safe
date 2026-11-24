package ru.opencode.safe.data.backup

import org.junit.Assert.assertEquals
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

    @Test
    fun `чистая установка поднимает копию молча`() {
        assertEquals(
            BackupSafety.StartupAction.RESTORE,
            BackupSafety.startupAction(freshInstall = true, localSecrets = 0, remoteSecrets = 12),
        )
    }

    @Test
    fun `копия полнее локальных данных — спрашиваем`() {
        assertEquals(
            BackupSafety.StartupAction.OFFER,
            BackupSafety.startupAction(freshInstall = false, localSecrets = 2, remoteSecrets = 12),
        )
    }

    @Test
    fun `пустой сейф на устройстве со следами работы — только предлагаем`() {
        // Пользователь мог удалить все записи намеренно: молча возвращать их нельзя.
        assertEquals(
            BackupSafety.StartupAction.OFFER,
            BackupSafety.startupAction(freshInstall = false, localSecrets = 0, remoteSecrets = 5),
        )
    }

    @Test
    fun `локальные данные полнее копии — ничего не делаем`() {
        assertEquals(
            BackupSafety.StartupAction.NONE,
            BackupSafety.startupAction(freshInstall = false, localSecrets = 9, remoteSecrets = 4),
        )
    }

    @Test
    fun `пустая копия ничего не предлагает`() {
        assertEquals(
            BackupSafety.StartupAction.NONE,
            BackupSafety.startupAction(freshInstall = true, localSecrets = 0, remoteSecrets = 0),
        )
        assertEquals(
            BackupSafety.StartupAction.NONE,
            BackupSafety.startupAction(freshInstall = true, localSecrets = 0, remoteSecrets = -1),
        )
    }
}
