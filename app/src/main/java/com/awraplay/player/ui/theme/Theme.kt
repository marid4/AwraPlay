package com.awraplay.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AwraBackgroundDark = Color(0xFF101014)
val AwraSurfaceDark = Color(0xFF181820)
val AwraSurfaceVariantDark = Color(0xFF22222E)
val AwraPrimaryPurple = Color(0xFF8855FF)
val AwraSecondaryBlue = Color(0xFF20BFFF)
val AwraAccentPink = Color(0xFFFF4DA6)
val AwraTextPrimaryDark = Color(0xFFFFFFFF)
val AwraTextSecondaryDark = Color(0xFFB8B8C8)

val AwraBackgroundLight = Color(0xFFF6F6FA)
val AwraSurfaceLight = Color(0xFFFFFFFF)
val AwraSurfaceVariantLight = Color(0xFFECECF4)

private val DarkColors = darkColorScheme(
    primary = AwraPrimaryPurple,
    onPrimary = Color.White,
    secondary = AwraSecondaryBlue,
    onSecondary = Color.Black,
    tertiary = AwraAccentPink,
    onTertiary = Color.White,
    background = AwraBackgroundDark,
    onBackground = AwraTextPrimaryDark,
    surface = AwraSurfaceDark,
    onSurface = AwraTextPrimaryDark,
    surfaceVariant = AwraSurfaceVariantDark,
    onSurfaceVariant = AwraTextSecondaryDark
)

private val LightColors = lightColorScheme(
    primary = AwraPrimaryPurple,
    onPrimary = Color.White,
    secondary = Color(0xFF0095D6),
    onSecondary = Color.White,
    tertiary = AwraAccentPink,
    onTertiary = Color.White,
    background = AwraBackgroundLight,
    onBackground = Color(0xFF121218),
    surface = AwraSurfaceLight,
    onSurface = Color(0xFF121218),
    surfaceVariant = AwraSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF555568)
)

private val AwraTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
)

@Composable
fun AwraPlayTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AwraTypography,
        content = content
    )
}
