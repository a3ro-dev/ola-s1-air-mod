package dev.a3ro.betterola.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Roles Material has no slot for. */
data class Tokens(
    val live: Color, val warn: Color, val ok: Color, val ink2: Color, val hairline: Color, val onLive: Color
)

val LocalTokens = staticCompositionLocalOf {
    Tokens(Live, Warn, Ok, Ink2, Hairline, OnLive)
}

private val Dark = darkColorScheme(
    primary = Live, onPrimary = OnLive,
    background = Ground, onBackground = Ink,
    surface = Surface, onSurface = Ink,
    surfaceVariant = Surface, onSurfaceVariant = Ink2,
    outline = Hairline, outlineVariant = Hairline,
    error = Fault, onError = Ground,
    secondaryContainer = Hairline, onSecondaryContainer = Ink,
)

private val Light = lightColorScheme(
    primary = LiveLight, onPrimary = Color.White,
    background = GroundLight, onBackground = InkLight,
    surface = SurfaceLight, onSurface = InkLight,
    surfaceVariant = SurfaceLight, onSurfaceVariant = Ink2Light,
    outline = HairlineLight, outlineVariant = HairlineLight,
    error = FaultLight, onError = Color.White,
    secondaryContainer = HairlineLight, onSecondaryContainer = InkLight,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun BetterOlaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val tokens = if (dark) Tokens(Live, Warn, Ok, Ink2, Hairline, OnLive)
    else Tokens(LiveLight, WarnLight, OkLight, Ink2Light, HairlineLight, Color.White)
    CompositionLocalProvider(LocalTokens provides tokens) {
        MaterialTheme(
            colorScheme = if (dark) Dark else Light,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

val tokens: Tokens
    @Composable get() = LocalTokens.current
