package com.legacy.fingame.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.legacy.fingame.R

/**
 * Builds the FinGame Material3 [ColorScheme] by reading the palette from
 * `res/values/colors.xml`. The scheme depends on the explicit [darkTheme]
 * flag rather than the system setting, so callers of [FinGameTheme] can
 * override the theme regardless of the device's current configuration.
 *
 * @param darkTheme whether to build the dark variant of the scheme.
 */
@Composable
private fun finGameColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = colorResource(id = R.color.primary_dark),
            onPrimary = colorResource(id = R.color.on_primary_dark),
            primaryContainer = colorResource(id = R.color.primary_container_dark),
            onPrimaryContainer = colorResource(id = R.color.on_primary_container_dark),
            secondary = colorResource(id = R.color.secondary_dark),
            onSecondary = colorResource(id = R.color.on_secondary_dark),
            secondaryContainer = colorResource(id = R.color.secondary_container_dark),
            onSecondaryContainer = colorResource(id = R.color.on_secondary_container_dark),
            tertiary = colorResource(id = R.color.tertiary_dark),
            onTertiary = colorResource(id = R.color.on_tertiary_dark),
            tertiaryContainer = colorResource(id = R.color.tertiary_container_dark),
            onTertiaryContainer = colorResource(id = R.color.on_tertiary_container_dark),
            background = colorResource(id = R.color.background_dark),
            onBackground = colorResource(id = R.color.on_background_dark),
            surface = colorResource(id = R.color.surface_dark),
            onSurface = colorResource(id = R.color.on_surface_dark),
            surfaceVariant = colorResource(id = R.color.surface_variant_dark),
            onSurfaceVariant = colorResource(id = R.color.on_surface_variant_dark),
            outline = colorResource(id = R.color.outline_dark),
            error = colorResource(id = R.color.error_dark),
            onError = colorResource(id = R.color.on_error_dark)
        )
    } else {
        lightColorScheme(
            primary = colorResource(id = R.color.primary_light),
            onPrimary = colorResource(id = R.color.on_primary_light),
            primaryContainer = colorResource(id = R.color.primary_container_light),
            onPrimaryContainer = colorResource(id = R.color.on_primary_container_light),
            secondary = colorResource(id = R.color.secondary_light),
            onSecondary = colorResource(id = R.color.on_secondary_light),
            secondaryContainer = colorResource(id = R.color.secondary_container_light),
            onSecondaryContainer = colorResource(id = R.color.on_secondary_container_light),
            tertiary = colorResource(id = R.color.tertiary_light),
            onTertiary = colorResource(id = R.color.on_tertiary_light),
            tertiaryContainer = colorResource(id = R.color.tertiary_container_light),
            onTertiaryContainer = colorResource(id = R.color.on_tertiary_container_light),
            background = colorResource(id = R.color.background_light),
            onBackground = colorResource(id = R.color.on_background_light),
            surface = colorResource(id = R.color.surface_light),
            onSurface = colorResource(id = R.color.on_surface_light),
            surfaceVariant = colorResource(id = R.color.surface_variant_light),
            onSurfaceVariant = colorResource(id = R.color.on_surface_variant_light),
            outline = colorResource(id = R.color.outline_light),
            error = colorResource(id = R.color.error_light),
            onError = colorResource(id = R.color.on_error_light)
        )
    }
}

/**
 * FinGame app theme — cozy casual-game palette (mint + amber).
 * Android 12+ dynamic colors are disabled by default to keep the brand palette
 * recognizable regardless of user wallpapers.
 *
 * @param darkTheme whether to use the dark color scheme; defaults to the system setting.
 * @param dynamicColor whether to use Android 12+ dynamic (wallpaper-based) colors instead of
 *   the brand palette; disabled by default.
 * @param content the app content to render inside this theme.
 */
@Composable
fun FinGameTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = finGameColorScheme(darkTheme = darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
