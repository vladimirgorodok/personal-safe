package ru.opencode.safe.ui.nav

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

sealed interface Screen {
    data object Vault : Screen
    data object Analyze : Screen
    data object ImageSearch : Screen
    data object Settings : Screen
    data object Appearance : Screen
    data object AiSettings : Screen
    data object Backup : Screen
    data class CategoryDetail(val categoryId: String) : Screen
    data class CategoryEdit(val categoryId: String?) : Screen
    data class SecretDetail(val secretId: String) : Screen
    data class BrandDetail(val domain: String, val name: String) : Screen
}

class Navigator {
    val stack: SnapshotStateList<Screen> = mutableStateListOf(Screen.Vault)
    val current: Screen get() = stack.last()

    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun popTo(screen: Screen) {
        val index = stack.indexOf(screen)
        if (index >= 0) {
            while (stack.size > index + 1) stack.removeAt(stack.lastIndex)
        }
    }

    fun resetTo(screen: Screen) {
        stack.clear()
        stack.add(screen)
    }

    val canGoBack: Boolean get() = stack.size > 1
}
