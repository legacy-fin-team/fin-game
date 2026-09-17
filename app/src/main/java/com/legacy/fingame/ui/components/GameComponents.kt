package com.legacy.fingame.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.utils.SpriteLoader
import kotlin.math.roundToInt

/**
 * Draws a sprite from `assets/textures/` via [SpriteLoader] and Coil.
 *
 * Uses [FilterQuality.None] so pixel-art edges stay crisp when the sprite is scaled up, instead of
 * being smoothed by bilinear filtering.
 *
 * @param assetPath path to the sprite, relative to `assets/textures/`. If the file at this path
 *   does not exist, [SpriteLoader] automatically falls back to `error.webp` — that is expected
 *   behavior while assets are being produced, not a bug, so callers should not add their own
 *   fallback handling for missing files.
 * @param contentDescription accessibility description announced for this image, or `null` when
 *   the sprite is purely decorative and should be skipped by screen readers.
 * @param modifier modifier applied to the underlying [AsyncImage].
 */
@Composable
fun Sprite(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loader = remember(context) { SpriteLoader(context) }
    val request = remember(assetPath, loader) { loader.getSprite(assetPath) }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None
    )
}

/**
 * Tappable sprite with no background, border or shadow — the artwork itself is the whole button.
 *
 * When [selected] is `false`, the sprite is drawn at reduced opacity and scale and no underline is
 * shown. When [selected] is `true`, the sprite is drawn at full opacity/scale and a thin underline
 * is drawn beneath it to highlight the active item; there is no circular plate behind the image in
 * either state.
 *
 * @param assetPath path to the sprite, relative to `assets/textures/`, passed through to [Sprite].
 * @param contentDescription accessibility description for the tappable element.
 * @param onClick called when the button is tapped.
 * @param modifier modifier applied to the outer [Column] container.
 * @param size side length (width and height) of the square sprite image.
 * @param selected whether this item is the currently active/selected one; when `true`, the sprite
 *   is shown at full opacity/scale and an underline is drawn beneath it to highlight it.
 */
@Composable
fun SpriteButton(
    assetPath: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    selected: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val alpha = if (selected) 1f else 0.6f
    val scale = if (selected) 1f else 0.92f

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Sprite(
            assetPath = assetPath,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(size * 0.4f)
                .height(2.dp)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
    }
}

/**
 * Player balance pill: coin sprite plus externally supplied text.
 *
 * No number formatting happens here — [text] is rendered as-is.
 *
 * @param modifier modifier applied to the outer [Surface].
 * @param text label displayed next to the coin sprite; expected to already be formatted (e.g. with
 *   currency and thousands separators) by the caller.
 */
@Composable
fun BalanceChip(
    modifier: Modifier = Modifier,
    // TODO: replace this placeholder with the player's real balance from app logic.
    text: String = "Баланс"
) {
    Surface(
        modifier = modifier.wrapContentSize(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Sprite(
                assetPath = Sprites.COIN,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Current-goal card: title, progress bar and percentage text.
 *
 * [progress] is supplied by the caller; the only computation performed here is clamping it to
 * `0f..1f` and formatting it as a rounded percentage string.
 *
 * @param progress fraction of the goal completed, in the `0f..1f` range; values outside that range
 *   are clamped before being used.
 * @param modifier modifier applied to the outer [Surface].
 * @param title label displayed above the progress bar, truncated with an ellipsis if it does not
 *   fit on one line.
 */
@Composable
fun GoalCard(
    progress: Float,
    modifier: Modifier = Modifier,
    // TODO: replace this placeholder with the player's real current goal title from app logic.
    title: String = "текущая цель"
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val percentText = "${(clampedProgress * 100).roundToInt()}%"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GameColors.cardStroke),
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.wrapContentSize()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { clampedProgress },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 0.dp),
                        color = GameColors.goalProgress,
                        trackColor = GameColors.goalProgressTrack
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = percentText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Pill-shaped button with a muted appearance when [enabled] is `false`.
 *
 * @param text label displayed inside the pill.
 * @param onClick called when the button is tapped; not invoked while [enabled] is `false`.
 * @param modifier modifier applied to the outer [Surface].
 * @param enabled whether the button responds to taps; when `false`, the button is rendered with
 *   the disabled container/content colors and taps are ignored.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val containerColor = if (enabled) MaterialTheme.colorScheme.primary else GameColors.disabledContainer
    val contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else GameColors.disabledContent
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .wrapContentSize()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Preview(name = "Components — Light", showBackground = true)
@Composable
private fun GameComponentsLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PreviewContent()
        }
    }
}

@Preview(name = "Components — Dark", showBackground = true)
@Composable
private fun GameComponentsDarkPreview() {
    FinGameTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PreviewContent()
        }
    }
}

@Composable
private fun PreviewContent() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Sprite(assetPath = Sprites.pet("cat"), contentDescription = "Питомец", modifier = Modifier.size(72.dp))
            SpriteButton(
                assetPath = Sprites.SHOP,
                contentDescription = "Открыть магазин",
                onClick = {}
            )
            SpriteButton(
                assetPath = Sprites.LOCATIONS,
                contentDescription = "Локации",
                onClick = {},
                selected = true
            )
            BalanceChip(text = "12 400 ₽")
        }
        GoalCard(title = "Велосипед", progress = 0.64f)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(text = "Купить", onClick = {})
            PillButton(text = "Недоступно", onClick = {}, enabled = false)
        }
    }
}
