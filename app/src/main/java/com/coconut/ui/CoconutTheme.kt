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

// Host Grotesk: Display titles, major headings, and primary emphasis
val HostGroteskFontFamily = FontFamily(
    Font(R.font.host_grotesk_regular, FontWeight.Normal),
    Font(R.font.host_grotesk_medium, FontWeight.Medium),
    Font(R.font.host_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.host_grotesk_bold, FontWeight.Bold)
)

// Urbanist: Body text, labels, inputs, buttons, and secondary captions
val UrbanistFontFamily = FontFamily(
    Font(R.font.urbanist_regular, FontWeight.Normal),
    Font(R.font.urbanist_medium, FontWeight.Medium),
    Font(R.font.urbanist_semibold, FontWeight.SemiBold),
    Font(R.font.urbanist_bold, FontWeight.Bold)
)

object LemonColors {
    // Light palette tokens
    val BackgroundLight = Color(0xFFFFFCF4)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceSecondaryLight = Color(0xFFF7F1E2)
    val PrimaryYellowLight = Color(0xFFF3C94F)
    val PressedYellowLight = Color(0xFFDFAF2E)
    val SoftYellowLight = Color(0xFFFFF1BF)
    val PrimaryGreenLight = Color(0xFF668F55)
    val SoftGreenLight = Color(0xFFEAF1E4)
    val TextPrimaryLight = Color(0xFF252319)
    val TextSecondaryLight = Color(0xFF777366)
    val BorderLight = Color(0xFFEAE4D5)
    val ErrorLight = Color(0xFFBA1A1A)
    val WarningLight = Color(0xFF8A5A00)

    // Dark palette tokens
    val BackgroundDark = Color(0xFF15160F)
    val SurfaceDark = Color(0xFF1E2118)
    val SurfaceElevatedDark = Color(0xFF282C20)
    val PrimaryYellowDark = Color(0xFFF4C84A)
    val PressedYellowDark = Color(0xFFFFD968)
    val SoftYellowDark = Color(0xFF40391F)
    val PrimaryGreenDark = Color(0xFFA6C58A)
    val SoftGreenDark = Color(0xFF2B3A28)
    val TextPrimaryDark = Color(0xFFF7F3E8)
    val TextSecondaryDark = Color(0xFFB7B09E)
    val BorderDark = Color(0xFF373A2E)
    val ErrorDark = Color(0xFFFFB4AB)
    val WarningDark = Color(0xFFF5BF48)

    // iOS-inspired glass / floating nav tokens
    val GlassLightFill = Color(0xF2FFFCF4) // 95% opacity warm cream
    val GlassLightBorder = Color(0x33EAE4D5)
    val GlassDarkFill = Color(0xF21E2118) // 95% opacity dark surface
    val GlassDarkBorder = Color(0x33373A2E)
}

// Backward-compatible alias referencing the canonical Lemon color system
object CoconutColors {
    val IvoryBackground = LemonColors.BackgroundLight
    val PulpSurface = LemonColors.SurfaceLight
    val SurfaceVariantLight = LemonColors.SurfaceSecondaryLight
    val SurfaceRaisedLight = LemonColors.SurfaceLight
    val CocoaPrimary = LemonColors.PrimaryYellowLight
    val CocoaDark = LemonColors.PrimaryYellowDark
    val TextPrimaryLight = LemonColors.TextPrimaryLight
    val TextSecondaryLight = LemonColors.TextSecondaryLight
    val OutlineLight = LemonColors.BorderLight
    val SuccessLight = LemonColors.PrimaryGreenLight
    val ErrorLight = LemonColors.ErrorLight
    val WarningLight = LemonColors.WarningLight

    val DarkCharcoalBackground = LemonColors.BackgroundDark
    val DarkSurface = LemonColors.SurfaceDark
    val DarkSurfaceVariant = LemonColors.SurfaceElevatedDark
    val DarkGlassSurface = LemonColors.SurfaceElevatedDark
    val CaramelDarkAccent = LemonColors.PrimaryYellowDark
    val SecondaryBrownDark = LemonColors.PrimaryGreenDark
    val DeepCaramel = LemonColors.SoftYellowDark
    val TextPrimaryDark = LemonColors.TextPrimaryDark
    val TextSecondaryDark = LemonColors.TextSecondaryDark
    val OutlineDark = LemonColors.BorderDark
    val SuccessDark = LemonColors.PrimaryGreenDark
    val ErrorDark = LemonColors.ErrorDark
    val WarningDark = LemonColors.WarningDark

    val GlassLightFill = LemonColors.GlassLightFill
    val GlassLightBorder = LemonColors.GlassLightBorder
    val GlassDarkFill = LemonColors.GlassDarkFill
    val GlassDarkBorder = LemonColors.GlassDarkBorder

    val Caramel = LemonColors.PrimaryYellowLight
    val SnappedRed = LemonColors.ErrorLight
    val ProtectedGreen = LemonColors.PrimaryGreenLight
    val AwaitingAmber = LemonColors.WarningLight
}

val LemonTypography = Typography(
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

val CoconutTypography = LemonTypography

private val LemonLightColorScheme = lightColorScheme(
    primary = LemonColors.PrimaryYellowLight,
    onPrimary = LemonColors.TextPrimaryLight, // Dark text on bright yellow buttons!
    primaryContainer = LemonColors.SoftYellowLight,
    onPrimaryContainer = LemonColors.TextPrimaryLight,
    secondary = LemonColors.PrimaryGreenLight,
    onSecondary = Color.White,
    secondaryContainer = LemonColors.SoftGreenLight,
    onSecondaryContainer = LemonColors.TextPrimaryLight,
    tertiary = LemonColors.PrimaryGreenLight,
    onTertiary = Color.White,
    background = LemonColors.BackgroundLight,
    onBackground = LemonColors.TextPrimaryLight,
    surface = LemonColors.SurfaceLight,
    onSurface = LemonColors.TextPrimaryLight,
    surfaceVariant = LemonColors.SurfaceSecondaryLight,
    onSurfaceVariant = LemonColors.TextSecondaryLight,
    outline = LemonColors.BorderLight,
    error = LemonColors.ErrorLight,
    onError = Color.White
)

private val LemonDarkColorScheme = darkColorScheme(
    primary = LemonColors.PrimaryYellowDark,
    onPrimary = LemonColors.TextPrimaryLight, // Dark text on bright yellow buttons in dark mode too!
    primaryContainer = LemonColors.SoftYellowDark,
    onPrimaryContainer = LemonColors.TextPrimaryDark,
    secondary = LemonColors.PrimaryGreenDark,
    onSecondary = LemonColors.BackgroundDark,
    secondaryContainer = LemonColors.SoftGreenDark,
    onSecondaryContainer = LemonColors.TextPrimaryDark,
    tertiary = LemonColors.PrimaryGreenDark,
    onTertiary = LemonColors.BackgroundDark,
    background = LemonColors.BackgroundDark,
    onBackground = LemonColors.TextPrimaryDark,
    surface = LemonColors.SurfaceDark,
    onSurface = LemonColors.TextPrimaryDark,
    surfaceVariant = LemonColors.SurfaceElevatedDark,
    onSurfaceVariant = LemonColors.TextSecondaryDark,
    outline = LemonColors.BorderDark,
    error = LemonColors.ErrorDark,
    onError = Color(0xFF690005)
)

@Composable
fun LemonTheme(
    content: @Composable () -> Unit
) {
    val themeMode by ThemePreferences.themeModeFlow.collectAsState()
    val systemDark = isSystemInDarkTheme()

    val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) LemonDarkColorScheme else LemonLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LemonTypography,
        content = content
    )
}

@Composable
fun CoconutTheme(
    content: @Composable () -> Unit
) = LemonTheme(content)
