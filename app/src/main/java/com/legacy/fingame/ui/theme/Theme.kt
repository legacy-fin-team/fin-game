package com.legacy.fingame.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = MintPrimaryLight,
    onPrimary = MintOnPrimaryLight,
    primaryContainer = MintPrimaryContainerLight,
    onPrimaryContainer = MintOnPrimaryContainerLight,
    secondary = AmberSecondaryLight,
    onSecondary = AmberOnSecondaryLight,
    secondaryContainer = AmberSecondaryContainerLight,
    onSecondaryContainer = AmberOnSecondaryContainerLight,
    tertiary = CoralTertiaryLight,
    onTertiary = CoralOnTertiaryLight,
    tertiaryContainer = CoralTertiaryContainerLight,
    onTertiaryContainer = CoralOnTertiaryContainerLight,
    background = CreamBackgroundLight,
    onBackground = OnCreamBackgroundLight,
    surface = CreamSurfaceLight,
    onSurface = OnCreamSurfaceLight,
    surfaceVariant = CreamSurfaceVariantLight,
    onSurfaceVariant = OnCreamSurfaceVariantLight,
    outline = CreamOutlineLight,
    error = ErrorLight,
    onError = OnErrorLight
)

private val DarkColorScheme = darkColorScheme(
    primary = MintPrimaryDark,
    onPrimary = MintOnPrimaryDark,
    primaryContainer = MintPrimaryContainerDark,
    onPrimaryContainer = MintOnPrimaryContainerDark,
    secondary = AmberSecondaryDark,
    onSecondary = AmberOnSecondaryDark,
    secondaryContainer = AmberSecondaryContainerDark,
    onSecondaryContainer = AmberOnSecondaryContainerDark,
    tertiary = CoralTertiaryDark,
    onTertiary = CoralOnTertiaryDark,
    tertiaryContainer = CoralTertiaryContainerDark,
    onTertiaryContainer = CoralOnTertiaryContainerDark,
    background = DeepBlueGreyBackgroundDark,
    onBackground = OnDeepBlueGreyBackgroundDark,
    surface = DeepBlueGreySurfaceDark,
    onSurface = OnDeepBlueGreySurfaceDark,
    surfaceVariant = DeepBlueGreySurfaceVariantDark,
    onSurfaceVariant = OnDeepBlueGreySurfaceVariantDark,
    outline = DeepBlueGreyOutlineDark,
    error = ErrorDark,
    onError = OnErrorDark
)

/**
 * FinGame app theme — cozy casual-game palette (mint + amber).
 * Android 12+ dynamic colors are disabled by default to keep the brand palette
 * recognizable regardless of user wallpapers.
 */
@Composable
fun FinGameTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
