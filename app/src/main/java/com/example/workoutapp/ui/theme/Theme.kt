package com.example.workoutapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Every Material 3 role is defined on purpose.
 *
 * The previous schemes set only nine, so the remaining twelve silently fell back
 * to the Material defaults, which are purple tinted. Cards, the FAB, selected
 * chips and the profile screen therefore rendered purple inside an otherwise
 * green and black app.
 */
private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = DarkGrey,
    // Dark, desaturated container. The saturated green produced white body text
    // at roughly 1.4:1 on the "Ready to train" and streak cards.
    primaryContainer = Color(0xFF16301C),
    onPrimaryContainer = Color(0xFFB9EFC8),
    inversePrimary = NeonGreenDark,

    // Secondary is a muted sage, deliberately not the brand green. It carries
    // low emphasis surfaces and secondary fills, so reserving the bright green
    // for primary actions is what stops the app reading as one green wash.
    secondary = Color(0xFF9FD4AE),
    onSecondary = Color(0xFF06210F),
    secondaryContainer = DarkSurfaceContainerHigh,
    onSecondaryContainer = TextWhite,

    // Tertiary is the contrast accent: warm amber, used for highlights and
    // attention states such as a personal record or a live sensor reading.
    tertiary = AccentAmber,
    onTertiary = Color(0xFF241A00),
    tertiaryContainer = Color(0xFF3D2E00),
    onTertiaryContainer = Color(0xFFFFDF9E),

    error = AccentRose,
    onError = DarkGrey,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = DarkGrey,
    onBackground = TextWhite,
    surface = SurfaceGrey,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC9CCC9),
    surfaceTint = NeonGreen,
    inverseSurface = Color(0xFFEDEDED),
    inverseOnSurface = Color(0xFF1B1B1B),

    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFF37373C),

    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = Color(0xFF000000)
)

/**
 * Light mode uses a much darker green as the primary. NeonGreen on a near white
 * background measured 1.56:1, and the completed exercise card measured 1.15:1,
 * both of which fail WCAG AA.
 */
private val LightColorScheme = lightColorScheme(
    primary = NeonGreenDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8F5D8),
    onPrimaryContainer = Color(0xFF00210F),
    inversePrimary = NeonGreen,

    secondary = NeonGreenDark,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceContainer,
    onSecondaryContainer = TextBlack,

    tertiary = AccentBlueLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCDE9F8),
    onTertiaryContainer = Color(0xFF00344A),

    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,

    background = LightBackground,
    onBackground = TextBlack,
    surface = LightSurface,
    onSurface = TextBlack,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF414942),
    surfaceTint = NeonGreenDark,
    inverseSurface = Color(0xFF2E312E),
    inverseOnSurface = Color(0xFFF0F1EC),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFBF7),
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = Color(0xFFEBEDE9),
    surfaceContainerHighest = Color(0xFFE5E8E3),

    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = Color(0xFF000000)
)

@Composable
fun WorkoutAppTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // The previous code cast view.context straight to Activity, which
            // throws ClassCastException under a ContextThemeWrapper or a dialog
            // host. Look the activity up safely instead.
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            // statusBarColor is a no-op from API 35, where edge-to-edge is
            // enforced, so it is only set below that.
            if (Build.VERSION.SDK_INT < 35) {
                @Suppress("DEPRECATION")
                window.statusBarColor = colorScheme.background.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
