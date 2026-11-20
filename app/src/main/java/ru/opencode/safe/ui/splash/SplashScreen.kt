package ru.opencode.safe.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.opencode.safe.R
import ru.opencode.safe.ui.theme.LocalThemeSet
import kotlin.math.cos
import kotlin.math.sin

/** Заставка на 5 секунд: стеклянный щит, сияние, частицы и прогресс. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val theme = LocalThemeSet.current
    val progress = remember { Animatable(0f) }
    val appear = remember { Animatable(0f) }

    val infinite = rememberInfiniteTransition(label = "splash")
    val spin by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.92f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
    )
    val shimmer by infinite.animateFloat(
        initialValue = -0.3f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "shimmer",
    )

    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(5000, easing = LinearEasing))
        onFinished()
    }

    val gradient = theme.gradient

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(gradient.map { it.copy(alpha = 0.9f) } + theme.scheme.background)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h * 0.42f)

            // вращающееся сияние
            rotate(spin, center) {
                for (i in 0..5) {
                    val angle = i * 60f
                    val r = w * 0.42f
                    val p = Offset(
                        center.x + r * cos(Math.toRadians(angle.toDouble())).toFloat(),
                        center.y + r * sin(Math.toRadians(angle.toDouble())).toFloat(),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(gradient[i % gradient.size].copy(alpha = 0.42f), Color.Transparent),
                            center = p, radius = w * 0.28f,
                        ),
                        radius = w * 0.28f, center = p,
                    )
                }
            }

            // стеклянное кольцо
            drawCircle(
                brush = Brush.sweepGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))),
                radius = w * 0.34f, center = center, style = Stroke(width = 2f),
            )
            drawCircle(color = Color.White.copy(alpha = 0.06f), radius = w * 0.30f, center = center)

            // частицы
            for (i in 0..22) {
                val fi = i.toFloat()
                val t = ((shimmer + fi / 22f) % 1.0f)
                val x = w * (0.1f + ((fi * 37f) % 80f) / 100f)
                val y = h * (1f - t) * 0.9f + h * 0.05f
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f + 0.25f * (1f - t)),
                    radius = (1.5f + (fi % 3f)) * density,
                    center = Offset(x, y),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(0.7f + appear.value * 0.3f)
                    .alpha(appear.value)
                    .scale(pulse),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.size(180.dp),
                )
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "Личный сейф",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(appear.value),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Ваши секреты под надёжным стеклом",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(appear.value * 0.95f),
            )
        }

        // индикатор прогресса
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .width(180.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.16f)),
        ) {
            Box(
                modifier = Modifier
                    .width(180.dp * progress.value)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Color.White, gradient.first()))),
            )
        }
    }
}
