package com.example.workoutapp.ui.workout

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workoutapp.domain.session.CountdownType
import com.example.workoutapp.ui.theme.NeonGreen

/**
 * Variant B ("Command bar") timer: a progress ring wrapping the countdown digits.
 * Tapping the ring skips the running timer (accessibility label "Skip timer").
 * Rest/Switch durations are managed in Settings only; there are no chips here.
 */
@Composable
fun TimerHeader(
    seconds: Int,
    totalSeconds: Int,
    isRunning: Boolean,
    isPaused: Boolean,
    timerType: CountdownType,
    onSkipTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showCountdown = isRunning || isPaused
    val ringSize = 136.dp
    val strokeWidth = 10.dp

    // Flash effect confined to the digits badge during the final 3 seconds.
    val infiniteTransition = rememberInfiniteTransition(label = "flash")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRunning && seconds <= 3 && seconds > 0) 0.6f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashAlpha"
    )

    val elapsedFraction = if (showCountdown && totalSeconds > 0) {
        ((totalSeconds - seconds).toFloat() / totalSeconds).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(ringSize)
                    .alpha(if (isPaused) 0.5f else 1f)
                    .then(
                        if (showCountdown) {
                            Modifier
                                .semantics { contentDescription = "Skip timer" }
                                .clickable(
                                    onClickLabel = "Skip timer",
                                    role = Role.Button,
                                    onClick = onSkipTimer
                                )
                        } else {
                            Modifier
                        }
                    )
            ) {
                RingCanvas(
                    elapsedFraction = elapsedFraction,
                    strokeWidth = strokeWidth,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    progressColor = NeonGreen,
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showCountdown && timerType != CountdownType.NONE) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = when (timerType) {
                                CountdownType.SWITCH -> MaterialTheme.colorScheme.tertiaryContainer
                                CountdownType.HOLD -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                        ) {
                            Text(
                                text = when (timerType) {
                                    CountdownType.SWITCH -> "SWITCH"
                                    CountdownType.HOLD -> "HOLD"
                                    else -> "REST"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isRunning && seconds <= 3 && seconds > 0) {
                            NeonGreen.copy(alpha = flashAlpha)
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Text(
                            text = if (showCountdown) {
                                String.format("%02d:%02d", seconds / 60, seconds % 60)
                            } else {
                                "--:--"
                            },
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (showCountdown) NeonGreen else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                    Text(
                        text = if (showCountdown) "tap = skip" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RingCanvas(
    elapsedFraction: Float,
    strokeWidth: androidx.compose.ui.unit.Dp,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val arcSize = Size(size.width - strokePx, size.height - strokePx)
        val topLeft = Offset(strokePx / 2, strokePx / 2)
        val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)

        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        if (elapsedFraction > 0f) {
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * elapsedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }
    }
}
