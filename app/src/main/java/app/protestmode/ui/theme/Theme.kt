package app.protestmode.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class StatusColors(
    val exposed: Color,
    val onExposed: Color,
    val partial: Color,
    val onPartial: Color,
    val discreet: Color,
    val onDiscreet: Color,
    val caution: Color,
    val onCaution: Color,
    val unknown: Color
)

private val LightStatus =
    StatusColors(
        exposed = Color(0xFFB3261E),
        onExposed = Color.White,
        partial = Color(0xFFF57C00),
        onPartial = Color(0xFF1B1B1B),
        discreet = Color(0xFF1B6E20),
        onDiscreet = Color.White,
        caution = Color(0xFFF2C230),
        onCaution = Color(0xFF1B1B1B),
        unknown = Color(0xFF5F6368)
    )

private val DarkStatus =
    StatusColors(
        exposed = Color(0xFFFFB4AB),
        onExposed = Color(0xFF1B1B1B),
        partial = Color(0xFFFFB870),
        onPartial = Color(0xFF1B1B1B),
        discreet = Color(0xFF8FD88B),
        onDiscreet = Color(0xFF1B1B1B),
        caution = Color(0xFFFFE08A),
        onCaution = Color(0xFF1B1B1B),
        unknown = Color(0xFFC4C7C5)
    )

// Neutral greys around the night blue primary. Material 3 defaults are built on a violet seed, which
// tints every background and surface pink; each surface role is therefore defined explicitly.
private val LightScheme =
    lightColorScheme(
        primary = Color(0xFF1B3A4B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC6E7FF),
        onPrimaryContainer = Color(0xFF001E2E),
        secondary = Color(0xFF4D616C),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDCE4EA),
        onSecondaryContainer = Color(0xFF151D23),
        tertiary = Color(0xFF4D616C),
        background = Color(0xFFFCFCFC),
        onBackground = Color(0xFF1A1C1E),
        surface = Color(0xFFFCFCFC),
        onSurface = Color(0xFF1A1C1E),
        surfaceVariant = Color(0xFFE7E8EA),
        onSurfaceVariant = Color(0xFF44474B),
        surfaceTint = Color(0xFF1B3A4B),
        surfaceBright = Color(0xFFFCFCFC),
        surfaceDim = Color(0xFFDDDEE0),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF6F7F8),
        surfaceContainer = Color(0xFFF1F2F4),
        surfaceContainerHigh = Color(0xFFEBEDEF),
        surfaceContainerHighest = Color(0xFFE5E7EA),
        outline = Color(0xFF75787C),
        outlineVariant = Color(0xFFC5C7CB),
        inverseSurface = Color(0xFF2F3133),
        inverseOnSurface = Color(0xFFF0F1F3),
        inversePrimary = Color(0xFF9DCBEA)
    )

private val DarkScheme =
    darkColorScheme(
        primary = Color(0xFF9DCBEA),
        onPrimary = Color(0xFF00344D),
        primaryContainer = Color(0xFF1B3A4B),
        onPrimaryContainer = Color(0xFFC6E7FF),
        secondary = Color(0xFFB4CAD6),
        onSecondary = Color(0xFF1F333D),
        secondaryContainer = Color(0xFF374851),
        onSecondaryContainer = Color(0xFFD0E6F2),
        tertiary = Color(0xFFB4CAD6),
        background = Color(0xFF121416),
        onBackground = Color(0xFFE2E2E5),
        surface = Color(0xFF121416),
        onSurface = Color(0xFFE2E2E5),
        surfaceVariant = Color(0xFF44474B),
        onSurfaceVariant = Color(0xFFC5C7CB),
        surfaceTint = Color(0xFF9DCBEA),
        surfaceBright = Color(0xFF38393C),
        surfaceDim = Color(0xFF121416),
        surfaceContainerLowest = Color(0xFF0D0F11),
        surfaceContainerLow = Color(0xFF1A1C1E),
        surfaceContainer = Color(0xFF1E2022),
        surfaceContainerHigh = Color(0xFF282A2D),
        surfaceContainerHighest = Color(0xFF333538),
        outline = Color(0xFF8F9196),
        outlineVariant = Color(0xFF44474B),
        inverseSurface = Color(0xFFE2E2E5),
        inverseOnSurface = Color(0xFF2F3133),
        inversePrimary = Color(0xFF1B3A4B)
    )

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

@Composable
fun ProtestModeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // Dynamic colour is not used: it would tint the app after the wallpaper, differently on each phone.
    val scheme = if (darkTheme) DarkScheme else LightScheme
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
