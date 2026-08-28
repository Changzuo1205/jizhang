package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BackgroundOptionType
import com.example.ui.theme.LocalAppBackgroundConfig

// Frosted Glass Light Effect Palette
val GlassBackgroundDark = Color(0xFF090D16)
val GlassSurfaceDark = Color(0xFF131B2E).copy(alpha = 0.55f)
val GlassSurfaceHighlight = Color(0xFF1E293B).copy(alpha = 0.40f)
val GlassWhiteTranslucent = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val GlassBorderGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFFFFF).copy(alpha = 0.38f),
        Color(0xFFFFFFFF).copy(alpha = 0.06f),
        Color(0xFF818CF8).copy(alpha = 0.25f),
        Color(0xFF38BDF8).copy(alpha = 0.12f)
    )
)

val GlowViolet = Color(0xFF8B5CF6)
val GlowCyan = Color(0xFF06B6D4)
val GlowPink = Color(0xFFF43F5E)
val GlowEmerald = Color(0xFF10B981)
val GlowAmber = Color(0xFFF59E0B)

/**
 * Adaptive Background supporting default 灰白纯色 (Gray-White Solid),
 * warm creamy ivory, custom color pickers, and immersive dark cosmic atmospheres.
 */
@Composable
fun GlassBackgroundWithGlow(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current

    val infiniteTransition = rememberInfiniteTransition(label = "AtmosphereGlow")
    
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbPulse1"
    )

    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 1.08f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbPulse2"
    )

    when (bgConfig.type) {
        BackgroundOptionType.GRAY_WHITE -> {
            // Default 灰白纯色: 纯色背景不画 orb（性能优化，跳过永久循环动画）
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(bgConfig.solidColor)
            ) {
                content()
            }
        }
        BackgroundOptionType.DEEP_COSMIC -> {
            // Immersive dark cosmic atmosphere
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0D1322),
                                Color(0xFF090D18),
                                Color(0xFF030712)
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-60).dp, y = (-40).dp)
                        .size(320.dp)
                        .scale(pulse1)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GlowCyan.copy(alpha = 0.38f),
                                    Color(0xFF3B82F6).copy(alpha = 0.20f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 80.dp, y = 80.dp)
                        .size(360.dp)
                        .scale(pulse2)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GlowViolet.copy(alpha = 0.35f),
                                    GlowPink.copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                content()
            }
        }
        BackgroundOptionType.AURORA_NIGHT -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF040D21),
                                Color(0xFF0A192F),
                                Color(0xFF020C1B)
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-40).dp)
                        .size(400.dp)
                        .scale(pulse1)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GlowEmerald.copy(alpha = 0.35f),
                                    GlowCyan.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                content()
            }
        }
        BackgroundOptionType.SLATE_DARK -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
            ) {
                content()
            }
        }
        BackgroundOptionType.CUSTOM_IMAGE -> {
            val context = LocalContext.current
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(if (bgConfig.isLight) Color(0xFFF1F5F9) else Color(0xFF0B0F19))
            ) {
                // Background Wallpaper Image
                if (!bgConfig.imageUri.isNullOrBlank()) {
                    val blurMod = if (bgConfig.blurRadius > 0f) {
                        Modifier
                            .fillMaxSize()
                            .blur(bgConfig.blurRadius.dp)
                    } else {
                        Modifier.fillMaxSize()
                    }

                    val imgData: Any = if (bgConfig.imageUri.startsWith("/") || bgConfig.imageUri.startsWith("file:")) {
                        File(bgConfig.imageUri.removePrefix("file://"))
                    } else {
                        bgConfig.imageUri
                    }

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imgData)
                            .crossfade(true)
                            .build(),
                        contentDescription = "自定义背景壁纸",
                        contentScale = ContentScale.Crop,
                        modifier = blurMod
                    )
                }

                // Frosted Glass Scrim / Dim Overlay for razor-sharp text contrast
                val overlayColor = if (bgConfig.isLight) {
                    Color.White.copy(alpha = bgConfig.frostAlpha.coerceIn(0f, 0.85f))
                } else {
                    Color.Black.copy(alpha = (bgConfig.frostAlpha + 0.15f).coerceIn(0f, 0.90f))
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                )

                // Atmospheric ambient lights
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 80.dp, y = (-40).dp)
                        .size(340.dp)
                        .scale(pulse1)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    if (bgConfig.isLight) Color(0xFF818CF8).copy(alpha = 0.20f) else GlowViolet.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                content()
            }
        }
        else -> {
            // Light / Custom Solid Color: 跳过 orb（性能优化）
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(bgConfig.solidColor)
            ) {
                content()
            }
        }
    }
}

/**
 * Frosted Glass Card with adaptive styling for both light (灰白纯色) and dark themes.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color? = null,
    borderColor: Brush? = null,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val effectiveBg = backgroundColor ?: bgConfig.cardBackground
    val effectiveBorder = borderColor ?: bgConfig.cardBorder

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = if (bgConfig.isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.2f)),
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(effectiveBg)
            .border(width = borderWidth, brush = effectiveBorder, shape = shape)
            .then(clickModifier)
    ) {
        // Subtle sheen in dark mode
        if (!bgConfig.isLight) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.01f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.45f
                            )
                        )
                    }
            )
        }
        content()
    }
}

/**
 * Frosted Glass Pill / Chip for tabs, filters, and categories.
 */
@Composable
fun GlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedGlowColor: Color = Color(0xFF6366F1),
    content: @Composable () -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current

    val bg = if (selected) {
        if (bgConfig.isLight) selectedGlowColor.copy(alpha = 0.18f) else selectedGlowColor.copy(alpha = 0.28f)
    } else {
        bgConfig.chipUnselectedBg
    }

    val borderBrush = if (selected) {
        Brush.linearGradient(
            listOf(
                selectedGlowColor.copy(alpha = 0.95f),
                selectedGlowColor.copy(alpha = 0.6f)
            )
        )
    } else {
        bgConfig.chipUnselectedBorder
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderBrush, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = if (bgConfig.isLight) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Luminous Glass Floating Action Button with animated halo glow ring.
 */
@Composable
fun GlassGlowFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = Color(0xFF6366F1),
    content: @Composable () -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val infiniteTransition = rememberInfiniteTransition(label = "FabGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (bgConfig.isLight) 0.35f else 0.5f,
        targetValue = if (bgConfig.isLight) 0.10f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    Box(
        modifier = modifier.size(68.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing Halo
        Box(
            modifier = Modifier
                .size(58.dp)
                .scale(glowScale)
                .background(
                    Brush.radialGradient(
                        listOf(
                            glowColor.copy(alpha = glowAlpha),
                            Color(0xFF38BDF8).copy(alpha = glowAlpha * 0.5f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // Glass Button
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF6366F1),
                            Color(0xFF4F46E5)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.7f),
                            Color.White.copy(alpha = 0.15f)
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
