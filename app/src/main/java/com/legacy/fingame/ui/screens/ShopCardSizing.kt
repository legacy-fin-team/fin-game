package com.legacy.fingame.ui.screens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor

/**
 * Sizes of a shop card worked out from the room the screen actually leaves for it, rather than
 * guessed at with a constant that happens to fit one device.
 *
 * Most of this is about the card laid out sideways — the shape [ShopScreen] takes on a short screen,
 * i.e. on a phone held sideways — which has to be readable whole without scrolling for the bottom of
 * it: the shop's header and its bottom bar take what they take, and whatever is left between them is
 * all one row of cards may be. Such a card is three things standing in a row: its sprite, the details
 * beside it (name, price and what the item does to the pet) and the column that buys it. The two on
 * the right are sized by what has to fit in them, so it is the sprite that gives way.
 *
 * All of it is plain subtraction, and it lives here — away from the composables, where it can be
 * checked at the widths, heights and font scales a phone really has.
 */

/** Padding between a sideways card's edge and what is in it. */
internal val ShortScreenCardPadding = 12.dp

/** Gap between the three columns of a sideways card. */
internal val ShortScreenRailGap = 12.dp

/**
 * Width of a sideways card's rightmost column, the one that buys the item: enough for the widest of
 * the purchase controls, which is the counter — two [CounterButtonSize] buttons, two [CounterGap]
 * gaps and the quantity between them, i.e. `48 * 2 + 2 * 2 + 28`. The 28.dp left for
 * the number holds twice the two digits the cart ever counts up to (see
 * [com.legacy.fingame.game.GameViewModel.MAX_ITEM_QUANTITY]) at the ordinary font scale, and the
 * number shrinks rather than wraps above it, see [counterValueWidth].
 *
 * The "Добавить" pill that stands there instead on a shelf of items bought once is a hair wider than
 * this at its normal size (`8` letters of a 13.sp font plus the compact pill's own padding come to
 * `129.6.dp`), so its label shrinks by that hair and no further — which is what a pill's label shrink
 * is for, see [com.legacy.fingame.ui.components.PillButtonMinLabelSize]. Widening the column instead
 * would have to come out of the sprite beside it, and a legible sprite is worth more than a tenth of
 * a point of type.
 */
internal val ShortScreenPurchaseWidth = 128.dp

/**
 * Smallest width the middle column of a sideways card is left with, i.e. what the sprite gives way
 * down to [ShortScreenCardSpriteMinSize] for: two effect chips side by side at the ordinary font
 * scale, `(CompactEffectChipFixedWidth + CompactEffectChipValueWidth) * 2 + EffectChipGap`.
 *
 * A stacked card on the narrowest phone no longer has room for two of them (a card there is
 * `134.dp` wide inside its padding against the `148.dp` two chips ask for), so this is the one
 * layout that still shows an item's two effects side by side; the stacked one wraps them instead,
 * see [effectChipsPerRow].
 */
internal val ShortScreenDetailsMinWidth = 148.dp

/**
 * Most chips one row of a sideways card is ever given, however wide the card turns out to be. Three
 * is every effect an item of this game has (the cake's), so a wide enough card says all of it in one
 * line; [effectChipsPerRow] is what decides whether this card is one of them.
 */
internal const val ShortScreenEffectChipsPerRow = 3

/** The range a sideways card's sprite is sized within, however much room the screen has. */
internal val ShortScreenCardSpriteMinSize = 40.dp
internal val ShortScreenCardSpriteMaxSize = 96.dp

/** Gap between two effect chips, across and down. */
internal val EffectChipGap = 4.dp

/**
 * Everything but the value's own text in a compact effect chip: the 16.dp icon, the 8.dp of padding
 * on either side of the row and the 4.dp gap after the icon (see
 * [com.legacy.fingame.ui.components.StatValueChip]'s `compact` mode), i.e. `8 + 16 + 4 + 8`.
 *
 * Unlike the value beside it this part does not grow with the font scale — an icon is a picture and
 * the padding is a padding — which is why [effectChipsPerRow] scales only the other half.
 */
internal val CompactEffectChipFixedWidth = 36.dp

/**
 * Width the value of a compact effect chip takes at the ordinary font scale: three characters of the
 * game's pixel font at the `labelSmall` the chip writes them in, i.e. the widest an effect is ever
 * written ("+35"). The font is monospaced, so that is `3 * (11 + 0.5)` rounded up to the grid.
 * Grows with the font scale, which is why [effectChipsPerRow] is given one.
 */
internal val CompactEffectChipValueWidth = 36.dp

/**
 * Width of one cell of the shop's grid, i.e. how wide one card is drawn.
 *
 * Mirrors what `GridCells.Adaptive` itself does — as many columns of at least [minCellWidth] as fit,
 * never fewer than one, the leftover width shared out between them — so a card can be sized before
 * the grid has laid it out.
 *
 * @param gridWidth width the grid itself was given.
 * @param minCellWidth smallest a cell may be, as handed to `GridCells.Adaptive`.
 * @param gap gap between two columns.
 * @return Width of one cell.
 */
internal fun shopGridCellWidth(gridWidth: Dp, minCellWidth: Dp, gap: Dp): Dp {
    val columns = floor((gridWidth + gap) / (minCellWidth + gap)).toInt().coerceAtLeast(1)
    return (gridWidth - gap * (columns - 1)) / columns
}

/**
 * Side of the square sprite on a sideways card.
 *
 * The sprite is drawn as big as the card is tall, and no bigger than the width left once the details
 * and the purchase column beside it have had theirs — whichever of the two runs out first — and is
 * then kept within [ShortScreenCardSpriteMinSize] and [ShortScreenCardSpriteMaxSize] so it is
 * neither a speck nor bigger than the artwork itself.
 *
 * Sizing it this way is what keeps a whole row of cards on a short screen: the card ends up as tall
 * as its tallest column, and the sprite is never the column that makes it taller than the screen.
 *
 * @param cardHeight all the height one card may take, i.e. the grid's own height less what it keeps
 * around its content.
 * @param cellWidth width of one cell, as [shopGridCellWidth] works it out.
 * @return Side of the sprite.
 */
internal fun shortScreenCardSpriteSize(cardHeight: Dp, cellWidth: Dp): Dp {
    val byHeight = cardHeight - ShortScreenCardPadding * 2
    val byWidth = cellWidth - ShortScreenCardPadding * 2 - ShortScreenRailGap * 2 -
            ShortScreenPurchaseWidth - ShortScreenDetailsMinWidth
    return minOf(byHeight, byWidth)
        .coerceIn(ShortScreenCardSpriteMinSize, ShortScreenCardSpriteMaxSize)
}

/**
 * Width the middle column of a sideways card is left with once the sprite and the purchase column
 * have taken theirs: the width the name, the price and the effect chips are laid out in.
 *
 * @param cellWidth width of one cell, as [shopGridCellWidth] works it out.
 * @param spriteSize side of the sprite, as [shortScreenCardSpriteSize] works it out.
 * @return Width of the details column.
 */
internal fun shortScreenDetailsWidth(cellWidth: Dp, spriteSize: Dp): Dp =
    cellWidth - ShortScreenCardPadding * 2 - ShortScreenRailGap * 2 - spriteSize -
            ShortScreenPurchaseWidth

/**
 * Width left for the quantity of a counter between the "−" and the "+" that change it.
 *
 * The row is stretched across the whole control (see [PurchaseControl]) and the buttons take a fixed
 * size each, so everything the two of them and the gaps beside them do not take is the number's —
 * which is what keeps a two-digit quantity on one line instead of wrapping it into two, the way a
 * number boxed into a fixed 28.dp did.
 *
 * @param contentWidth width the counter's row is laid out in, i.e. a card's width inside its own
 * padding, or the width of the purchase column of a sideways card.
 * @param buttonSize side of one of the two buttons.
 * @param gap gap between a button and the number.
 * @return Width the number is drawn in; it shrinks to fit that width rather than wrapping, down to
 * [com.legacy.fingame.ui.components.PillButtonMinLabelSize].
 */
internal fun counterValueWidth(contentWidth: Dp, buttonSize: Dp, gap: Dp): Dp =
    contentWidth - buttonSize * 2 - gap * 2

/**
 * How many effect chips are put in one row of a card.
 *
 * Counted rather than fixed, because a chip is as wide as the number written in it and the player
 * chooses how big text is drawn: at the ordinary font scale two of them fit a card laid out sideways
 * and one fits the narrowest stacked card, while at a big font scale the same widths hold fewer.
 * Counting is what keeps the shelf's promise — a card reserves exactly as many rows as it will draw
 * (see [com.legacy.fingame.game.items.ShopShelf.effectRowsOf]), so the cards of one row still end at
 * the same height, and no chip is ever squeezed or cut in half.
 *
 * @param availableWidth width the chips are laid out in.
 * @param fontScale the system font scale the chips' value is drawn at.
 * @param maxChips most chips to put in one row however much room there is; the layout's own choice.
 * @return Chips per row, at least one — a chip too wide for its row is still drawn whole, it simply
 * gets a row to itself.
 */
internal fun effectChipsPerRow(availableWidth: Dp, fontScale: Float, maxChips: Int): Int {
    val chipWidth = CompactEffectChipFixedWidth + CompactEffectChipValueWidth * fontScale
    val fit = floor((availableWidth + EffectChipGap) / (chipWidth + EffectChipGap)).toInt()
    return fit.coerceIn(1, maxChips.coerceAtLeast(1))
}
