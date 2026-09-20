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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens
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
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null
) {
    val context = LocalContext.current
    val loader = remember(context) { SpriteLoader(context) }
    val request = remember(assetPath, loader) { loader.getSprite(assetPath) }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        colorFilter = colorFilter
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
 * @param size side length (width and height) of the square sprite image on a phone; on tablets the
 *   button is enlarged from it by [GameDimens.buttonSize], so call sites only state the phone size.
 * @param selected whether this item is the currently active/selected one; when `true`, the sprite
 *   is shown at full opacity/scale and an underline is drawn beneath it to highlight it.
 */
/** Translucent black laid over a sprite while its button is held down. */
private val PressedOverlayColor = Color(0x59000000)

/** Gap between the sprite of a [SpriteButton] and the underline that marks it as selected. */
private val SpriteButtonUnderlineGap = 4.dp

/** Thickness of the underline a selected [SpriteButton] is marked with. */
private val SpriteButtonUnderlineThickness = 2.dp

/**
 * How tall a [SpriteButton] ends up being, underline included.
 *
 * Lets a layout reserve exactly the room such a button takes without repeating what the button is
 * built of — which is what the shop cards do to come out the same height whether or not they have
 * a row of variants to show.
 *
 * @param size the size the button's sprite is asked for, i.e. the phone-sized value.
 * @return The full height of the button on the current screen.
 */
@Composable
@ReadOnlyComposable
fun spriteButtonHeight(size: Dp): Dp =
    GameDimens.buttonSize(size) + SpriteButtonUnderlineGap + SpriteButtonUnderlineThickness

@Composable
fun SpriteButton(
    assetPath: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    selected: Boolean = false
) {
    val spriteSize = GameDimens.buttonSize(size)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressFilter = if (pressed) {
        ColorFilter.tint(PressedOverlayColor, BlendMode.SrcAtop)
    } else {
        null
    }

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Sprite(
            assetPath = assetPath,
            contentDescription = null,
            modifier = Modifier.size(spriteSize),
            colorFilter = pressFilter
        )
        Spacer(modifier = Modifier.height(SpriteButtonUnderlineGap))
        Box(
            modifier = Modifier
                .width(spriteSize * 0.4f)
                .height(SpriteButtonUnderlineThickness)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
    }
}

/**
 * Player balance pill: coin sprite plus the coins the player has.
 *
 * @param balance number of coins to show, taken from
 *   [com.legacy.fingame.game.GameUiState.balance].
 * @param modifier modifier applied to the outer [Surface].
 */
@Composable
fun BalanceChip(
    balance: Int,
    modifier: Modifier = Modifier
) {
    val text = balance.toString()
    Surface(
        modifier = modifier.wrapContentSize(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Sprite(
                assetPath = Sprites.COIN,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
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

/** Padding around the label of a [PillButton] on a phone; enlarged on tablets. */
private val PillHorizontalPadding = 24.dp
private val PillVerticalPadding = 12.dp

/**
 * Name of a pet stat as the player reads it.
 *
 * @return The Russian title of the stat.
 */
fun StatKind.title(): String = when (this) {
    StatKind.HEALTH -> "Здоровье"
    StatKind.HUNGER -> "Голод"
    StatKind.PLEASURE -> "Удовольствие"
}

/**
 * Color a stat value is written in: it turns from the healthy color to the warning one as the stat
 * empties, so a pet that needs the player is noticed without reading the numbers.
 *
 * @param fraction how full the stat is, in the `0f..1f` range.
 * @return The color of the value at that level.
 */
@Composable
private fun statValueColor(fraction: Float): Color =
    if (fraction <= StatLowLevel) MaterialTheme.colorScheme.error else GameColors.success

/** How empty a stat has to get before it is shown as a warning; see [statValueColor]. */
private const val StatLowLevel = 0.25f

/** Side of the stat icon inside a [StatChip]; the chip is sized around it. */
private val StatIconSize = 24.dp

/**
 * One stat of the pet as a [StatValueChip]: the icon of the stat and how full it is, in percent.
 *
 * Several of these sit in a row next to the balance instead of taking up half the screen with bars.
 *
 * @param stat the stat to show.
 * @param stats the pet's stats to read [stat] from.
 * @param modifier modifier applied to the outer [Surface].
 */
@Composable
fun StatChip(
    stat: StatKind,
    stats: PetStats,
    modifier: Modifier = Modifier
) {
    val fraction = stats.fractionOf(stat)

    StatValueChip(
        stat = stat,
        value = "${(fraction * 100).roundToInt()}%",
        valueColor = statValueColor(fraction),
        modifier = modifier
    )
}

/**
 * Chip made of a stat's icon and one short value next to it, the shape every stat is shown in —
 * how full it is on the main screen, how much an item moves it in the inventory.
 *
 * The stat is never named in words here: the icon says which one it is, so the chip stays small
 * enough for several of them to sit in a row. The name is still announced to screen readers
 * through the icon's description.
 *
 * @param stat the stat whose icon the chip carries.
 * @param value the text written next to the icon, already formatted for the player.
 * @param valueColor color of that text.
 * @param modifier modifier applied to the outer [Surface].
 */
@Composable
fun StatValueChip(
    stat: StatKind,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.wrapContentSize(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Sprite(
                assetPath = Sprites.stat(stat.xmlName),
                contentDescription = stat.title(),
                modifier = Modifier.size(StatIconSize)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Pill-shaped button with a muted appearance when [enabled] is `false`.
 *
 * The pill takes its size from its label plus [PillHorizontalPadding]/[PillVerticalPadding], both
 * of which grow on tablets via [GameDimens.buttonSize]; the label follows with a larger text style
 * so the button does not end up as a big pill around small text.
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
    val textStyle = if (GameDimens.isTabletScreen) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.labelLarge
    }

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
            modifier = Modifier.padding(
                horizontal = GameDimens.buttonSize(PillHorizontalPadding),
                vertical = GameDimens.buttonSize(PillVerticalPadding)
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = textStyle,
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

/** Preview of the same components on a tablet, where the buttons are drawn enlarged. */
@Preview(name = "Components — Tablet", showBackground = true, device = Devices.TABLET)
@Composable
private fun GameComponentsTabletPreview() {
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
            Sprite(assetPath = Sprites.pet("cat", "white"), contentDescription = "Питомец", modifier = Modifier.size(72.dp))
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
            BalanceChip(balance = 12400)
        }
        GoalCard(title = "Велосипед", progress = 0.64f)
        val stats = PetStats(
            mapOf(
                StatKind.HEALTH to 80,
                StatKind.HUNGER to 15,
                StatKind.PLEASURE to 55
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatKind.entries.forEach { stat -> StatChip(stat = stat, stats = stats) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(text = "Купить", onClick = {})
            PillButton(text = "Недоступно", onClick = {}, enabled = false)
        }
    }
}
