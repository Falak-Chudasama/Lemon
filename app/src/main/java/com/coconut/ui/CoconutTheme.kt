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

val HostGroteskFontFamily = FontFamily(
    Font(R.font.host_grotesk_regular, FontWeight.Normal),
    Font(R.font.host_grotesk_medium, FontWeight.Medium),
    Font(R.font.host_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.host_grotesk_bold, FontWeight.Bold)
)

val UrbanistFontFamily = FontFamily(
    Font(R.font.urbanist_regular, FontWeight.Normal),
    Font(R.font.urbanist_medium, FontWeight.Medium),
    Font(R.font.urbanist_semibold, FontWeight.SemiBold),
    Font(R.font.urbanist_bold, FontWeight.Bold)
)

object CoconutColors {
    // Coconut brand palette from app-icon & logo
    val Espresso = Color(0xFF603913)
    val HuskBronze = Color(0xFF8B5E3C)
    val Caramel = Color(0xFFA97C50)
    val CoconutCream = Color(0xFFF3E5D8)
    val CoconutFlesh = Color(0xFFF5F8FA)
    val SlateTint = Color(0xFF99AAB5)

    // Status colors
    val ProtectedGreen = Color(0xFF2E7D32)
    val AwaitingAmber = Color(0xFFED6C02)
    val SnappedRed = Color(0xFFD32F2F)
    val Error = Color(0xFFBA1A1A)
}

val CoconutTypography = Typography(
    // Headings in Google Font: Host Grotesk
    displayLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp
    ),
    displayMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
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
        fontSize = 28.sp,
        lineHeight = 34.sp
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
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),

    // Subheadings, Body, and Labels in Google Font: Urbanist
    titleMedium = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.Medium,
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
    primary = CoconutColors.Espresso,
    onPrimary = Color.White,
    primaryContainer = CoconutColors.CoconutCream,
    onPrimaryContainer = Color(0xFF2C1600),
    secondary = CoconutColors.HuskBronze,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFE4DA),
    onSecondaryContainer = Color(0xFF381E0A),
    tertiary = CoconutColors.Caramel,
    onTertiary = Color.White,
    error = CoconutColors.SnappedRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF1E1A17),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1A17),
    surfaceVariant = Color(0xFFF3ECE4),
    onSurfaceVariant = Color(0xFF5A524A),
    outline = CoconutColors.SlateTint
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFDDB892),
    onPrimary = Color(0xFF3E2207),
    primaryContainer = Color(0xFF4C2F14),
    onPrimaryContainer = Color(0xFFFFE0C4),
    secondary = Color(0xFFBFA084),
    onSecondary = Color(0xFF2E1C0A),
    secondaryContainer = Color(0xFF3D2C1E),
    onSecondaryContainer = Color(0xFFE4D3C3),
    tertiary = Color(0xFFE5B589),
    onTertiary = Color(0xFF422709),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF151311),
    onBackground = Color(0xFFEDE5DF),
    surface = Color(0xFF1E1A17),
    onSurface = Color(0xFFEDE5DF),
    surfaceVariant = Color(0xFF2C2621),
    onSurfaceVariant = Color(0xFFC7BCB3),
    outline = Color(0xFF7A868E)
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
