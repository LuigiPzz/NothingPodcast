package com.example.nothingpodcast.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import com.example.nothingpodcast.ui.theme.NothingWhite
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * An effect that makes the content "decompose" into pixels (dots) and dissolve.
 * Perfect for the Nothing OS aesthetic.
 */
@Composable
fun PixelDissolveContainer(
    isDissolving: Boolean,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val particles = remember { List(500) { Particle() } }

    LaunchedEffect(isDissolving) {
        if (isDissolving) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 960, easing = LinearEasing)
            )
            onAnimationEnd()
        }
    }

    Box(modifier = modifier) {
        // The main content that fades out
        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = 1f - (progress.value * 1.5f).coerceIn(0f, 1f)
                    // Slight scale and translation
                    scaleX = 1f + (progress.value * 0.1f)
                    scaleY = 1f + (progress.value * 0.1f)
                    translationY = -progress.value * 40f
                }
        ) {
            content()
        }

        // The "pixels" scattering effect
        if (progress.value > 0.05f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val p = progress.value
                val dotSize = 1.dp.toPx() 
                
                particles.forEach { particle ->
                    // Start from a more concentrated area
                    val startX = (particle.gridX.toFloat() / 50f) * size.width
                    val startY = (particle.gridY.toFloat() / 30f) * size.height
                    
                    val currentX = startX + (particle.dirX * p * 80f)
                    val currentY = startY + (particle.dirY * p * 80f)
                    
                    val particleAlpha = (1f - (p / particle.lifeSpan)).coerceIn(0f, 1f)
                    
                    if (particleAlpha > 0f) {
                        // Use Nothing colors for contrast
                        val color = if (particle.isRed) Color(0xFFFF0000) else Color.White
                        drawRect(
                            color = color.copy(alpha = particleAlpha),
                            topLeft = Offset(currentX, currentY),
                            size = Size(dotSize, dotSize)
                        )
                    }
                }
            }
        }
    }
}

private class Particle {
    val gridX = Random.nextInt(50)
    val gridY = Random.nextInt(30)
    val dirX = Random.nextFloat() * 8f - 4f
    val dirY = Random.nextFloat() * 8f - 4f
    val lifeSpan = Random.nextFloat() * 0.5f + 0.5f 
    val isRed = Random.nextFloat() > 0.8f // Some red particles for "WOW" effect
}
