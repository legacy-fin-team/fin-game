package com.legacy.fingame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MintPrimaryLight = Color(0xFF2E9C7A)
val MintOnPrimaryLight = Color(0xFFFFFFFF)
val MintPrimaryContainerLight = Color(0xFFB6F0DA)
val MintOnPrimaryContainerLight = Color(0xFF00311F)

val AmberSecondaryLight = Color(0xFFE0912B)
val AmberOnSecondaryLight = Color(0xFFFFFFFF)
val AmberSecondaryContainerLight = Color(0xFFFFDDB0)
val AmberOnSecondaryContainerLight = Color(0xFF4A2B00)

val CoralTertiaryLight = Color(0xFFE0745F)
val CoralOnTertiaryLight = Color(0xFFFFFFFF)
val CoralTertiaryContainerLight = Color(0xFFFFDAD1)
val CoralOnTertiaryContainerLight = Color(0xFF3E0A00)

val CreamBackgroundLight = Color(0xFFFFF8EE)
val OnCreamBackgroundLight = Color(0xFF231B10)
val CreamSurfaceLight = Color(0xFFFFFBF4)
val OnCreamSurfaceLight = Color(0xFF231B10)
val CreamSurfaceVariantLight = Color(0xFFF1E4CE)
val OnCreamSurfaceVariantLight = Color(0xFF504536)
val CreamOutlineLight = Color(0xFF827865)
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)

val MintPrimaryDark = Color(0xFF8FD9BB)
val MintOnPrimaryDark = Color(0xFF00382497)
val MintPrimaryContainerDark = Color(0xFF0B5A40)
val MintOnPrimaryContainerDark = Color(0xFFB6F0DA)

val AmberSecondaryDark = Color(0xFFF6BC6A)
val AmberOnSecondaryDark = Color(0xFF4A2B00)
val AmberSecondaryContainerDark = Color(0xFF6B4200)
val AmberOnSecondaryContainerDark = Color(0xFFFFDDB0)

val CoralTertiaryDark = Color(0xFFFFB4A2)
val CoralOnTertiaryDark = Color(0xFF5F1200)
val CoralTertiaryContainerDark = Color(0xFF7F2A15)
val CoralOnTertiaryContainerDark = Color(0xFFFFDAD1)

val DeepBlueGreyBackgroundDark = Color(0xFF14181F)
val OnDeepBlueGreyBackgroundDark = Color(0xFFE4E2DD)
val DeepBlueGreySurfaceDark = Color(0xFF1B2029)
val OnDeepBlueGreySurfaceDark = Color(0xFFE4E2DD)
val DeepBlueGreySurfaceVariantDark = Color(0xFF3A4048)
val OnDeepBlueGreySurfaceVariantDark = Color(0xFFC3C9CF)
val DeepBlueGreyOutlineDark = Color(0xFF8C939A)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)

/**
 * Additional game color tokens not included in the standard Material3 scheme
 * (coins, goal progress, card borders, etc.). Values are chosen to be readable
 * in both light and dark themes via [Composable]-getters.
 */
object GameColors {
    val coin: Color
        @Composable get() = if (isSystemDark()) Color(0xFFFFCB66) else Color(0xFFE0912B)

    val goalProgress: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val goalProgressTrack: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant

    val cardStroke: Color
        @Composable get() = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    val cardShadow: Color
        @Composable get() = if (isSystemDark()) Color.Black.copy(alpha = 0.5f) else Color(0xFF7A6A4E).copy(alpha = 0.18f)

    val success: Color
        @Composable get() = if (isSystemDark()) Color(0xFF8FD9BB) else Color(0xFF2E9C7A)

    val disabledContent: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    val disabledContainer: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
}

@Composable
private fun isSystemDark(): Boolean = androidx.compose.foundation.isSystemInDarkTheme()
