package ru.opencode.safe

import android.app.Application
import ru.opencode.safe.core.AppContainer

class SafeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
