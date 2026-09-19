package com.legacy.fingame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.legacy.fingame.R

/**
 * Additional game color tokens not included in the standard Material3 scheme
 * (coins, goal progress, card borders, etc.). All values are sourced from
 * `res/values/colors.xml` via [Composable]-getters so the palette can be
 * changed without touching code, and stay readable in both light and dark
 * themes.
 */
object GameColors {
    val coin: Color
        @Composable get() = colorResource(
            id = if (isSystemDark()) R.color.game_coin_dark else R.color.game_coin_light
        )

    val goalProgress: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val goalProgressTrack: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant

    val cardStroke: Color
        @Composable get() = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    val cardShadow: Color
        @Composable get() = if (isSystemDark()) {
            colorResource(id = R.color.game_card_shadow_base_dark).copy(alpha = 0.5f)
        } else {
            colorResource(id = R.color.game_card_shadow_base_light).copy(alpha = 0.18f)
        }

    val success: Color
        @Composable get() = colorResource(
            id = if (isSystemDark()) R.color.game_success_dark else R.color.game_success_light
        )

    val disabledContent: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    val disabledContainer: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
}

/** @return whether the system is currently in dark theme. */
@Composable
private fun isSystemDark(): Boolean = androidx.compose.foundation.isSystemInDarkTheme()
