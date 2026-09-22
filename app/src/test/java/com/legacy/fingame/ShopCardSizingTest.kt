package com.legacy.fingame

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.game.items.ShopShelf
import com.legacy.fingame.ui.components.PillButtonMinLabelSize
import com.legacy.fingame.ui.screens.CounterButtonSize
import com.legacy.fingame.ui.screens.CounterGap
import com.legacy.fingame.ui.screens.EffectChipGap
import com.legacy.fingame.ui.screens.EffectChipsPerRow
import com.legacy.fingame.ui.screens.CompactEffectChipFixedWidth
import com.legacy.fingame.ui.screens.CompactEffectChipValueWidth
import com.legacy.fingame.ui.screens.ItemCardPadding
import com.legacy.fingame.ui.screens.ItemCellMinSize
import com.legacy.fingame.ui.screens.ShopGridGap
import com.legacy.fingame.ui.screens.ShortScreenCardPadding
import com.legacy.fingame.ui.screens.ShortScreenCardSpriteMaxSize
import com.legacy.fingame.ui.screens.ShortScreenCardSpriteMinSize
import com.legacy.fingame.ui.screens.ShortScreenEffectChipsPerRow
import com.legacy.fingame.ui.screens.ShortScreenItemCellMinSize
import com.legacy.fingame.ui.screens.ShortScreenPurchaseWidth
import com.legacy.fingame.ui.screens.counterValueWidth
import com.legacy.fingame.ui.screens.effectChipsPerRow
import com.legacy.fingame.ui.screens.shopGridCellWidth
import com.legacy.fingame.ui.screens.shortScreenCardSpriteSize
import com.legacy.fingame.ui.screens.shortScreenDetailsWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The arithmetic a shop card is sized by, checked at the two phones held sideways the shop is played
 * on — a 891x411 one and a 800x360 one — where a whole row of cards has to be readable without
 * scrolling for the bottom of it.
 *
 * The widths and heights fed in here are the ones the shop itself measures at runtime: the window
 * less the system bars, less the screen's own 16.dp padding, less the one-line header of a short
 * screen and the bottom bar with the categories and "Купить".
 */
class ShopCardSizingTest {

    /** Smallest a shop cell may be on a short screen, as `GridCells.Adaptive` is told. */
    private val shortScreenCellMinWidth = ShortScreenItemCellMinSize

    /** Smallest a shop cell may be on an ordinary screen. */
    private val cellMinWidth = ItemCellMinSize

    /** Gap the shop's grid keeps between two columns. */
    private val gridGap = ShopGridGap

    /**
     * Width a stacked card leaves for what is in it on the narrowest phone the shop still puts two
     * cards to a row: a 360.dp screen, less its own 16.dp padding on either side, split in two by
     * the grid, less the card's own padding on either side.
     */
    private val narrowCardContentWidth =
        shopGridCellWidth(328.dp, cellMinWidth, gridGap) - ItemCardPadding * 2

    @Test
    fun `the grid splits its width the way an adaptive grid does`() {
        // 891.dp wide, less the screen's own padding: two columns of 288.dp fit, three do not.
        assertEquals(423.5f, shopGridCellWidth(859.dp, shortScreenCellMinWidth, gridGap).value, 0.01f)

        // 800.dp wide: still two columns, just narrower ones.
        assertEquals(378f, shopGridCellWidth(768.dp, shortScreenCellMinWidth, gridGap).value, 0.01f)

        // And the stacked layout's own promise: two columns on a 360.dp phone.
        assertEquals(158f, shopGridCellWidth(328.dp, cellMinWidth, gridGap).value, 0.01f)
    }

    @Test
    fun `a grid too narrow for two columns still has one`() {
        // A cell narrower than the minimum is better than no cell at all: the card then takes the
        // whole width, same as `GridCells.Adaptive` would give it.
        assertEquals(200f, shopGridCellWidth(200.dp, shortScreenCellMinWidth, gridGap).value, 0.01f)
    }

    @Test
    fun `on a 891x411 phone the sprite is drawn at its full size`() {
        val cellWidth = shopGridCellWidth(859.dp, shortScreenCellMinWidth, gridGap)

        // Some 200.dp of height is left for a card there, and the cell is wide: nothing has to give.
        val sprite = shortScreenCardSpriteSize(cardHeight = 200.dp, cellWidth = cellWidth)

        assertEquals(ShortScreenCardSpriteMaxSize, sprite)
        assertDetailsFitTwoChips(cellWidth = cellWidth, spriteSize = sprite)
    }

    @Test
    fun `on a 800x360 phone the sprite gives way to the details beside it`() {
        val cellWidth = shopGridCellWidth(768.dp, shortScreenCellMinWidth, gridGap)

        // Height is not what runs out first here either — the cell's width is.
        val sprite = shortScreenCardSpriteSize(cardHeight = 149.dp, cellWidth = cellWidth)

        // 378 of cell, less the card's padding and the two gaps of its rail, less the column that
        // buys the item and the smallest the details beside it may be: 54 for the sprite.
        assertEquals(54f, sprite.value, 0.01f)
        assertTrue("the sprite must stay smaller than it is on a roomier phone", sprite < ShortScreenCardSpriteMaxSize)
        assertDetailsFitTwoChips(cellWidth = cellWidth, spriteSize = sprite)
    }

    @Test
    fun `a card with next to no height left gets a sprite no taller than itself`() {
        // The other way round: a window flattened until the height, and not the width, is what runs
        // out — the sprite must not make the card taller than the room it is drawn in.
        val sprite = shortScreenCardSpriteSize(cardHeight = 90.dp, cellWidth = 423.5.dp)

        assertEquals(90f - ShortScreenCardPadding.value * 2, sprite.value, 0.01f)
    }

    @Test
    fun `the sprite is never smaller than a sprite`() {
        val sprite = shortScreenCardSpriteSize(cardHeight = 20.dp, cellWidth = 288.dp)

        assertEquals(ShortScreenCardSpriteMinSize, sprite)
    }

    @Test
    fun `a sideways card puts two chips in a row on a phone and three on a wider screen`() {
        val phoneCell = shopGridCellWidth(859.dp, shortScreenCellMinWidth, gridGap)
        val phoneSprite = shortScreenCardSpriteSize(cardHeight = 200.dp, cellWidth = phoneCell)

        assertEquals(
            2,
            effectChipsPerRow(
                availableWidth = shortScreenDetailsWidth(phoneCell, phoneSprite),
                fontScale = 1f,
                maxChips = ShortScreenEffectChipsPerRow
            )
        )

        // A card wide enough for all three — a tablet held sideways, one column of a wide window —
        // says everything the item does in a single line.
        val wideCell = 560.dp
        val wideSprite = shortScreenCardSpriteSize(cardHeight = 200.dp, cellWidth = wideCell)

        assertEquals(
            ShortScreenEffectChipsPerRow,
            effectChipsPerRow(
                availableWidth = shortScreenDetailsWidth(wideCell, wideSprite),
                fontScale = 1f,
                maxChips = ShortScreenEffectChipsPerRow
            )
        )
    }

    @Test
    fun `a bigger font scale puts fewer chips in a row rather than cutting one in half`() {
        val cellWidth = shopGridCellWidth(859.dp, shortScreenCellMinWidth, gridGap)
        val sprite = shortScreenCardSpriteSize(cardHeight = 200.dp, cellWidth = cellWidth)
        val details = shortScreenDetailsWidth(cellWidth, sprite)

        val atLargeScale = effectChipsPerRow(details, fontScale = 1.3f, ShortScreenEffectChipsPerRow)

        assertTrue(
            "a row must never hold more chips than it does at the ordinary font scale",
            atLargeScale <= effectChipsPerRow(details, 1f, ShortScreenEffectChipsPerRow)
        )
        assertTrue("a chip always gets a row, however wide it is drawn", atLargeScale >= 1)
    }

    @Test
    fun `the shelf reserves the rows the card is about to draw`() {
        // The two have to agree, or a card keeps room for one number of rows and draws another —
        // which is exactly how the cards of one row stop ending at the same height.
        val cellWidth = shopGridCellWidth(768.dp, shortScreenCellMinWidth, gridGap)
        val sprite = shortScreenCardSpriteSize(cardHeight = 149.dp, cellWidth = cellWidth)
        val chipsPerRow = effectChipsPerRow(
            availableWidth = shortScreenDetailsWidth(cellWidth, sprite),
            fontScale = 1f,
            maxChips = ShortScreenEffectChipsPerRow
        )

        val food = listOf(TestItems.APPLE, TestItems.FISH, TestItems.CAKE)
        val rows = ShopShelf.effectRowsOf(food, chipsPerRow)

        // Two chips to a row and a cake with three of them: two rows, kept by every card of the
        // shelf, the apple's included.
        assertEquals(2, chipsPerRow)
        assertEquals(2, rows)
    }

    @Test
    fun `a row too narrow for even one chip still holds one`() {
        assertEquals(1, effectChipsPerRow(10.dp, fontScale = 1f, maxChips = 3))
    }

    @Test
    fun `a stacked card on the narrowest phone takes its chips one to a row`() {
        // A compact chip is 72.dp wide at the ordinary font scale and the card has 134.dp of
        // content: two of them stopped fitting side by side when the chip's own padding grew, so
        // the pair is stacked rather than squeezed — and the shelf keeps the rows for it.
        assertEquals(
            1,
            effectChipsPerRow(
                availableWidth = narrowCardContentWidth,
                fontScale = 1f,
                maxChips = EffectChipsPerRow
            )
        )
    }

    @Test
    fun `the counter fits the narrowest card whole, buttons, gaps and number`() {
        val forTheNumber = counterValueWidth(
            contentWidth = narrowCardContentWidth,
            buttonSize = CounterButtonSize,
            gap = CounterGap
        )

        // Nothing is left over and nothing is missing: the row is the two buttons, the two gaps and
        // everything else. A number given a width of its own instead is a number that wraps.
        assertEquals(
            narrowCardContentWidth.value,
            (CounterButtonSize * 2 + CounterGap * 2 + forTheNumber).value,
            0.01f
        )
        assertTrue("the number was left $forTheNumber, i.e. nothing at all", forTheNumber > 0.dp)
    }

    @Test
    fun `the number between the buttons has room for three digits`() {
        val forTheNumber = counterValueWidth(
            contentWidth = narrowCardContentWidth,
            buttonSize = CounterButtonSize,
            gap = CounterGap
        )

        // The cart counts to two digits (see the sideways card's own test below), and the third is
        // the room a quantity has to grow into before anything is ever cropped again. The floor is
        // the one a shrinking label stops at, so a digit is never drawn below it either.
        assertTrue(
            "three digits need ${PillButtonMinLabelSize * 3}, the card left $forTheNumber",
            forTheNumber >= PillButtonMinLabelSize * 3
        )
    }

    @Test
    fun `the counter of a sideways card holds every quantity the cart counts to`() {
        val forTheNumber = counterValueWidth(
            contentWidth = ShortScreenPurchaseWidth,
            buttonSize = CounterButtonSize,
            gap = CounterGap
        )
        val digits = GameViewModel.MAX_ITEM_QUANTITY.toString().length

        assertTrue(
            "$digits digits need ${PillButtonMinLabelSize * digits}, the column left $forTheNumber",
            forTheNumber >= PillButtonMinLabelSize * digits
        )
    }

    /**
     * Asserts the details column of a sideways card is left the width two effect chips need, which
     * is what the sprite gives way for.
     *
     * @param cellWidth width of the cell the card is drawn in.
     * @param spriteSize side of the sprite.
     */
    private fun assertDetailsFitTwoChips(cellWidth: Dp, spriteSize: Dp) {
        val details = shortScreenDetailsWidth(cellWidth, spriteSize)
        val twoChips = (CompactEffectChipFixedWidth + CompactEffectChipValueWidth) * 2 + EffectChipGap

        assertTrue(
            "the details column got $details, less than the $twoChips two chips need",
            details >= twoChips
        )
    }
}
