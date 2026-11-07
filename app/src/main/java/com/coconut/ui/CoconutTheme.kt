package com.coconut.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.coconut.R

// Host Grotesk is the EXCLUSIVE application typeface across all text elements
val HostGroteskFontFamily = FontFamily(
    Font(R.font.host_grotesk_regular, FontWeight.Normal),
    Font(R.font.host_grotesk_medium, FontWeight.Medium),
    Font(R.font.host_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.host_grotesk_bold, FontWeight.Bold)
)

object CoconutColors {
    // Warm Coconut Brand Palette
    val Espresso = Color(0xFF4A2800)
    val HuskBronze = Color(0xFF7A4A22)
    val Caramel = Color(0xFFA26C3D)
    val CoconutCream = Color(0xFFF7EFE6)
    val CoconutFlesh = Color(0xFFFAF7F2)
    val SlateTint = Color(0xFF8C7E72)

    // Status colors
    val ProtectedGreen = Color(0xFF2E7D32)
    val AwaitingAmber = Color(0xFFE65100)
    val SnappedRed = Color(0xFFC62828)
    val Error = Color(0xFFBA1A1A)

    // Glass surfaces
    val GlassLightFill = Color(0xF2FFFFFF)
    val GlassLightBorder = Color(0x33A26C3D)
    val GlassDarkFill = Color(0xE6241D17)
    val GlassDarkBorder = Color(0x33DDB892)
}

val CoconutTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

private val LightColorScheme = lightColorScheme(
    primary = CoconutColors.Espresso,
    onPrimary = Color.White,
    primaryContainer = CoconutColors.CoconutCream,
    onPrimaryContainer = Color(0xFF2C1600),
    secondary = CoconutColors.HuskBronze,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E5DC),
    onSecondaryContainer = Color(0xFF381E0A),
    tertiary = CoconutColors.Caramel,
    onTertiary = Color.White,
    error = CoconutColors.SnappedRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF201A15),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF201A15),
    surfaceVariant = Color(0xFFF3ECE4),
    onSurfaceVariant = Color(0xFF534940),
    outline = CoconutColors.SlateTint
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE4C3A3),
    onPrimary = Color(0xFF3A2007),
    primaryContainer = Color(0xFF44280E),
    onPrimaryContainer = Color(0xFFFFE0C4),
    secondary = Color(0xFFCCAC91),
    onSecondary = Color(0xFF2E1C0A),
    secondaryContainer = Color(0xFF38271A),
    onSecondaryContainer = Color(0xFFEBD7C7),
    tertiary = Color(0xFFE8BD93),
    onTertiary = Color(0xFF40260B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF14110E),
    onBackground = Color(0xFFEFE7E0),
    surface = Color(0xFF1E1814),
    onSurface = Color(0xFFEFE7E0),
    surfaceVariant = Color(0xFF2C241E),
    onSurfaceVariant = Color(0xFFC7BCB2),
    outline = Color(0xFF877C72)
)

@Composable
fun CoconutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CoconutTypography,
        content = content
    )
}
