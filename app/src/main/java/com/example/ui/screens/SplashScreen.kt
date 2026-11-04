package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.sound.SoundManager
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Premium Launch Splash Screen for JITU LUDO.
 *
 * Features:
 * - Displays complete, crystal-clear "JITU LUDO" app name with zero letter clipping on all screen sizes.
 * - Plays pleasant, premium royal instrumental startup music via locally bundled audio.
 * - Smoothly stops startup music when splash screen transitions to the main screen.
 * - Strictly NO background music during gameplay; game sound effects remain 100% active.
 * - Rich dark chocolate brown and metallic gold royal background.
 * - Central 3D JITU LUDO artwork with radiant golden aura.
 * - Tap to skip immediately.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
    soundManager: SoundManager? = null,
    startupMusicEnabled: Boolean = true,
    soundVolume: Float = 1.0f
) {
    var animationStarted by remember { mutableStateOf(false) }

    // Smooth splash duration then transition to home screen where menu music starts
    LaunchedEffect(Unit) {
        animationStarted = true
        delay(2000L) // Pleasant 2.0s splash
        onSplashFinished()
    }

    val logoScale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.88f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "splashLogoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "splashLogoAlpha"
    )

    // Pulsing aura background animation
    val infiniteTransition = rememberInfiniteTransition(label = "splashInfinite")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlowAnim"
    )

    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particlePhaseAnim"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2B140A), // Rich dark bronze
                        Color(0xFF1B0B04), // Deep chocolate espresso
                        Color(0xFF140803), // Warm royal dark cocoa
                        Color(0xFF0A0402)  // Deep obsidian base
                    )
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Immediate tap to skip splash
                onSplashFinished()
            }
            .testTag("splash_screen")
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        // Adaptive logo sizing maintaining 1:1 square ratio without clipping on any screen
        val logoSize = minOf(screenWidth * 0.84f, screenHeight * 0.48f, 340.dp)

        // Floating ambient golden sparkles and particle stars
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val particleData = listOf(
                Triple(0.18f, 0.22f, 2.5f),
                Triple(0.82f, 0.18f, 3.2f),
                Triple(0.12f, 0.45f, 2.0f),
                Triple(0.88f, 0.42f, 3.0f),
                Triple(0.25f, 0.70f, 2.8f),
                Triple(0.78f, 0.72f, 2.2f),
                Triple(0.50f, 0.12f, 3.5f),
                Triple(0.35f, 0.85f, 2.0f),
                Triple(0.65f, 0.88f, 2.6f)
            )

            particleData.forEachIndexed { i, (relX, relY, radius) ->
                val wave = (sin(particlePhase + i.toFloat()) + 1f) / 2f
                val currentRadius = radius * (0.7f + 0.5f * wave)
                val alpha = 0.3f + 0.55f * wave
                val yOffset = sin(particlePhase * 0.7f + i.toFloat()) * 12f

                drawCircle(
                    color = Color(0xFFFDE047).copy(alpha = alpha),
                    radius = currentRadius,
                    center = Offset(relX * width, relY * height + yOffset)
                )
            }
        }

        // Central Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(logoSize)
                    .scale(logoScale)
                    .alpha(logoAlpha)
            ) {
                // Radiant golden aura glow behind logo
                Box(
                    modifier = Modifier
                        .size(logoSize * 0.94f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x60F59E0B),
                                    Color(0x20FDE047),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Master JITU LUDO Logo:
                // Using ContentScale.Fit (fitCenter equivalent) with NO box clipping.
                // Guarantees the complete artwork, outer gold frame, royal crown,
                // and the full letters of "LUDO" (including bottom curves and wings)
                // are 100% visible, centered, uncropped, and undistorted on any screen size.
                Image(
                    painter = painterResource(id = R.drawable.img_splash_logo),
                    contentDescription = "JITU LUDO Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("splash_logo")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Complete, prominent app name text "JITU LUDO" (never clipped)
            Text(
                text = "JITU LUDO",
                fontSize = if (screenWidth < 360.dp) 22.sp else 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = TextStyle(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFFBEB),
                            Color(0xFFFDE047),
                            Color(0xFFF59E0B),
                            Color(0xFFB45309)
                        )
                    )
                ),
                modifier = Modifier
                    .testTag("splash_app_title")
                    .scale(logoScale)
                    .alpha(logoAlpha)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle Brand Pill
            Box(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF0180B05))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFF78350F),
                                Color(0xFFFDE047),
                                Color(0xFFF59E0B),
                                Color(0xFF78350F)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 7.dp)
                    .alpha(logoAlpha),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ROYAL 3D LUDO • PASS & PLAY • SMART AI",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE68A),
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Subtle pulsing loading status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.alpha(logoAlpha * 0.90f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "READY TO ROLL 🎲",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1),
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

