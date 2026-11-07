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

// Host Grotesk: Display titles, major headings, and selected emphasis
val HostGroteskFontFamily = FontFamily(
    Font(R.font.host_grotesk_regular, FontWeight.Normal),
    Font(R.font.host_grotesk_medium, FontWeight.Medium),
    Font(R.font.host_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.host_grotesk_bold, FontWeight.Bold)
)

// Urbanist: Normal body text, labels, inputs, buttons, and secondary information
val UrbanistFontFamily = FontFamily(
    Font(R.font.urbanist_regular, FontWeight.Normal),
    Font(R.font.urbanist_medium, FontWeight.Medium),
    Font(R.font.urbanist_semibold, FontWeight.SemiBold),
    Font(R.font.urbanist_bold, FontWeight.Bold)
)

object CoconutColors {
    // Warm Coconut Brand Palette
    val Espresso = Color(0xFF382006)
    val HuskBronze = Color(0xFF6B4019)
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
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
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
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFDDB892),
    onPrimary = Color(0xFF382006),
    primaryContainer = Color(0xFF53381B),
    onPrimaryContainer = Color(0xFFF7EFE6),
    secondary = Color(0xFFC7A88A),
    onSecondary = Color(0xFF2E1D0C),
    secondaryContainer = Color(0xFF45301E),
    onSecondaryContainer = Color(0xFFE8D7C6),
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF00390E),
    background = Color(0xFF16120E),
    onBackground = Color(0xFFEBE0D7),
    surface = Color(0xFF221B15),
    onSurface = Color(0xFFEBE0D7),
    surfaceVariant = Color(0xFF352B23),
    onSurfaceVariant = Color(0xFFD3C3B6),
    outline = Color(0xFF9C8D81),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7A4A22),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF7EFE6),
    onPrimaryContainer = Color(0xFF2E1700),
    secondary = Color(0xFF6E5948),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4E9DF),
    onSecondaryContainer = Color(0xFF27170A),
    tertiary = Color(0xFF2E7D32),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF201B16),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF201B16),
    surfaceVariant = Color(0xFFEFE8E1),
    onSurfaceVariant = Color(0xFF4F453D),
    outline = Color(0xFF81756C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
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
