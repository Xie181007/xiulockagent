package com.xiu.lockagent.ui

import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.example.R
import com.example.ui.theme.MysticBackground
import com.example.ui.theme.MysticBorder
import com.example.ui.theme.MysticContainer
import com.example.ui.theme.MysticCyan
import com.example.ui.theme.MysticPrimary
import com.example.ui.theme.MysticPrimaryHighlight
import com.example.ui.theme.MysticSuccess
import com.example.ui.theme.MysticTextPrimary
import com.xiu.lockagent.model.MascotPhase
import kotlinx.coroutines.delay

/**
 * Komponen Maskot 3D Interaktif Xiu LockAgent (Tepat 56x56 dp)
 * Mendukung Lottie Animation Engine (@raw/xiu_mascot_3d.json) dan fallback Canvas.
 * Mendukung 4 Fase Visual: IDLE, MENDENGARKAN, MENGEKSEKUSI, SUKSES.
 */
@Composable
fun XiuMascot56Dp(
    phase: MascotPhase,
    modifier: Modifier = Modifier,
    audioRms: Float = 0f,
    useLottie: Boolean = true,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_anim")

    // Animasi Breathing untuk IDLE
    val idleScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_breathing"
    )

    // Rotasi Cincin Energi untuk MENGEKSEKUSI
    val executingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "executing_spin"
    )

    // Gelombang Pulsa untuk MENDENGARKAN
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Flare Sukses
    val successBurst = remember { Animatable(0.8f) }
    LaunchedEffect(phase) {
        if (phase == MascotPhase.SUKSES) {
            successBurst.snapTo(0.8f)
            successBurst.animateTo(1.15f, animationSpec = tween(350, easing = FastOutSlowInEasing))
            successBurst.animateTo(1.0f, animationSpec = tween(250, easing = FastOutSlowInEasing))
        }
    }

    val currentScale = when (phase) {
        MascotPhase.IDLE -> idleScale
        MascotPhase.MENDENGARKAN -> 1.0f + (audioRms * 0.18f)
        MascotPhase.MENGEKSEKUSI -> 1.0f
        MascotPhase.SUKSES -> successBurst.value
    }

    Box(
        modifier = modifier
            .size(56.dp)
            .scale(currentScale)
            .clip(CircleShape)
            .background(MysticBackground)
            .border(1.5.dp, getPhaseBorderColor(phase), CircleShape)
            .shadow(6.dp, CircleShape, spotColor = getPhaseAuraColor(phase))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("xiu_mascot_56dp"),
        contentAlignment = Alignment.Center
    ) {
        if (useLottie) {
            // Render via Lottie Animation Engine ringan (@raw/xiu_mascot_3d.json)
            AndroidView(
                factory = { ctx ->
                    LottieAnimationView(ctx).apply {
                        setAnimation(R.raw.xiu_mascot_3d)
                        repeatCount = LottieDrawable.INFINITE
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        enableMergePathsForKitKatAndAbove(true)
                    }
                },
                update = { lottie ->
                    when (phase) {
                        MascotPhase.IDLE -> {
                            lottie.setMinAndMaxFrame(0, 60)
                            lottie.speed = 0.9f
                            lottie.repeatCount = LottieDrawable.INFINITE
                            if (!lottie.isAnimating) lottie.playAnimation()
                        }
                        MascotPhase.MENDENGARKAN -> {
                            lottie.setMinAndMaxFrame(60, 120)
                            lottie.speed = 1.0f + (audioRms * 1.4f)
                            lottie.repeatCount = LottieDrawable.INFINITE
                            lottie.playAnimation()
                        }
                        MascotPhase.MENGEKSEKUSI -> {
                            lottie.setMinAndMaxFrame(120, 180)
                            lottie.speed = 1.8f
                            lottie.repeatCount = LottieDrawable.INFINITE
                            lottie.playAnimation()
                        }
                        MascotPhase.SUKSES -> {
                            lottie.setMinAndMaxFrame(60, 180)
                            lottie.speed = 2.0f
                            lottie.repeatCount = 1
                            lottie.playAnimation()
                        }
                    }
                },
                modifier = Modifier.size(52.dp)
            )
        } else {
            // Canvas Rendering 3D-Look Cyber Mascot Fallback
            Canvas(modifier = Modifier.size(56.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f - 4f

                // 1. Gambar Aura Latar
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            getPhaseAuraColor(phase).copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius + 6f
                    ),
                    radius = radius,
                    center = center
                )

                // 2. Lingkaran Dasar Bodi Maskot
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF27272A),
                            Color(0xFF18181B),
                            Color(0xFF09090B)
                        )
                    ),
                    radius = radius - 3f,
                    center = center
                )

                // 3. Render Spesifik Fase Visual
                when (phase) {
                    MascotPhase.IDLE -> {
                        drawIdlePhase(center, radius)
                    }
                    MascotPhase.MENDENGARKAN -> {
                        drawListeningPhase(center, radius, audioRms, pulseAlpha)
                    }
                    MascotPhase.MENGEKSEKUSI -> {
                        drawExecutingPhase(center, radius, executingRotation)
                    }
                    MascotPhase.SUKSES -> {
                        drawSuccessPhase(center, radius)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawIdlePhase(center: Offset, radius: Float) {
    // Visor Cyan/Indigo
    val visorWidth = radius * 1.1f
    val visorHeight = radius * 0.45f
    val visorTop = center.y - visorHeight / 2f
    val visorLeft = center.x - visorWidth / 2f

    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(MysticPrimary, MysticPrimaryHighlight, MysticCyan)
        ),
        topLeft = Offset(visorLeft, visorTop),
        size = Size(visorWidth, visorHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
    )

    // Mata Kembar Cyber Glow
    val eyeRadius = 4f
    val eyeY = center.y
    drawCircle(
        color = MysticTextPrimary,
        radius = eyeRadius,
        center = Offset(center.x - 10f, eyeY)
    )
    drawCircle(
        color = MysticTextPrimary,
        radius = eyeRadius,
        center = Offset(center.x + 10f, eyeY)
    )

    // Simbol Kunci/Lock Glyph di Dahi
    drawCircle(
        color = MysticPrimaryHighlight,
        radius = 3.5f,
        center = Offset(center.x, center.y - 14f)
    )
}

private fun DrawScope.drawListeningPhase(center: Offset, radius: Float, audioRms: Float, pulseAlpha: Float) {
    // Gelombang Suara Melingkar (Pulse Ring)
    val ringRadius = radius - 4f + (audioRms * 12f)
    drawCircle(
        color = MysticCyan.copy(alpha = pulseAlpha),
        radius = ringRadius.coerceAtMost(radius + 2f),
        center = center,
        style = Stroke(width = 2.5f)
    )

    // Mikrofon Cyber di Tengah
    drawCircle(
        color = MysticCyan,
        radius = 8f,
        center = center
    )
    drawRoundRect(
        color = MysticTextPrimary,
        topLeft = Offset(center.x - 3.5f, center.y - 9f),
        size = Size(7f, 13f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
    )
}

private fun DrawScope.drawExecutingPhase(center: Offset, radius: Float, rotationAngle: Float) {
    // Busur Cincin Berputar (Spinning Cyber Runes)
    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(
                MysticPrimary,
                MysticCyan,
                MysticPrimaryHighlight,
                Color.Transparent
            )
        ),
        startAngle = rotationAngle,
        sweepAngle = 260f,
        useCenter = false,
        topLeft = Offset(center.x - radius + 4f, center.y - radius + 4f),
        size = Size((radius - 4f) * 2, (radius - 4f) * 2),
        style = Stroke(width = 3f, cap = StrokeCap.Round)
    )

    // Inti Pemrosesan AI
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(MysticTextPrimary, MysticPrimaryHighlight, MysticPrimary)
        ),
        radius = 7f,
        center = center
    )
}

private fun DrawScope.drawSuccessPhase(center: Offset, radius: Float) {
    // Latar Belakang Sukses Zamrud
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                MysticSuccess.copy(alpha = 0.5f),
                Color.Transparent
            )
        ),
        radius = radius - 2f,
        center = center
    )

    // Tanda Centang Vektor pada Canvas
    val checkPath = Path().apply {
        moveTo(center.x - 10f, center.y)
        lineTo(center.x - 3f, center.y + 7f)
        lineTo(center.x + 11f, center.y - 7f)
    }
    drawPath(
        path = checkPath,
        color = MysticTextPrimary,
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )
}

private fun getPhaseBorderColor(phase: MascotPhase): Color {
    return when (phase) {
        MascotPhase.IDLE -> MysticBorder
        MascotPhase.MENDENGARKAN -> MysticCyan
        MascotPhase.MENGEKSEKUSI -> MysticPrimaryHighlight
        MascotPhase.SUKSES -> MysticSuccess
    }
}

private fun getPhaseAuraColor(phase: MascotPhase): Color {
    return when (phase) {
        MascotPhase.IDLE -> MysticPrimary
        MascotPhase.MENDENGARKAN -> MysticCyan
        MascotPhase.MENGEKSEKUSI -> MysticPrimaryHighlight
        MascotPhase.SUKSES -> MysticSuccess
    }
}

/**
 * AI Speech Bubble Pop-up (@drawable/bg_popup_card, radius 14dp, border 1dp)
 * Fitur auto-dismiss timer 3000 ms.
 */
@Composable
fun XiuSpeechBubble(
    message: String,
    phase: MascotPhase,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    if (message.isBlank()) return

    // Auto-dismiss timer 3000 ms
    LaunchedEffect(message) {
        delay(3000L)
        onDismiss()
    }

    Box(
        modifier = modifier
            .widthIn(min = 140.dp, max = 280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MysticContainer.copy(alpha = 0.95f))
            .border(1.dp, MysticBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clickable { onDismiss() }
            .testTag("xiu_speech_bubble")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon = when (phase) {
                MascotPhase.IDLE -> Icons.Default.AutoAwesome
                MascotPhase.MENDENGARKAN -> Icons.Default.Mic
                MascotPhase.MENGEKSEKUSI -> Icons.Default.PlayArrow
                MascotPhase.SUKSES -> Icons.Default.Check
            }
            val iconTint = when (phase) {
                MascotPhase.IDLE -> MysticPrimaryHighlight
                MascotPhase.MENDENGARKAN -> MysticCyan
                MascotPhase.MENGEKSEKUSI -> MysticPrimary
                MascotPhase.SUKSES -> MysticSuccess
            }

            Icon(
                imageVector = icon,
                contentDescription = "Status Maskot",
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = message,
                color = MysticTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
