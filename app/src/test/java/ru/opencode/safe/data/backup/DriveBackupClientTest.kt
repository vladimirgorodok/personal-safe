package ru.opencode.safe.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveBackupClientTest {

    /**
     * Регрессия: в Drive API v3 скрытая папка приложения называется `appDataFolder`.
     * Написание `appdata` из v2 Google отвергает с «400 Invalid Value», поэтому копия
     * не находилась, хотя доступ к Drive уже был выдан.
     */
    @Test
    fun `список копий ищется в папке appDataFolder`() {
        val url = DriveBackupClient.listUrl()
        assertTrue(url, url.contains("spaces=appDataFolder"))
        assertFalse(url, url.contains("appdata"))
        assertTrue(url, url.contains("drive/v3/files"))
    }
}
