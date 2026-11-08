package com.coconut.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.coconut.R

// --- TYPOGRAPHY FAMILIES ---

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

// --- SPACING SYSTEM (8pt / 4pt grid) ---

object LemonSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

// --- SHAPE SYSTEM ---

object LemonShapes {
    val button = RoundedCornerShape(100.dp)       // Full Capsule
    val badge = RoundedCornerShape(100.dp)        // Full Capsule
    val pill = RoundedCornerShape(100.dp)         // Full Capsule
    val card = RoundedCornerShape(22.dp)          // 22dp standard card
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp) // 32dp top bottom sheet
    val iconSquircle = RoundedCornerShape(12.dp)  // 12dp icon squircle
    val input = RoundedCornerShape(100.dp)        // Capsule search / input
    val dialog = RoundedCornerShape(24.dp)        // 24dp center dialog
}

// --- SEMANTIC COLOR TOKENS ---

data class LemonSemanticColors(
    val isDark: Boolean,
    val pageBackground: Color,
    val cardBackground: Color,
    val cardSecondaryBackground: Color,
    val cardElevatedBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textPlaceholder: Color,
    val textAccent: Color,
    val separator: Color,
    val cardBorder: Color,
    val primaryAccent: Color,
    val onPrimaryAccent: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val inactive: Color,
    val inactiveContainer: Color,
    val switchTrackOn: Color,
    val switchTrackOff: Color,
    val switchThumb: Color,
    val navBackground: Color,
    val navBorder: Color
)

val LemonLightSemanticColors = LemonSemanticColors(
    isDark = false,
    pageBackground = Color(0xFFF6F2E6),
    cardBackground = Color(0xFFFFFFFF),
    cardSecondaryBackground = Color(0xFFEFEAD9),
    cardElevatedBackground = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1C1B14),
    textSecondary = Color(0xFF5F5B4E),
    textTertiary = Color(0xFF706B5D),
    textPlaceholder = Color(0xFF8E887A),
    textAccent = Color(0xFF8A6200), // High-contrast amber/ochre for yellow-related text
    separator = Color(0x241C1B14),  // 14% opacity dark separator
    cardBorder = Color(0x181C1B14), // Subtle 10% outline
    primaryAccent = Color(0xFFF3C94F),
    onPrimaryAccent = Color(0xFF1C1B14), // High contrast dark text on yellow
    success = Color(0xFF2F7A3B),
    successContainer = Color(0xFFE8F5E9),
    onSuccessContainer = Color(0xFF1E5327),
    warning = Color(0xFF8A6200),
    warningContainer = Color(0xFFFEF3D6),
    onWarningContainer = Color(0xFF5C4100),
    danger = Color(0xFFC4261C),
    dangerContainer = Color(0xFFFDE8E7),
    onDangerContainer = Color(0xFF7A140D),
    info = Color(0xFF2A6B9C),
    infoContainer = Color(0xFFE3F2FD),
    onInfoContainer = Color(0xFF174363),
    inactive = Color(0xFF706B5D),
    inactiveContainer = Color(0xFFEAE4D5),
    switchTrackOn = Color(0xFF2FA84F),
    switchTrackOff = Color(0xFFD8D3C4),
    switchThumb = Color(0xFFFFFFFF),
    navBackground = Color(0xF2FFFCF4), // 95% opacity warm cream
    navBorder = Color(0x2E1C1B14)
)

val LemonDarkSemanticColors = LemonSemanticColors(
    isDark = true,
    pageBackground = Color(0xFF12130D),
    cardBackground = Color(0xFF1E2118),
    cardSecondaryBackground = Color(0xFF262920),
    cardElevatedBackground = Color(0xFF2B2E24),
    textPrimary = Color(0xFFF7F3E8),
    textSecondary = Color(0xFFBDB6A4),
    textTertiary = Color(0xFF8E887A),
    textPlaceholder = Color(0xFF706B5D),
    textAccent = Color(0xFFF4C84A),
    separator = Color(0x1FFFFFFF),  // 12% opacity white separator
    cardBorder = Color(0x24FFFFFF), // Subtle dark outline
    primaryAccent = Color(0xFFF4C84A),
    onPrimaryAccent = Color(0xFF1C1B14), // Dark ink on yellow in dark mode too!
    success = Color(0xFF7ED492),
    successContainer = Color(0xFF1B3B22),
    onSuccessContainer = Color(0xFFC7F1CF),
    warning = Color(0xFFF5BF48),
    warningContainer = Color(0xFF3D2E0B),
    onWarningContainer = Color(0xFFFDE197),
    danger = Color(0xFFFF8A80),
    dangerContainer = Color(0xFF441B18),
    onDangerContainer = Color(0xFFFFDAD6),
    info = Color(0xFF86BEEA),
    infoContainer = Color(0xFF162F44),
    onInfoContainer = Color(0xFFCBE6FC),
    inactive = Color(0xFF8E887A),
    inactiveContainer = Color(0xFF262820),
    switchTrackOn = Color(0xFF30B955),
    switchTrackOff = Color(0xFF3A3B33),
    switchThumb = Color(0xFFE0DDD5),
    navBackground = Color(0xF21E2118), // 95% opacity warm charcoal
    navBorder = Color(0x33FFFFFF)
)

// Legacy alias bridge
object LemonColors {
    val BackgroundLight = LemonLightSemanticColors.pageBackground
    val SurfaceLight = LemonLightSemanticColors.cardBackground
    val SurfaceSecondaryLight = LemonLightSemanticColors.cardSecondaryBackground
    val PrimaryYellowLight = LemonLightSemanticColors.primaryAccent
    val PressedYellowLight = Color(0xFFDFAF2E)
    val SoftYellowLight = Color(0xFFFFF1BF)
    val PrimaryGreenLight = LemonLightSemanticColors.success
    val SoftGreenLight = LemonLightSemanticColors.successContainer
    val TextPrimaryLight = LemonLightSemanticColors.textPrimary
    val TextSecondaryLight = LemonLightSemanticColors.textSecondary
    val BorderLight = Color(0xFFEAE4D5)
    val ErrorLight = LemonLightSemanticColors.danger
    val WarningLight = LemonLightSemanticColors.warning

    val BackgroundDark = LemonDarkSemanticColors.pageBackground
    val SurfaceDark = LemonDarkSemanticColors.cardBackground
    val SurfaceElevatedDark = LemonDarkSemanticColors.cardElevatedBackground
    val PrimaryYellowDark = LemonDarkSemanticColors.primaryAccent
    val PressedYellowDark = Color(0xFFFFD968)
    val SoftYellowDark = Color(0xFF40391F)
    val PrimaryGreenDark = LemonDarkSemanticColors.success
    val SoftGreenDark = LemonDarkSemanticColors.successContainer
    val TextPrimaryDark = LemonDarkSemanticColors.textPrimary
    val TextSecondaryDark = LemonDarkSemanticColors.textSecondary
    val BorderDark = Color(0xFF373A2E)
    val ErrorDark = LemonDarkSemanticColors.danger
    val WarningDark = LemonDarkSemanticColors.warning

    val GlassLightFill = LemonLightSemanticColors.navBackground
    val GlassLightBorder = LemonLightSemanticColors.navBorder
    val GlassDarkFill = LemonDarkSemanticColors.navBackground
    val GlassDarkBorder = LemonDarkSemanticColors.navBorder
}

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

// --- FULL MATERIAL 3 COLOR SCHEMES (Zero Default Lavender Leaks) ---

private val LemonLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFF3C94F),
    onPrimary = Color(0xFF1C1B14),
    primaryContainer = Color(0xFFFFF1BF),
    onPrimaryContainer = Color(0xFF1C1B14),
    secondary = Color(0xFF2F7A3B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = Color(0xFF1E5327),
    tertiary = Color(0xFF8A6200),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3D6),
    onTertiaryContainer = Color(0xFF5C4100),
    background = Color(0xFFF6F2E6),
    onBackground = Color(0xFF1C1B14),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B14),
    surfaceVariant = Color(0xFFEFEAD9),
    onSurfaceVariant = Color(0xFF5F5B4E),
    surfaceDim = Color(0xFFECE7DA),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F2E6),
    surfaceContainer = Color(0xFFEFEAD9),
    surfaceContainerHigh = Color(0xFFEAE4D3),
    surfaceContainerHighest = Color(0xFFE2DCB9),
    outline = Color(0x2E1C1B14),
    outlineVariant = Color(0x181C1B14),
    error = Color(0xFFC4261C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE8E7),
    onErrorContainer = Color(0xFF7A140D),
    inverseSurface = Color(0xFF1E2118),
    inverseOnSurface = Color(0xFFF7F3E8),
    inversePrimary = Color(0xFFF4C84A),
    scrim = Color.Black
)

private val LemonDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFF4C84A),
    onPrimary = Color(0xFF1C1B14),
    primaryContainer = Color(0xFF42340E),
    onPrimaryContainer = Color(0xFFF7F3E8),
    secondary = Color(0xFF7ED492),
    onSecondary = Color(0xFF12130D),
    secondaryContainer = Color(0xFF1B3B22),
    onSecondaryContainer = Color(0xFFC7F1CF),
    tertiary = Color(0xFFF5BF48),
    onTertiary = Color(0xFF12130D),
    tertiaryContainer = Color(0xFF3D2E0B),
    onTertiaryContainer = Color(0xFFFDE197),
    background = Color(0xFF12130D),
    onBackground = Color(0xFFF7F3E8),
    surface = Color(0xFF1E2118),
    onSurface = Color(0xFFF7F3E8),
    surfaceVariant = Color(0xFF262920),
    onSurfaceVariant = Color(0xFFBDB6A4),
    surfaceDim = Color(0xFF12130D),
    surfaceBright = Color(0xFF2B2E24),
    surfaceContainerLowest = Color(0xFF0D0E09),
    surfaceContainerLow = Color(0xFF161811),
    surfaceContainer = Color(0xFF1E2118),
    surfaceContainerHigh = Color(0xFF262920),
    surfaceContainerHighest = Color(0xFF2F3228),
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1FFFFFFF),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF441B18),
    errorContainer = Color(0xFF441B18),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFF6F2E6),
    inverseOnSurface = Color(0xFF1C1B14),
    inversePrimary = Color(0xFF8A6200),
    scrim = Color.Black
)

// --- PROPORTIONAL TYPOGRAPHY SCALE ---

val LemonTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp
    ),
    displaySmall = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = UrbanistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

val CoconutTypography = LemonTypography

// --- COMPOSITION LOCALS ---

val LocalLemonColors = staticCompositionLocalOf<LemonSemanticColors> {
    error("No LemonSemanticColors provided")
}

val LocalIsDarkTheme = staticCompositionLocalOf<Boolean> {
    false
}

object LemonTheme {
    val colors: LemonSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLemonColors.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsDarkTheme.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val spacing: LemonSpacing
        get() = LemonSpacing

    val shapes: LemonShapes
        get() = LemonShapes
}

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

    val semanticColors = if (useDarkTheme) LemonDarkSemanticColors else LemonLightSemanticColors
    val colorScheme = if (useDarkTheme) LemonDarkColorScheme else LemonLightColorScheme

    // Synchronize system status and navigation bar icons with authoritative in-app theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !useDarkTheme
                insetsController.isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalLemonColors provides semanticColors,
        LocalIsDarkTheme provides useDarkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LemonTypography,
            content = content
        )
    }
}

@Composable
fun CoconutTheme(
    content: @Composable () -> Unit
) = LemonTheme(content)
