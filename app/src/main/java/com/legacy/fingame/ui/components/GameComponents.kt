package com.legacy.fingame.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.legacy.fingame.ui.screens.depositTextOf
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens
import com.legacy.fingame.utils.SpriteLoader
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

/** Translucent black laid over a sprite while its button is held down. */
private val PressedOverlayColor = Color(0x59000000)

/**
 * How long the press highlight of a [SpriteButton] is kept on screen at the very least.
 *
 * A quick tap would otherwise not show it at all. Both of the game's tap targets that live on the
 * darkening alone — the "+" and the "−" of the budget and of the shop — sit inside a scrolling
 * container, and `clickable` inside one holds a press back for `TapIndicationDelay` (150 ms) so
 * that the start of a scroll does not light a button up. Lift the finger before that, and
 * foundation emits the press and its release back to back in a single frame: the highlight is
 * turned on and off between two recompositions, and the player sees the value change with no sign
 * that the button was theirs. Holding it for this long afterwards costs nothing on a slow press
 * and is the whole feedback on a fast one.
 */
private const val PressFeedbackMinMillis = 120L

/**
 * Whether a button should be drawn as pressed right now, kept on for at least
 * [PressFeedbackMinMillis] after the finger is lifted.
 *
 * Reads the interactions themselves rather than
 * [androidx.compose.foundation.interaction.collectIsPressedAsState], which follows the press
 * exactly and so cannot be seen at all when press and release land in the same frame.
 *
 * @param interactionSource the source the button's `clickable` reports its presses to.
 * @return State that is `true` while the button is held and for a short while after.
 */
@Composable
private fun pressFeedbackOf(interactionSource: InteractionSource): State<Boolean> {
    // Keyed by the source, like the effect below it: handed a different source, the state starts
    // over with it rather than carrying the previous button's highlight into the new one.
    val pressed = remember(interactionSource) { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        var release: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    // A press during the tail of the previous one keeps the highlight on rather
                    // than letting that tail switch it off under the finger.
                    release?.cancel()
                    pressed.value = true
                }

                is PressInteraction.Release,
                is PressInteraction.Cancel -> {
                    release = launch {
                        delay(PressFeedbackMinMillis)
                        pressed.value = false
                    }
                }
            }
        }
    }

    return pressed
}

/** How much of its own opacity a [SpriteButton] keeps while it has nothing to do. */
private const val DisabledContentAlpha = 0.38f

/** Room left between the caption of a [SpriteButton]'s stand-in plate and the edge of the plate. */
private val SpriteButtonLabelPadding = 4.dp

/** Gap between the sprite of a [SpriteButton] and the underline that marks it as selected. */
private val SpriteButtonUnderlineGap = 4.dp

/** Thickness of the underline a selected [SpriteButton] is marked with. */
private val SpriteButtonUnderlineThickness = 3.dp

/** How wide that underline is, as a fraction of the sprite it is drawn under. */
private const val SpriteButtonUnderlineWidthFraction = 0.6f

/** How round the ends of the underline are. */
private val SpriteButtonUnderlineCorner = 2.dp

/**
 * How tall a [SpriteButton] ends up being, underline included.
 *
 * Lets a layout reserve exactly the room such a button takes without repeating what the button is
 * built of — which is what the shop cards do to come out the same height whether or not they have
 * a row of variants to show.
 *
 * @param size the size the button's sprite is asked for, i.e. the phone-sized value.
 * @param showIndicator whether the button in question draws the underline of a selected item at
 *   all; when it does not, the row is the sprite and nothing else. Must match what the button
 *   itself is given, or the layout keeps room the button never uses (or cuts off the room it does).
 * @return The full height of the button on the current screen.
 */
@Composable
@ReadOnlyComposable
fun spriteButtonHeight(size: Dp, showIndicator: Boolean = true): Dp =
    GameDimens.buttonSize(size) + if (showIndicator) {
        SpriteButtonUnderlineGap + SpriteButtonUnderlineThickness
    } else {
        0.dp
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
 * @param enabled whether the button responds to taps; when `false`, the sprite (or the caption
 *   standing in for it) is dimmed and taps are ignored, and a screen reader announces the
 *   button as disabled — which is what a button that is there but has nothing to do right now
 *   should look and sound like, instead of looking pressable and doing nothing.
 * @param selected whether this item is the currently active/selected one; when `true`, an
 *   underline is drawn beneath the sprite to highlight it. Ignored while [showIndicator] is
 *   `false`, where there is no underline to draw.
 * @param showIndicator whether this button belongs to a group one item of which is selected — the
 *   tabs of the shop, the variants of an item — and so keeps room under the sprite for the
 *   underline that marks it. `true` by default, which is what every such group wants and what the
 *   button has always done; a button that belongs to no group is given `false` and comes out
 *   exactly as tall as its sprite, since room kept under it for a mark it can never carry lifts it
 *   off the row it stands in for no reason anyone can see. Whatever this is, [spriteButtonHeight]
 *   has to be told the same thing.
 * @param label emergency caption, drawn instead of the sprite while [assetPath] does not resolve to
 *   a real file yet — a button whose icon is still missing from `assets/` would otherwise render
 *   [SpriteLoader]'s `error.webp` placeholder, which reads as a bug rather than as unfinished art.
 *   It is a way of surviving missing art, never a kind of button: a captioned plate standing in a
 *   row of bare icons reads as a different control altogether, which is exactly what the row of
 *   money and action buttons on the main screen looked like until their icons were drawn. A button
 *   whose sprite is in `assets/` is given no caption at all.
 *   The fallback keeps the same size, press feedback and underline as the sprite it stands in for,
 *   so once the file is added the button becomes an icon with no change at the call site. A caption
 *   too wide for the plate at its normal size is drawn smaller instead of being cropped by it, down
 *   to whatever size the plate has room for. Its own
 *   text carries no semantics of its own — [contentDescription] on the outer button already says
 *   what the button does, so a screen reader is meant to read that once, not the caption too. Left
 *   `null` — the default — a button whose sprite is missing still falls back to `error.webp`, which
 *   is what every other [SpriteButton] call wants.
 */
@Composable
fun SpriteButton(
    assetPath: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    enabled: Boolean = true,
    selected: Boolean = false,
    showIndicator: Boolean = true,
    label: String? = null
) {
    val context = LocalContext.current
    val loader = remember(context) { SpriteLoader(context) }
    // Looking the file up costs a walk of the assets, and only a button carrying a caption has
    // anything to do with the answer: without one it falls back to error.webp either way. A grid of
    // buttons — the shop's, say — would otherwise pay for that walk once per item on the screen.
    val hasSprite = label == null || remember(assetPath, loader) { loader.hasSprite(assetPath) }
    // The caption is drawn only while it has a missing sprite to stand in for; with the file there
    // — or with no caption given at all — the button is the artwork and nothing else.
    val fallbackLabel = label.takeIf { !hasSprite }
    val spriteSize = GameDimens.buttonSize(size)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by pressFeedbackOf(interactionSource)
    val pressFilter = if (pressed) {
        ColorFilter.tint(PressedOverlayColor, BlendMode.SrcAtop)
    } else {
        null
    }
    val labelTextStyle = MaterialTheme.typography.labelLarge
    // The game's font is a monospaced one: every glyph takes a full em, so a caption of n letters
    // comes out n times its own font size wide, whatever the letters are. A plate this narrow can
    // therefore only hold the caption below the floor a pill label stops at, and the floor is
    // lowered to what the plate actually has room for rather than the caption being cropped at it.
    // The extra letter of room covers the letter spacing between them and the rounding.
    val labelRoom = spriteSize - SpriteButtonLabelPadding * 2
    val labelFloor = minOf(
        PillButtonMinLabelSize,
        labelRoom / (label?.length?.plus(1) ?: 1).coerceAtLeast(1)
    )
    val (labelMinFontSize, labelMaxFontSize) = pillButtonAutoSizeRange(
        minLabelSize = labelFloor,
        styleFontSize = labelTextStyle.fontSize,
        density = LocalDensity.current
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .semantics {
                this.contentDescription = contentDescription
                if (!enabled) disabled()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (fallbackLabel == null) {
            Sprite(
                assetPath = assetPath,
                contentDescription = null,
                modifier = Modifier
                    .size(spriteSize)
                    .alpha(if (enabled) 1f else DisabledContentAlpha),
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
                    .alpha(if (enabled) 1f else DisabledContentAlpha)
                    .background(
                        color = if (pressed) {
                            GameColors.disabledContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(BorderStroke(1.dp, GameColors.cardStroke), RoundedCornerShape(20.dp))
                    .padding(SpriteButtonLabelPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fallbackLabel,
                    // The outer Column already carries contentDescription for the whole button;
                    // without this, TalkBack would also read the caption as its own node.
                    modifier = Modifier.clearAndSetSemantics {},
                    style = labelTextStyle,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    // One line, and wrapping left on: `softWrap = false` lays the caption out
                    // against an endless width, so `autoSize` is never told it did not fit and
                    // leaves it at full size for the plate to crop. With wrapping on, the caption
                    // is measured against the plate and shrinks into it instead.
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = labelMinFontSize,
                        maxFontSize = labelMaxFontSize
                    )
                )
            }
        }
        if (showIndicator) {
            Spacer(modifier = Modifier.height(SpriteButtonUnderlineGap))
            Box(
                modifier = Modifier
                    .width(spriteSize * SpriteButtonUnderlineWidthFraction)
                    .height(SpriteButtonUnderlineThickness)
                    .background(
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(SpriteButtonUnderlineCorner)
                    )
            )
        }
    }
}

/** Sizes a [BalanceChip] is built out of: the coin, the padding around it and the gap after it. */
private val BalanceCoinSize = 24.dp
private val BalanceChipVerticalPadding = 6.dp
private val BalanceChipIconGap = 8.dp

/**
 * Пилюля со счетами игрока: иконка монеты, текущие деньги и — мелкой подписью рядом — тело вклада.
 *
 * Вклад написан словом «вклад», а не отделён чёрточкой, и, пока вклада нет, подписи нет совсем:
 * `250 | 0` читалось как два случайных числа и значок между ними. Проценты по вкладу здесь не
 * показываются — они ещё не начислены, и показывать их как деньги игрока значило бы обещать.
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
    val text = balance.toString()
    val depositText = depositTextOf(depositAmount)
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = BalanceChipVerticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Sprite(
                assetPath = Sprites.COIN,
                contentDescription = null,
                modifier = Modifier.size(BalanceCoinSize)
            )
            Spacer(modifier = Modifier.width(BalanceChipIconGap))
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
            if (depositText != null) {
                Spacer(modifier = Modifier.width(BalanceChipIconGap))
                Text(
                    text = depositText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    softWrap = false
                )
            }
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
 * @param title label displayed above the progress bar, wrapped onto a second line when it does not
 *   fit on one — the card is cut to the width of the chips above it, and on a narrow phone with the
 *   system text turned up "Текущая цель" is wider than that — and only cut with an ellipsis if even
 *   two lines are not enough. Stated by the caller rather than defaulted here: a card that names the goal
 *   out of its own pocket says the same thing whatever the player is actually saving for.
 */
@Composable
fun GoalCard(
    progress: Float,
    modifier: Modifier = Modifier,
    title: String
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
                    maxLines = GoalTitleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Stripped down to a plain bar: the round cap, the gap before it and the dot
                    // Material puts at the end are the one thing on this screen drawn in a
                    // different language from the pixel art standing next to it.
                    LinearProgressIndicator(
                        progress = { clampedProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(GoalProgressHeight),
                        color = GameColors.goalProgress,
                        trackColor = GameColors.goalProgressTrack,
                        strokeCap = StrokeCap.Butt,
                        gapSize = 0.dp,
                        // Material takes a drawing of the stop indicator, not a way of
                        // saying there is none; drawing nothing is how it is left out.
                        drawStopIndicator = {}
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
 * How thick the goal card's progress bar is drawn. Material's own four points are a hairline next
 * to the pixel art around them; eight read as a bar the child can see filling up.
 */
private val GoalProgressHeight = 8.dp

/** How many lines the goal card's title may take before it is cut; see [GoalCard]. */
private const val GoalTitleMaxLines = 2

/** Padding around the label of a [PillButton] on a phone; enlarged on tablets. */
private val PillHorizontalPadding = 20.dp
private val PillVerticalPadding = 10.dp

/**
 * Horizontal padding a [PillButton] uses instead of [PillHorizontalPadding] when its `compact`
 * parameter is `true` — a button stretched to fill a shop card's width needs as much of that width
 * as possible handed to the label, not spent on padding around it. Not *no* padding, though: a word
 * that starts where the pill starts reads as a word that has fallen out of it.
 */
private val PillCompactHorizontalPadding = 12.dp

/**
 * Smallest a [PillButton] is ever drawn, whatever its label ends up being.
 *
 * The padding above is written for a label at its normal size; a label shrunk by `autoSize`, or a
 * shorter scale altogether, would otherwise take the whole button down with it, and the button
 * would become hard to hit exactly where the screen is already tight. Same 44 dp the platform's own
 * accessibility guidance asks for.
 */
private val PillMinTouchSize = 44.dp

/**
 * How loudly a [PillButton] speaks.
 *
 * One screen carries one [Primary] button — the thing the player came to that screen to do — and
 * everything else on it is quieter than that. The game is played on a cream background under a
 * pixel font, where a filled button is a solid block of color: nine of them on one screen is what
 * made the budget screen flicker, not the color itself.
 */
enum class PillStyle {
    /** The one action of the screen: filled with the brand color. */
    Primary,

    /** An action next to that one: filled, but softly. The default a button is given. */
    Tonal,

    /** An action of its own that is not the point of the screen: outline and label, no fill. */
    Outlined,

    /** Something closer to a link than to a button: the label alone. */
    Text
}

/** Thickness and tone of the outline an [PillStyle.Outlined] — or deselected — pill is drawn with. */
private val PillBorderWidth = 1.5.dp
private const val PillBorderAlpha = 0.6f

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
private val StatChipVerticalPadding = 8.dp
private val StatChipIconGap = 8.dp

/**
 * The same sizes for a chip drawn `compact`, i.e. small enough for two of them to sit side by side
 * inside a shop card.
 *
 * Everything but the value's own text is fixed here and adds up to `36.dp`: the icon, the gap after
 * it and the padding on either side. The value itself is three characters at most ("+20", "-5"),
 * and the font is monospaced, so at `labelSmall` they want `3 × 11.sp` — `33.dp` at the normal font
 * scale and `43.dp` at the largest one the game is read at. A shop card has nowhere to take that
 * extra room from, so the text is given `autoSize` down to [PillButtonMinLabelSize] rather than
 * being cut to "+2…": that floor is a physical size, so three characters at it are those same
 * `33.dp` whatever the player's font setting, and a chip never needs more than `36 + 33 = 69.dp`.
 *
 * The shop keeps its own copy of the `36.dp` (`CompactEffectChipFixedWidth` in
 * [com.legacy.fingame.ui.screens.ShopScreen]'s sizing) to work out how wide a card has to be for
 * two of these and the gap between them; the two numbers have to move together.
 *
 * The sizes are all multiples of four: this chip stands next to everything else on the main screen,
 * and one padded by three while its neighbours are padded by four is exactly the near-miss that
 * reads as carelessness without anyone being able to say what is wrong.
 */
private val CompactStatIconSize = 16.dp
private val CompactStatChipHorizontalPadding = 8.dp
private val CompactStatChipVerticalPadding = 4.dp
private val CompactStatChipIconGap = 4.dp

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
 * The chip is always as wide as it is at [StatFullValue]: a value is "100%" one moment and "0%" the
 * next, and a chip that followed its own text would make the row — and everything the main screen
 * lays out from the width of that row, the demo's time button included — move under the player's
 * finger between two taps.
 *
 * @param stat the stat to show.
 * @param stats the pet's stats to read [stat] from.
 * @param modifier modifier applied to the outer [Surface].
 * @param compact whether to draw the chip at the smaller size several of them fit a narrow screen
 *   in; see [StatValueChip].
 */
@Composable
fun StatChip(
    stat: StatKind,
    stats: PetStats,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val fraction = stats.fractionOf(stat)

    StatValueChip(
        stat = stat,
        value = "${(fraction * 100).roundToInt()}%",
        valueColor = statValueColor(fraction),
        modifier = modifier,
        compact = compact,
        reservedValue = StatFullValue
    )
}

/** The widest value a [StatChip] ever says: a stat is never fuller than that. */
private const val StatFullValue = "100%"

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
 * @param reservedValue the widest text [value] can ever be, or `null` — the default — for a chip
 *   that is as wide as what it says. When given, the value keeps at least the room this text takes
 *   in the chip's own style and stands in the middle of it, so the chip is one width whatever it
 *   says at the moment.
 */
@Composable
fun StatValueChip(
    stat: StatKind,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    reservedValue: String? = null
) {
    val sizeModifier = if (compact) Modifier.height(compactEffectChipHeight()) else Modifier
    val textStyle = if (compact) {
        MaterialTheme.typography.labelSmall
    } else {
        MaterialTheme.typography.labelLarge
    }
    // Only ever bites on a compact chip, which is handed a width worked out from the sizes above
    // and has to fit three characters into it at any font scale; a full-sized chip wraps its own
    // text and so is always drawn at the style's own size.
    val (minFontSize, maxFontSize) = pillButtonAutoSizeRange(
        minLabelSize = PillButtonMinLabelSize,
        styleFontSize = textStyle.fontSize,
        density = LocalDensity.current
    )
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val reservedWidth = if (reservedValue == null) {
        0.dp
    } else {
        remember(reservedValue, textStyle, density) {
            with(density) {
                textMeasurer.measure(text = reservedValue, style = textStyle).size.width.toDp()
            }
        }
    }

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
                modifier = Modifier.widthIn(min = reservedWidth),
                style = textStyle,
                color = valueColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = minFontSize,
                    maxFontSize = maxFontSize
                )
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
 * The button of the game: a label in a pill, in one of the four voices of [PillStyle].
 *
 * The pill takes its size from its label plus [PillHorizontalPadding]/[PillVerticalPadding], both of
 * which grow on tablets via [GameDimens.buttonSize], and is never smaller than [PillMinTouchSize].
 * The label itself does not grow with the screen: it is `bodyMedium`, the very size the text around
 * the button is written in, everywhere. A button whose label is larger than the sentence it answers
 * is a button that shouts, and a row of them is what the owner of this game saw when he said the
 * words in the frames look much bigger than plain text.
 *
 * @param text label displayed inside the pill.
 * @param onClick called when the button is tapped; not invoked while [enabled] is `false`.
 * @param modifier modifier applied to the outer [Surface]; a button given `Modifier.fillMaxWidth()`
 *   stretches into all the width it is handed instead of wrapping its label, which the label then
 *   has all of to grow or shrink into (see `compact` below and [PillButtonMinLabelSize]).
 * @param style how loud the button is; see [PillStyle]. [PillStyle.Tonal] by default on purpose —
 *   a button that says nothing about its own importance is not the most important one on the screen.
 *   Ignored while [selected] says `true` or `false`: a button that is part of a group is drawn by
 *   whether it is the chosen one, which is the only thing about it the player needs to see.
 * @param enabled whether the button responds to taps; when `false`, the button is rendered with
 *   the disabled container/content colors and taps are ignored.
 * @param compact whether the pill (a) uses [PillCompactHorizontalPadding] instead of the normal,
 *   wider [PillHorizontalPadding] and (b) actually stretches into a [modifier] with
 *   `Modifier.fillMaxWidth()` in it rather than staying wrap-content-sized regardless (`Compose`'s
 *   `Modifier.wrapContentSize()`, which the button otherwise carries so *other* callers' `modifier`
 *   cannot stretch it by accident, does exactly that — it would let a wider space through to the
 *   surrounding layout for alignment purposes while leaving the pill itself, and so the label inside
 *   it, no wider than the label needs). Meant for a button meant to fill a narrow space — a shop
 *   card, say — so the label gets the padding out of its way and the width to shrink into before
 *   `autoSize` ever needs its floor; everywhere else the button keeps wrapping its own label.
 * @param selected whether the button is the one picked out of a group of them, e.g. the deposit
 *   term the player has chosen, or `null` — the default — when the button belongs to no such group
 *   at all. The picked one is the filled one and the rest are outlines, which is the way round it
 *   has to be: before this, a picked term came out pale and the six it was picked over came out in
 *   solid green, so the group shouted down its own answer. A `true`/`false` button also carries
 *   [androidx.compose.ui.semantics.SemanticsProperties.Selected] for a screen reader, which is what
 *   tells a deselected button apart from one that is simply turned off: a picked option is still an
 *   option, and one announced as disabled reads as a button the player may not press. A `null`
 *   button carries neither: it is not part of a group, so there is nothing to say it was not picked.
 * @param autoShrink whether the label may shrink to fit the pill. On by default, and worth turning
 *   off for every button that stands in a row with others: `autoSize` measures each button against
 *   its own width, so three buttons sharing a row by weight came out in three different sizes —
 *   "Всё" twice the size of "Половина" beside it. A row of buttons whose labels are already short
 *   enough does not need it and looks wrong with it.
 *
 * With [autoShrink] on, the label is kept to a single line ([Text]'s `maxLines = 1`, with wrapping
 * left on so the label is measured against the width the pill has — `softWrap = false` would lay it
 * out against an endless one, and `autoSize`, never told it did not fit, would leave it at full size
 * to be cropped by a pill narrower than that): a button narrow enough that the label does not fit at
 * its normal size shrinks the label's font instead of breaking a word across two lines, down to
 * [PillButtonMinLabelSize] — a size that does not grow with the system font scale, so the label can
 * always shrink enough to fit rather than running out of room and being cropped instead (see
 * [pillButtonAutoSizeRange]). At a width wide enough for the label, this changes nothing.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Tonal,
    enabled: Boolean = true,
    compact: Boolean = false,
    selected: Boolean? = null,
    autoShrink: Boolean = true
) {
    val isSelected = selected == true
    val containerColor = when {
        !enabled -> GameColors.disabledContainer
        selected == false -> Color.Transparent
        isSelected || style == PillStyle.Primary -> MaterialTheme.colorScheme.primary
        style == PillStyle.Tonal -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val contentColor = when {
        !enabled -> GameColors.disabledContent
        selected == false -> MaterialTheme.colorScheme.onSurfaceVariant
        isSelected || style == PillStyle.Primary -> MaterialTheme.colorScheme.onPrimary
        style == PillStyle.Tonal -> MaterialTheme.colorScheme.onPrimaryContainer
        style == PillStyle.Outlined -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.primary
    }
    // An unfilled button still has to have an edge, or a row of them reads as a row of loose words.
    val borderStroke = if (enabled && (selected == false || (selected == null && style == PillStyle.Outlined))) {
        BorderStroke(
            width = PillBorderWidth,
            color = MaterialTheme.colorScheme.outline.copy(alpha = PillBorderAlpha)
        )
    } else {
        null
    }
    val textStyle = MaterialTheme.typography.bodyMedium
    val horizontalPadding = if (compact) PillCompactHorizontalPadding else PillHorizontalPadding
    val (minFontSize, maxFontSize) = pillButtonAutoSizeRange(
        minLabelSize = PillButtonMinLabelSize,
        styleFontSize = textStyle.fontSize,
        density = LocalDensity.current
    )
    // wrapContentSize() relaxes the *minimum* width/height a caller's modifier might otherwise force
    // on the pill down to zero, so the pill stays only as big as its label needs even if it is placed
    // somewhere that hands it a bigger minimum than that — which is what keeps every other PillButton
    // call site wrap-content-sized. A `compact` button wants the opposite of that: it is handed
    // `Modifier.fillMaxWidth()` on purpose, specifically to be stretched, so it skips this altogether.
    val sizingModifier = if (compact) Modifier else Modifier.wrapContentSize()

    // The click belongs to the Surface itself rather than to the modifier handed to it: Surface adds
    // its own background and clip *inside* that modifier, so a `clickable` placed there draws its
    // ripple under the fill and outside the rounding — which on a pill as small as a term chip came
    // out as a rectangle standing around the button. Given to Surface, the ripple lands over the fill
    // and inside the shape, and it takes its color from contentColor, i.e. from the button's own
    // label rather than from the text color of the card the button happens to stand on.
    Surface(
        onClick = onClick,
        modifier = modifier
            .then(sizingModifier)
            .defaultMinSize(minHeight = GameDimens.buttonSize(PillMinTouchSize))
            .semantics { if (selected != null) this.selected = isSelected },
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor,
        border = borderStroke
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
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = if (autoShrink) {
                    TextAutoSize.StepBased(minFontSize = minFontSize, maxFontSize = maxFontSize)
                } else {
                    null
                }
            )
        }
    }
}

/**
 * Everything the game is built out of, on one screen, in the light theme: the four voices of
 * [PillButton] with the states each of them has, the two kinds of [SpriteButton], the chips and a
 * window. What a new palette or a new scale does to the game is visible here before it is visible
 * anywhere else.
 */
@Preview(name = "Components — Light", showBackground = true, heightDp = 1200)
@Composable
private fun GameComponentsLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PreviewContent()
        }
    }
}

/** The same components on a tablet, where the buttons grow but the writing on them does not. */
@Preview(name = "Components — Tablet", showBackground = true, device = Devices.TABLET)
@Composable
private fun GameComponentsTabletPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PreviewContent()
        }
    }
}

/** The same components in the dark theme, which is warm now rather than blue-grey. */
@Preview(name = "Components — Dark", showBackground = true, heightDp = 1200)
@Composable
private fun GameComponentsDarkPreview() {
    FinGameTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PreviewContent()
        }
    }
}

/**
 * The buttons on the narrowest screen the game is laid out for, at the largest font scale it is
 * read at — the pair of numbers every label in the game is checked against.
 */
@Preview(name = "Buttons — 360dp, font 1.0", showBackground = true, widthDp = 360, heightDp = 540)
@Composable
private fun PillButtonsNarrowPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PillButtonGallery(modifier = Modifier.padding(16.dp))
        }
    }
}

@Preview(
    name = "Buttons — 360dp, font 1.3",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    fontScale = 1.3f
)
@Composable
private fun PillButtonsLargeFontPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PillButtonGallery(modifier = Modifier.padding(16.dp))
        }
    }
}

/** A window on the narrowest screen: its buttons stand in a column, one under the other. */
@Preview(name = "Dialog — 360dp", showBackground = true, widthDp = 360, heightDp = 420)
@Composable
private fun GameDialogPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.padding(16.dp)) {
                GameDialogBlock(
                    title = "Бюджет",
                    closeDescription = "Отменить подтверждение",
                    onDismiss = {},
                    actions = {
                        PillButton(
                            text = "Подтвердить",
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            style = PillStyle.Primary,
                            compact = true
                        )
                        PillButton(
                            text = "Отмена",
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            style = PillStyle.Text,
                            compact = true
                        )
                    }
                ) {
                    Text(
                        text = "После подтверждения бюджет не изменить.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Every state a [PillButton] has, in rows: the four styles, a row of buttons sharing the width the
 * way a picker's do, a group with one of them selected, and a button with nothing to do.
 */
@Composable
private fun PillButtonGallery(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Четыре голоса",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(text = "Главное", onClick = {}, style = PillStyle.Primary)
            PillButton(text = "Рядом", onClick = {}, style = PillStyle.Tonal)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(text = "Своё", onClick = {}, style = PillStyle.Outlined)
            PillButton(text = "Тихое", onClick = {}, style = PillStyle.Text)
            PillButton(text = "Нельзя", onClick = {}, enabled = false)
        }
        Text(
            text = "Ряд: один кегль на всех",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // autoShrink = false is the point of this row: with it on, each of the three measures its
        // own width and "Всё" comes out twice the size of "Половина" standing beside it.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Ничего", "50%", "Всё").forEach { label ->
                PillButton(
                    text = label,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    style = PillStyle.Text,
                    compact = true,
                    autoShrink = false
                )
            }
        }
        Text(
            text = "Группа: выбранное — залитое",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (2..7).forEach { term ->
                PillButton(
                    text = term.toString(),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    compact = true,
                    selected = term == 3,
                    autoShrink = false
                )
            }
        }
        Text(
            text = "Кнопка во всю ширину",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PillButton(
            text = "Подтвердить",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            style = PillStyle.Primary,
            compact = true
        )
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
            Sprite(
                assetPath = Sprites.pet("cat", "white"),
                contentDescription = "Питомец",
                modifier = Modifier.size(72.dp)
            )
            BalanceChip(balance = 12400, depositAmount = 500)
            // The same chip with nothing on deposit: the caption is gone, not written as a zero.
            BalanceChip(balance = 250, depositAmount = 0)
        }
        // A button that belongs to a row of tabs keeps room for the mark of the chosen one; a button
        // that stands on its own is exactly as tall as its artwork.
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpriteButton(
                assetPath = Sprites.SHOP,
                contentDescription = "Открыть магазин",
                onClick = {},
                selected = true
            )
            SpriteButton(
                assetPath = Sprites.LOCATIONS,
                contentDescription = "Локации",
                onClick = {}
            )
            SpriteButton(
                assetPath = Sprites.SHOP,
                contentDescription = "Открыть магазин",
                onClick = {},
                showIndicator = false
            )
            // What a button whose art is still missing looks like: no such file is in assets/, so
            // this one draws the caption fallback instead of a picture. Every icon the game itself
            // asks for is drawn by now, so the fallback has to be shown by a path made up for it.
            SpriteButton(
                assetPath = "ui/not-drawn-yet.webp",
                contentDescription = "Кнопка без картинки",
                onClick = {},
                showIndicator = false,
                label = "Нет арта"
            )
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatKind.entries.forEach { stat ->
                StatChip(stat = stat, stats = stats, compact = true)
            }
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
        PillButtonGallery()
    }
}
