package ru.opencode.safe.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * Мягкое появление блока при входе на экран. [index] задаёт лёгкую задержку,
 * чтобы карточки появлялись волной.
 */
@Composable
fun Appear(
    index: Int = 0,
    step: Int = 36,
    content: @Composable () -> Unit,
) {
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibleState.targetState = true }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(durationMillis = 260, delayMillis = (index.coerceAtMost(12)) * step)) +
            scaleIn(
                initialScale = 0.96f,
                animationSpec = tween(durationMillis = 260, delayMillis = (index.coerceAtMost(12)) * step),
            ),
    ) {
        content()
    }
}
