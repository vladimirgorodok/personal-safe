package ru.opencode.safe.ui

/**
 * Что показывать при запуске приложения.
 *
 * Если профиль уже сохранён, а в настройках включено «Запомнить вход», экран Google-входа
 * не показываем: пользователь сразу попадает в сейф. Защиту при запуске можно включить
 * отдельно — «Настройки» → «Вход по биометрии».
 */
object SessionPolicy {
    fun startPhase(hasProfile: Boolean, rememberSignIn: Boolean): Phase =
        if (hasProfile && rememberSignIn) Phase.READY else Phase.AUTH
}
