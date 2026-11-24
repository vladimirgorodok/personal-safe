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

    /** Что делать при запуске, если в Google Drive уже лежит копия. */
    enum class StartupAction {
        /** Новая установка: локальный сейф ещё не создавался — поднимаем копию молча. */
        RESTORE,

        /** Локально данные есть, но копия полнее: молча не затираем, предлагаем кнопку. */
        OFFER,

        /** Копия не полнее локальной — ничего не делаем. */
        NONE,
    }

    /**
     * Решение по копии в Drive на старте.
     *
     * Молча восстанавливаем только на **чистой установке** (файла сейфа на устройстве ещё не было
     * и секретов нет) — тогда терять нечего. Во всех остальных случаях, когда копия полнее
     * локальных данных, спрашиваем: пользователь мог намеренно удалить записи, и автоматическая
     * загрузка вернула бы их обратно.
     */
    fun startupAction(freshInstall: Boolean, localSecrets: Int, remoteSecrets: Int): StartupAction = when {
        remoteSecrets <= 0 -> StartupAction.NONE
        freshInstall && localSecrets == 0 -> StartupAction.RESTORE
        remoteSecrets > localSecrets -> StartupAction.OFFER
        else -> StartupAction.NONE
    }
}
