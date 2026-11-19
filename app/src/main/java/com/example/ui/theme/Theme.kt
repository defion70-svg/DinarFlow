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
    income = IncomeEmerald,
    incomeContainer = IncomeContainerDark,
    expense = CoralExpense,
    expenseContainer = CoralContainerDark,
    tipsGold = GoldAccent,
    tipsGoldContainer = GoldContainerDark,
    plannedSky = SkyPlanned,
    plannedSkyContainer = SkyContainerDark,
    warning = GoldAccent
)

private val LightSemanticColors = SemanticColors(
    income = IncomeEmeraldDark,
    incomeContainer = IncomeContainerLight,
    expense = CoralExpenseLight,
    expenseContainer = CoralContainerLight,
    tipsGold = GoldAccentDark,
    tipsGoldContainer = GoldContainerLight,
    plannedSky = SkyBluePrimaryDark,
    plannedSkyContainer = SkyContainerLight,
    warning = GoldAccentDark
)

val LocalSemanticColors = staticCompositionLocalOf { DarkSemanticColors }

private val DarkColorScheme = darkColorScheme(
    primary = SkyBluePrimary,
    onPrimary = Color(0xFF041C2E),
    primaryContainer = SkyBlueContainerDark,
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = ElectricCobaltAccent,
    onSecondary = Color(0xFF061B38),
    secondaryContainer = ElectricCobaltContainerDark,
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = GoldAccent,
    onTertiary = Color(0xFF271900),
    tertiaryContainer = GoldContainerDark,
    onTertiaryContainer = Color(0xFFFDE68A),
    error = CoralExpense,
    onError = Color(0xFF3A0712),
    errorContainer = CoralContainerDark,
    onErrorContainer = Color(0xFFFFD6DF),
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
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = SkyBluePrimaryDark,
    onPrimary = Color.White,
    primaryContainer = SkyBlueContainerLight,
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = GoldAccentDark,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainerLight,
    onTertiaryContainer = Color(0xFF78350F),
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
    surfaceContainerHighest = Color(0xFFC7D7E8),
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

val DinarFlowShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DinarFlowTheme(
    themeMode: String = "DARK",
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
