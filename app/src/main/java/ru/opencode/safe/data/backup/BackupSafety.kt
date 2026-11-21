package ru.opencode.safe.data.backup

/**
 * Защита от затирания резервной копии.
 *
 * Автоматическая копия уходит в Google Drive после каждого изменения и **перезаписывает**
 * удалённый файл. Если открыть приложение на новом устройстве, локальный сейф почти пуст,
 * и такое затирание уничтожило бы хорошую копию. Поэтому правило простое: не перезаписываем
 * копию, в которой секретов больше, чем у нас сейчас.
 */
object BackupSafety {

    fun safeToOverwrite(localSecrets: Int, remoteSecrets: Int): Boolean = remoteSecrets <= localSecrets
}
