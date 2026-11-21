package ru.opencode.safe.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionPolicyTest {

    @Test
    fun `сохранённый профиль открывает сейф без экрана входа`() {
        assertEquals(Phase.READY, SessionPolicy.startPhase(hasProfile = true, rememberSignIn = true))
    }

    @Test
    fun `без профиля показываем вход`() {
        assertEquals(Phase.AUTH, SessionPolicy.startPhase(hasProfile = false, rememberSignIn = true))
    }

    @Test
    fun `выключенное запоминание входа показывает вход`() {
        assertEquals(Phase.AUTH, SessionPolicy.startPhase(hasProfile = true, rememberSignIn = false))
    }
}
