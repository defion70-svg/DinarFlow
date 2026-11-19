package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class SemanticColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val tipsGold: Color,
    val tipsGoldContainer: Color,
    val plannedSky: Color,
    val plannedSkyContainer: Color,
    val warning: Color,
)

private val DarkSemanticColors = SemanticColors(
    income = EmeraldPrimary,
    incomeContainer = EmeraldContainerDark,
    expense = CoralExpense,
    expenseContainer = CoralContainerDark,
    tipsGold = GoldAccent,
    tipsGoldContainer = GoldContainerDark,
    plannedSky = SkyPlanned,
    plannedSkyContainer = SkyContainerDark,
    warning = GoldAccent
)

private val LightSemanticColors = SemanticColors(
    income = EmeraldPrimaryDark,
    incomeContainer = EmeraldContainerLight,
    expense = CoralExpenseLight,
    expenseContainer = CoralContainerLight,
    tipsGold = GoldAccentDark,
    tipsGoldContainer = GoldContainerLight,
    plannedSky = Color(0xFF0284C7),
    plannedSkyContainer = SkyContainerLight,
    warning = GoldAccentDark
)

val LocalSemanticColors = staticCompositionLocalOf { DarkSemanticColors }

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF042219),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = GoldAccent,
    onSecondary = Color(0xFF271900),
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = SkyPlanned,
    onTertiary = Color(0xFF032333),
    tertiaryContainer = SkyContainerDark,
    onTertiaryContainer = Color(0xFFBAE6FD),
    error = CoralExpense,
    onError = Color(0xFF3A0712),
    errorContainer = CoralContainerDark,
    onErrorContainer = Color(0xFFFECDD3),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFF263B45),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = GoldAccentDark,
    onSecondary = Color.White,
    secondaryContainer = GoldContainerLight,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    tertiaryContainer = SkyContainerLight,
    onTertiaryContainer = Color(0xFF0C4A6E),
    error = CoralExpenseLight,
    onError = Color.White,
    errorContainer = CoralContainerLight,
    onErrorContainer = Color(0xFF881337),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFFD0DBD7),
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

val DinarFlowShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DinarFlowTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val semanticColors = if (isDark) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DinarFlowTypography,
            shapes = DinarFlowShapes,
            content = content
        )
    }
}
