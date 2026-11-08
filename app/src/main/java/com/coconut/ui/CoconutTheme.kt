package com.coconut.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    // Light palette tokens
    val IvoryBackground = Color(0xFFF7F3ED)
    val PulpSurface = Color(0xFFFFFCF7)
    val SurfaceVariantLight = Color(0xFFF0E9E0)
    val SurfaceRaisedLight = Color(0xFFFFFDFB)
    val CocoaPrimary = Color(0xFF704426)
    val EspressoDark = Color(0xFF4A2E1F)
    val ToastedBrown = Color(0xFF8B5A35)
    val CaramelSecondary = Color(0xFFB77A4A)
    val CopperAccent = Color(0xFFC58A59)
    val TextPrimaryLight = Color(0xFF211A16)
    val TextSecondaryLight = Color(0xFF6F6258)
    val OutlineLight = Color(0xFFD7C9BA)
    val SuccessLight = Color(0xFF18794E)
    val ErrorLight = Color(0xFFB3261E)
    val WarningLight = Color(0xFF8A5A00)

    // Dark palette tokens
    val DarkCharcoalBackground = Color(0xFF15120F)
    val DarkSurface = Color(0xFF211A16)
    val DarkSurfaceVariant = Color(0xFF2A211B)
    val DarkGlassSurface = Color(0xFF2A211B)
    val CaramelDarkAccent = Color(0xFFC58A59)
    val SecondaryBrownDark = Color(0xFFB77A4A)
    val DeepCaramel = Color(0xFF7A4B2A)
    val TextPrimaryDark = Color(0xFFF6EFE6)
    val TextSecondaryDark = Color(0xFFC2B4A5)
    val OutlineDark = Color(0xFF514237)
    val SuccessDark = Color(0xFF56C596)
    val ErrorDark = Color(0xFFFF8A80)
    val WarningDark = Color(0xFFE6B566)

    // Floating glass materials
    val GlassLightFill = Color(0xDCFFFCF7) // 86% opaque
    val GlassLightBorder = Color(0x33D7C9BA)
    val GlassDarkFill = Color(0xDC211A16) // 86% opaque
    val GlassDarkBorder = Color(0x33514237)

    // Backward compatibility aliases
    val Caramel = CaramelSecondary
    val SnappedRed = ErrorLight
    val ProtectedGreen = SuccessLight
    val AwaitingAmber = WarningLight
}

val CoconutTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
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

private val LightColorScheme = lightColorScheme(
    primary = CoconutColors.CocoaPrimary,
    onPrimary = Color.White,
    primaryContainer = CoconutColors.SurfaceVariantLight,
    onPrimaryContainer = CoconutColors.TextPrimaryLight,
    secondary = CoconutColors.CaramelSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9DED1),
    onSecondaryContainer = CoconutColors.TextPrimaryLight,
    tertiary = CoconutColors.CopperAccent,
    onTertiary = Color.White,
    background = CoconutColors.IvoryBackground,
    onBackground = CoconutColors.TextPrimaryLight,
    surface = CoconutColors.PulpSurface,
    onSurface = CoconutColors.TextPrimaryLight,
    surfaceVariant = CoconutColors.SurfaceVariantLight,
    onSurfaceVariant = CoconutColors.TextSecondaryLight,
    outline = CoconutColors.OutlineLight,
    error = CoconutColors.ErrorLight,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = CoconutColors.CaramelDarkAccent,
    onPrimary = CoconutColors.TextPrimaryLight,
    primaryContainer = CoconutColors.DarkSurfaceVariant,
    onPrimaryContainer = CoconutColors.TextPrimaryDark,
    secondary = CoconutColors.SecondaryBrownDark,
    onSecondary = CoconutColors.TextPrimaryLight,
    secondaryContainer = Color(0xFF352920),
    onSecondaryContainer = CoconutColors.TextPrimaryDark,
    tertiary = CoconutColors.DeepCaramel,
    onTertiary = Color.White,
    background = CoconutColors.DarkCharcoalBackground,
    onBackground = CoconutColors.TextPrimaryDark,
    surface = CoconutColors.DarkSurface,
    onSurface = CoconutColors.TextPrimaryDark,
    surfaceVariant = CoconutColors.DarkSurfaceVariant,
    onSurfaceVariant = CoconutColors.TextSecondaryDark,
    outline = CoconutColors.OutlineDark,
    error = CoconutColors.ErrorDark,
    onError = Color(0xFF690005)
)

@Composable
fun CoconutTheme(
    content: @Composable () -> Unit
) {
    val themeMode by ThemePreferences.themeModeFlow.collectAsState()
    val systemDark = isSystemInDarkTheme()

    val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CoconutTypography,
        content = content
    )
}
