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

// --- AUTHORITATIVE FRUIT FAMILY TYPOGRAPHY FAMILIES ---

val UnboundedFontFamily = FontFamily(
    Font(R.font.unbounded_regular, FontWeight.Normal),
    Font(R.font.unbounded_medium, FontWeight.Medium),
    Font(R.font.unbounded_semibold, FontWeight.SemiBold),
    Font(R.font.unbounded_bold, FontWeight.Bold),
    Font(R.font.unbounded_extrabold, FontWeight.ExtraBold)
)

val HostGroteskFontFamily = FontFamily(
    Font(R.font.host_grotesk_regular, FontWeight.Normal),
    Font(R.font.host_grotesk_medium, FontWeight.Medium),
    Font(R.font.host_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.host_grotesk_bold, FontWeight.Bold)
)

// Backward compatibility alias for legacy references
val UrbanistFontFamily = HostGroteskFontFamily

// --- SPACING SYSTEM (Consistent 4pt / 8pt grid) ---

object LemonSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

// --- SHAPE SYSTEM (Disciplined Fruit family radii scale) ---

object LemonShapes {
    val button = RoundedCornerShape(100.dp)       // Full Capsule (Buttons, Pills, Tabs)
    val badge = RoundedCornerShape(100.dp)        // Full Capsule
    val microBadge = RoundedCornerShape(6.dp)     // 6dp micro badge
    val pill = RoundedCornerShape(100.dp)         // Full Capsule
    val card = RoundedCornerShape(16.dp)          // 16dp standard card (rounded-2xl)
    val cardLarge = RoundedCornerShape(24.dp)     // 24dp large container (rounded-3xl)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) // 24dp top bottom sheet
    val iconSquircle = RoundedCornerShape(12.dp)  // 12dp icon squircle
    val input = RoundedCornerShape(12.dp)         // 12dp rounded-xl input
    val dialog = RoundedCornerShape(24.dp)        // 24dp center dialog
}

// --- SEMANTIC COLOR TOKENS ---

data class LemonSemanticColors(
    val isDark: Boolean,
    val pageBackground: Color,
    val cardBackground: Color,
    val cardSecondaryBackground: Color,
    val cardElevatedBackground: Color,
    val surfaceWell: Color = cardSecondaryBackground,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textPlaceholder: Color,
    val textAccent: Color,
    val separator: Color,
    val cardBorder: Color,
    val primaryAccent: Color,
    val onPrimaryAccent: Color,
    val brandYellow: Color = primaryAccent,
    val brandAmber: Color = textAccent,
    val brandGreen: Color = Color(0xFF2FA84F),
    val focusRing: Color = primaryAccent,
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
    pageBackground = Color(0xFFF8F9F5),             // Authoritative warm paper canvas
    cardBackground = Color(0xFFFFFFFF),             // Solid white card surface
    cardSecondaryBackground = Color(0xFFF5F5F4),    // Stone-100 secondary surface
    cardElevatedBackground = Color(0xFFFFFFFF),     // Elevated card
    surfaceWell = Color(0xFFFAFAF9),                // Stone-50 recessed metric well
    textPrimary = Color(0xFF1C1917),                // Stone-900 primary ink
    textSecondary = Color(0xFF44403C),              // Stone-700 secondary text
    textTertiary = Color(0xFF57534E),               // Stone-600 muted text
    textPlaceholder = Color(0xFFA8A29E),            // Stone-400 inactive/placeholder
    textAccent = Color(0xFF8A6200),                 // High-contrast ochre/amber for light surfaces
    separator = Color(0x0F000000),                  // 6% black separator line
    cardBorder = Color(0x0F000000),                 // 6% black 1px card boundary
    primaryAccent = Color(0xFFFBDB7B),              // Official Lemon logo yellow
    onPrimaryAccent = Color(0xFF1C1B14),            // Deep dark ink on yellow for pristine contrast
    brandYellow = Color(0xFFFBDB7B),
    brandAmber = Color(0xFFD97706),
    brandGreen = Color(0xFF2FA84F),
    focusRing = Color(0xFFFBDB7B),
    success = Color(0xFF1E8A38),                    // Lemon leaf green rind
    successContainer = Color(0xFFF0FDF2),
    onSuccessContainer = Color(0xFF086E18),
    warning = Color(0xFFD97706),                    // Amber warning
    warningContainer = Color(0xFFFFFBEB),
    onWarningContainer = Color(0xFF92400E),
    danger = Color(0xFFDC2626),                     // Coral / Red
    dangerContainer = Color(0xFFFFF1F2),
    onDangerContainer = Color(0xFF9F1239),
    info = Color(0xFF2563EB),                       // Sky blue info
    infoContainer = Color(0xFFEFF6FF),
    onInfoContainer = Color(0xFF1E40AF),
    inactive = Color(0xFF78716C),
    inactiveContainer = Color(0xFFF5F5F4),
    switchTrackOn = Color(0xFF2FA84F),
    switchTrackOff = Color(0xFFE7E5E4),
    switchThumb = Color(0xFFFFFFFF),
    navBackground = Color(0xF2FFFFFF),              // Frosted floating capsule
    navBorder = Color(0x12000000)
)

val LemonDarkSemanticColors = LemonSemanticColors(
    isDark = true,
    pageBackground = Color(0xFF121310),             // Authoritative obsidian warm-black canvas
    cardBackground = Color(0xFF181915),             // Solid dark card surface
    cardSecondaryBackground = Color(0xFF1C1D18),    // Elevated dark stone surface
    cardElevatedBackground = Color(0xFF20211C),     // Higher elevation
    surfaceWell = Color(0xFF141512),                // Recessed deep dark well
    textPrimary = Color(0xFFFAFAF9),                // Stone-50 primary text
    textSecondary = Color(0xFFD6D3D1),              // Stone-300 secondary text
    textTertiary = Color(0xFFA8A29E),               // Stone-400 muted text
    textPlaceholder = Color(0xFF78716C),            // Stone-500 inactive/placeholder
    textAccent = Color(0xFFFBDB7B),                 // Luminous Lemon yellow in dark mode
    separator = Color(0x14FFFFFF),                  // 8% white separator line
    cardBorder = Color(0x14FFFFFF),                 // 8% white 1px card boundary
    primaryAccent = Color(0xFFFBDB7B),              // Lemon yellow
    onPrimaryAccent = Color(0xFF1C1B14),            // High-contrast dark ink on yellow
    brandYellow = Color(0xFFFBDB7B),
    brandAmber = Color(0xFFFBBF24),
    brandGreen = Color(0xFF4ADE80),
    focusRing = Color(0xFFFBDB7B),
    success = Color(0xFF4ADE80),                    // Leaf green
    successContainer = Color(0xFF0F3818),
    onSuccessContainer = Color(0xFFDCFCE7),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF382305),
    onWarningContainer = Color(0xFFFEF3C7),
    danger = Color(0xFFF87171),
    dangerContainer = Color(0xFF3D1314),
    onDangerContainer = Color(0xFFFEE2E2),
    info = Color(0xFF60A5FA),
    infoContainer = Color(0xFF172554),
    onInfoContainer = Color(0xFFDBEAFE),
    inactive = Color(0xFF78716C),
    inactiveContainer = Color(0xFF20211C),
    switchTrackOn = Color(0xFF30B955),
    switchTrackOff = Color(0xFF292524),
    switchThumb = Color(0xFFE0DDD5),
    navBackground = Color(0xF0181915),              // Translucent frosted obsidian capsule
    navBorder = Color(0x1AFFFFFF)
)

// Legacy alias bridge
object LemonColors {
    val BackgroundLight = LemonLightSemanticColors.pageBackground
    val SurfaceLight = LemonLightSemanticColors.cardBackground
    val SurfaceSecondaryLight = LemonLightSemanticColors.cardSecondaryBackground
    val PrimaryYellowLight = LemonLightSemanticColors.primaryAccent
    val PressedYellowLight = Color(0xFFE5C564)
    val SoftYellowLight = Color(0xFFFFF8D1)
    val PrimaryGreenLight = LemonLightSemanticColors.success
    val SoftGreenLight = LemonLightSemanticColors.successContainer
    val TextPrimaryLight = LemonLightSemanticColors.textPrimary
    val TextSecondaryLight = LemonLightSemanticColors.textSecondary
    val BorderLight = Color(0xFFE7E5E4)
    val ErrorLight = LemonLightSemanticColors.danger
    val WarningLight = LemonLightSemanticColors.warning

    val BackgroundDark = LemonDarkSemanticColors.pageBackground
    val SurfaceDark = LemonDarkSemanticColors.cardBackground
    val SurfaceElevatedDark = LemonDarkSemanticColors.cardElevatedBackground
    val PrimaryYellowDark = LemonDarkSemanticColors.primaryAccent
    val PressedYellowDark = Color(0xFFFFE082)
    val SoftYellowDark = Color(0xFF332A15)
    val PrimaryGreenDark = LemonDarkSemanticColors.success
    val SoftGreenDark = LemonDarkSemanticColors.successContainer
    val TextPrimaryDark = LemonDarkSemanticColors.textPrimary
    val TextSecondaryDark = LemonDarkSemanticColors.textSecondary
    val BorderDark = Color(0xFF292524)
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

// --- FULL MATERIAL 3 COLOR SCHEMES ---

private val LemonLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFFBDB7B),
    onPrimary = Color(0xFF1C1B14),
    primaryContainer = Color(0xFFFFF8D1),
    onPrimaryContainer = Color(0xFF1C1B14),
    secondary = Color(0xFF1E8A38),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0FDF2),
    onSecondaryContainer = Color(0xFF086E18),
    tertiary = Color(0xFF8A6200),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFFBEB),
    onTertiaryContainer = Color(0xFF92400E),
    background = Color(0xFFF8F9F5),
    onBackground = Color(0xFF1C1917),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFF5F5F4),
    onSurfaceVariant = Color(0xFF44403C),
    surfaceDim = Color(0xFFE7E5E4),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAF9),
    surfaceContainer = Color(0xFFF5F5F4),
    surfaceContainerHigh = Color(0xFFE7E5E4),
    surfaceContainerHighest = Color(0xFFD6D3D1),
    outline = Color(0x0F000000),
    outlineVariant = Color(0x08000000),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFF1F2),
    onErrorContainer = Color(0xFF9F1239),
    inverseSurface = Color(0xFF181915),
    inverseOnSurface = Color(0xFFFAFAF9),
    inversePrimary = Color(0xFFFBDB7B),
    scrim = Color.Black
)

private val LemonDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFBDB7B),
    onPrimary = Color(0xFF1C1B14),
    primaryContainer = Color(0xFF332A15),
    onPrimaryContainer = Color(0xFFFAFAF9),
    secondary = Color(0xFF4ADE80),
    onSecondary = Color(0xFF121310),
    secondaryContainer = Color(0xFF0F3818),
    onSecondaryContainer = Color(0xFFDCFCE7),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF121310),
    tertiaryContainer = Color(0xFF382305),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF121310),
    onBackground = Color(0xFFFAFAF9),
    surface = Color(0xFF181915),
    onSurface = Color(0xFFFAFAF9),
    surfaceVariant = Color(0xFF1C1D18),
    onSurfaceVariant = Color(0xFFD6D3D1),
    surfaceDim = Color(0xFF121310),
    surfaceBright = Color(0xFF20211C),
    surfaceContainerLowest = Color(0xFF0C0D0A),
    surfaceContainerLow = Color(0xFF141512),
    surfaceContainer = Color(0xFF181915),
    surfaceContainerHigh = Color(0xFF1C1D18),
    surfaceContainerHighest = Color(0xFF242520),
    outline = Color(0x14FFFFFF),
    outlineVariant = Color(0x0DFFFFFF),
    error = Color(0xFFF87171),
    onError = Color(0xFF3D1314),
    errorContainer = Color(0xFF3D1314),
    onErrorContainer = Color(0xFFFEE2E2),
    inverseSurface = Color(0xFFF8F9F5),
    inverseOnSurface = Color(0xFF1C1917),
    inversePrimary = Color(0xFF8A6200),
    scrim = Color.Black
)

// --- PROPORTIONAL TYPOGRAPHY SCALE (Unbounded for Display/Titles + Host Grotesk for Body/UI) ---

val LemonTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = UnboundedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp
    ),
    displayMedium = TextStyle(
        fontFamily = UnboundedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.6).sp
    ),
    displaySmall = TextStyle(
        fontFamily = UnboundedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = UnboundedFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.15).sp
    ),
    titleMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.1).sp
    ),
    titleSmall = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.15).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp
    ),
    bodySmall = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp
    ),
    labelMedium = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = HostGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.3.sp
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
