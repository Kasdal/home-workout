package com.example.workoutapp.ui.theme

import androidx.compose.ui.graphics.Color

// Brand
val NeonGreen = Color(0xFF00E676)
val NeonGreenDark = Color(0xFF00B85C)
val NeonGreenLight = Color(0xFF6BFFB0)

// Dark neutrals. The previous scheme set only background and surface, so every
// other role fell back to the Material 3 defaults, which are purple tinted. That
// is why cards, the FAB and selected chips rendered purple against a green app.
val DarkGrey = Color(0xFF121212)
val SurfaceGrey = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2A2A2E)
val DarkSurfaceContainer = Color(0xFF232327)
val DarkSurfaceContainerHigh = Color(0xFF2D2D32)
val DarkOutline = Color(0xFF6E6E76)
val DarkOutlineVariant = Color(0xFF3A3A40)

// Light neutrals. NeonGreen on a near white background is 1.56:1, which fails
// WCAG AA, so light mode uses a much darker green as the primary.
val LightBackground = Color(0xFFFBFBF9)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE6E8E4)
val LightSurfaceContainer = Color(0xFFF1F3EF)
val LightOutline = Color(0xFF747A75)
val LightOutlineVariant = Color(0xFFC4C9C4)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val TextWhite = Color(0xFFFFFFFF)
val TextBlack = Color(0xFF1C1B1F)
val TextGrey = Color(0xFFB0B0B0)

// Semantic accents for analytics. Previously three hard coded Material colours
// (blue, pink, orange) were used side by side with no relationship to the brand.
val AccentBlue = Color(0xFF4FC3F7)
val AccentAmber = Color(0xFFFFC107)
val AccentRose = Color(0xFFFF8A80)
val AccentBlueLight = Color(0xFF0277BD)
val AccentAmberLight = Color(0xFFB26A00)
val AccentRoseLight = Color(0xFFB3261E)

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)
