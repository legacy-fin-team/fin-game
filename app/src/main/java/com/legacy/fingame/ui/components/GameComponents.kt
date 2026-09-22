package com.legacy.fingame.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import com.legacy.fingame.ui.screens.balancesDescriptionOf
import com.legacy.fingame.ui.screens.balancesTextOf
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
 * @param label short caption to draw instead of the sprite while [assetPath] does not resolve to a
 *   real file yet — a button whose icon is still missing from `assets/` would otherwise render
 *   [SpriteLoader]'s `error.webp` placeholder, which reads as a bug rather than as unfinished art.
 *   The fallback keeps the same size, press feedback and underline as the sprite it stands in for,
 *   so once the file is added the button becomes an icon with no change at the call site. Left
 *   `null` — the default — a button whose sprite is missing still falls back to `error.webp`, which
 *   is what every other [SpriteButton] call wants.
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
    selected: Boolean = false,
    label: String? = null
) {
    val context = LocalContext.current
    val loader = remember(context) { SpriteLoader(context) }
    val hasSprite = remember(assetPath, loader) { loader.hasSprite(assetPath) }
    val spriteSize = GameDimens.buttonSize(size)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressFilter = if (pressed) {
        ColorFilter.tint(PressedOverlayColor, BlendMode.SrcAtop)
    } else {
        null
    }
    val labelTextStyle = MaterialTheme.typography.labelLarge
    val (labelMinFontSize, labelMaxFontSize) = pillButtonAutoSizeRange(
        minLabelSize = PillButtonMinLabelSize,
        styleFontSize = labelTextStyle.fontSize,
        density = LocalDensity.current
    )

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
        if (hasSprite || label == null) {
            Sprite(
                assetPath = assetPath,
                contentDescription = null,
                modifier = Modifier.size(spriteSize),
                colorFilter = pressFilter
            )
        } else {
            // The sprite is not in assets/ yet: draw the button's own plate and caption instead of
            // letting Sprite fall back to error.webp, which would read as a bug rather than as art
            // still to be delivered. Same size as the sprite it stands in for, so the row around it
            // is unaffected either way.
            Box(
                modifier = Modifier
                    .size(spriteSize)
                    .background(
                        color = if (pressed) {
                            GameColors.disabledContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(BorderStroke(1.dp, GameColors.cardStroke), RoundedCornerShape(20.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = labelTextStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = labelMinFontSize,
                        maxFontSize = labelMaxFontSize
                    )
                )
            }
        }
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
 * Пилюля со счетами игрока: иконка монеты и два числа через чёрточку — текущие деньги и тело
 * вклада (`100 | 510`).
 *
 * Иконка одна на оба: две иконки в углу экрана не помещаются, а чёрточка читается как «и ещё»
 * не хуже. Проценты по вкладу здесь не показываются — они ещё не начислены, и показывать их как
 * деньги игрока значило бы обещать.
 *
 * Надпись всегда в одну строку и ужимается вместе с шириной пилюли, как ужимается надпись на
 * [PillButton], поэтому при крупном системном шрифте на узком экране числа не обрезаются.
 *
 * Для тех, кто слушает экран, пилюля — один узел с описанием из [balancesDescriptionOf]:
 * собственный текст чисел из дерева убран совсем, иначе чёрточки между ними прочитались бы вслух
 * как есть.
 *
 * @param balance текущие деньги, из [com.legacy.fingame.game.GameUiState.balance].
 * @param depositAmount тело вклада, из [com.legacy.fingame.game.GameUiState.depositAmount].
 * @param modifier модификатор внешнего [Surface].
 */
@Composable
fun BalanceChip(
    balance: Int,
    depositAmount: Int,
    modifier: Modifier = Modifier
) {
    val text = balancesTextOf(balance, depositAmount)
    val description = balancesDescriptionOf(balance, depositAmount)
    val textStyle = MaterialTheme.typography.labelLarge
    val (minFontSize, maxFontSize) = pillButtonAutoSizeRange(
        minLabelSize = PillButtonMinLabelSize,
        styleFontSize = textStyle.fontSize,
        density = LocalDensity.current
    )

    Surface(
        modifier = modifier
            .wrapContentSize()
            .clearAndSetSemantics { contentDescription = description },
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
                style = textStyle,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = minFontSize,
                    maxFontSize = maxFontSize
                )
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
 * Horizontal padding a [PillButton] uses instead of [PillHorizontalPadding] when its `compact`
 * parameter is `true` — a button stretched to fill a shop card's width needs as much of that width
 * as possible handed to the label, not spent on padding around it.
 */
private val PillCompactHorizontalPadding = 6.dp

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

/** Sizes a [StatValueChip] is built out of: the icon, the padding around it and the gap after it. */
private val StatIconSize = 24.dp
private val StatChipHorizontalPadding = 12.dp
private val StatChipVerticalPadding = 6.dp
private val StatChipIconGap = 6.dp

/**
 * The same sizes for a chip drawn `compact`, i.e. small enough for two of them to sit side by side
 * inside a shop card.
 *
 * A card's own content is `134.dp` wide at the narrowest the shop lays one out at (see
 * [com.legacy.fingame.ui.screens.ShopScreen]), and a row of it is meant to hold two chips and the
 * gap between them, i.e. some `65.dp` per chip. Everything but the value's own text is fixed here
 * and adds up to `27.dp` of that, which leaves the text `38.dp` — room for the three characters
 * ("+20", "-5") an effect is ever written in, at the font scales a player reads the game at.
 */
private val CompactStatIconSize = 14.dp
private val CompactStatChipHorizontalPadding = 5.dp
private val CompactStatChipVerticalPadding = 3.dp
private val CompactStatChipIconGap = 3.dp

/**
 * How tall a `compact` [StatValueChip] — and so a [EffectChip] in a shop card — comes out on the
 * current screen, the chip's text style and font scale included.
 *
 * Lets a layout keep room for a row of chips without measuring one first, which is what the shop's
 * cards do to come out the same height whether or not their item has effects to show; the chip
 * itself is drawn at exactly this height, so what is reserved is what is used. Mirrors
 * [spriteButtonHeight], which does the same for a row of sprite buttons.
 *
 * @return The full height of a compact chip.
 */
@Composable
@ReadOnlyComposable
fun compactEffectChipHeight(): Dp {
    val lineHeight = MaterialTheme.typography.labelSmall.lineHeight
    val textHeight = with(LocalDensity.current) { lineHeight.toDp() }
    return maxOf(textHeight, CompactStatIconSize) + CompactStatChipVerticalPadding * 2
}

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
 * @param compact whether to draw the chip at the smaller size a shop card has room for: the same
 *   chip in every part — icon, value, shape — only sized so that two of them fit a card's width
 *   side by side. A compact chip is exactly [compactEffectChipHeight] tall, so a layout can keep
 *   room for a row of them in advance.
 */
@Composable
fun StatValueChip(
    stat: StatKind,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val sizeModifier = if (compact) Modifier.height(compactEffectChipHeight()) else Modifier

    Surface(
        modifier = modifier
            .wrapContentSize()
            .then(sizeModifier),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) {
                    CompactStatChipHorizontalPadding
                } else {
                    StatChipHorizontalPadding
                },
                vertical = if (compact) CompactStatChipVerticalPadding else StatChipVerticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Sprite(
                assetPath = Sprites.stat(stat.xmlName),
                contentDescription = stat.title(),
                modifier = Modifier.size(if (compact) CompactStatIconSize else StatIconSize)
            )
            Spacer(
                modifier = Modifier.width(if (compact) CompactStatChipIconGap else StatChipIconGap)
            )
            Text(
                text = value,
                style = if (compact) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.labelLarge
                },
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * One of an item's effects on the pet, in the same shape every stat is shown in: the icon of the
 * stat and, next to it, how much the item moves that stat, with its sign. The stat is not named in
 * words — the icon already says which one it is.
 *
 * This is the one place an effect is drawn in the whole game: the inventory shows what an owned item
 * will do, the shop shows the same thing before the item is bought, and both read alike — a gain in
 * the healthy color, a loss in the warning one.
 *
 * @param stat the stat the effect is on.
 * @param value how much the effect adds to it; a negative value is shown with its minus sign.
 * @param modifier modifier applied to the chip surface.
 * @param compact whether to draw the chip at the size a shop card has room for; see [StatValueChip].
 */
@Composable
fun EffectChip(
    stat: StatKind,
    value: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val sign = if (value > 0) "+" else ""

    StatValueChip(
        stat = stat,
        value = "$sign$value",
        valueColor = if (value < 0) MaterialTheme.colorScheme.error else GameColors.success,
        modifier = modifier,
        compact = compact
    )
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
 * @param modifier modifier applied to the outer [Surface]; a button given `Modifier.fillMaxWidth()`
 *   stretches into all the width it is handed instead of wrapping its label, which the label then
 *   has all of to grow or shrink into (see `compact` below and [PillButtonMinLabelSize]).
 * @param enabled whether the button responds to taps; when `false`, the button is rendered with
 *   the disabled container/content colors and taps are ignored.
 * @param selected whether the button is the one picked out of a group of them, e.g. the deposit
 *   term the player has chosen, or `null` — the default — when the button belongs to no such group
 *   at all. A `true`/`false` button keeps its own container color when selected — the muted
 *   [androidx.compose.material3.ColorScheme.secondaryContainer] rather than the filled one — and
 *   carries [androidx.compose.ui.semantics.SemanticsProperties.Selected] for a screen reader, which
 *   is what tells a deselected button apart from one that is simply turned off: a picked option is
 *   still an option, and one announced as disabled reads as a button the player may not press. A
 *   `null` button carries neither: it is not part of a group, so there is nothing to say it was not
 *   picked, and it looks exactly as an unselected one does.
 * @param compact whether the pill (a) uses [PillCompactHorizontalPadding] instead of the normal,
 *   wider [PillHorizontalPadding] and (b) actually stretches into a [modifier] with
 *   `Modifier.fillMaxWidth()` in it rather than staying wrap-content-sized regardless (`Compose`'s
 *   `Modifier.wrapContentSize()`, which the button otherwise carries so *other* callers' `modifier`
 *   cannot stretch it by accident, does exactly that — it would let a wider space through to the
 *   surrounding layout for alignment purposes while leaving the pill itself, and so the label inside
 *   it, no wider than the label needs). Meant for a button meant to fill a narrow space — a shop
 *   card, say — so the label gets the padding out of its way and the width to shrink into before
 *   `autoSize` ever needs its floor; everywhere else the button keeps wrapping its own label.
 *
 * The label is always kept to a single line ([Text]'s `maxLines = 1`, `softWrap = false`): a button
 * narrow enough that the label does not fit at its normal size shrinks the label's font instead of
 * breaking a word across two lines, via `autoSize`, down to [PillButtonMinLabelSize] — a size that
 * does not grow with the system font scale, so the label can always shrink enough to fit rather than
 * running out of room and being cropped instead (see [pillButtonAutoSizeRange]). At a width wide
 * enough for the label, this changes nothing — `autoSize` picks the same size the label's
 * [MaterialTheme.typography] style already asks for.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
    selected: Boolean? = null
) {
    val isSelected = selected == true
    val containerColor = when {
        !enabled -> GameColors.disabledContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primary
    }
    val contentColor = when {
        !enabled -> GameColors.disabledContent
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimary
    }
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val textStyle = if (GameDimens.isTabletScreen) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.labelLarge
    }
    val horizontalPadding = if (compact) PillCompactHorizontalPadding else PillHorizontalPadding
    val (minFontSize, maxFontSize) = pillButtonAutoSizeRange(
        minLabelSize = PillButtonMinLabelSize,
        styleFontSize = textStyle.fontSize,
        density = density
    )
    // wrapContentSize() relaxes the *minimum* width/height a caller's modifier might otherwise force
    // on the pill down to zero, so the pill stays only as big as its label needs even if it is placed
    // somewhere that hands it a bigger minimum than that — which is what keeps every other PillButton
    // call site wrap-content-sized. A `compact` button wants the opposite of that: it is handed
    // `Modifier.fillMaxWidth()` on purpose, specifically to be stretched, so it skips this altogether.
    val sizingModifier = if (compact) Modifier else Modifier.wrapContentSize()

    Surface(
        modifier = modifier
            .then(sizingModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick
            )
            .semantics { if (selected != null) this.selected = isSelected },
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = GameDimens.buttonSize(horizontalPadding),
                vertical = GameDimens.buttonSize(PillVerticalPadding)
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = textStyle,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = minFontSize,
                    maxFontSize = maxFontSize
                )
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
            BalanceChip(balance = 12400, depositAmount = 500)
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
        // The two sizes an effect is shown in: the roomy one of the inventory's item window and the
        // compact one that fits two to a row inside a shop card.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EffectChip(stat = StatKind.HUNGER, value = 20)
            EffectChip(stat = StatKind.HEALTH, value = -5)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            EffectChip(stat = StatKind.HUNGER, value = 20, compact = true)
            EffectChip(stat = StatKind.HEALTH, value = -5, compact = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(text = "Купить", onClick = {})
            PillButton(text = "Недоступно", onClick = {}, enabled = false)
        }
        // Sprites.BUDGET has no file in assets/ yet, so this shows SpriteButton's label fallback.
        SpriteButton(
            assetPath = Sprites.BUDGET,
            contentDescription = "Открыть бюджет",
            onClick = {},
            label = "Бюджет"
        )
    }
}
