package com.legacy.fingame.game.items

/**
 * The shelf of the shop: the items of one category taken together rather than one by one.
 *
 * A shop card is not laid out on its own — the cards standing next to each other are cut to the same
 * pattern so they come out the same height (see [com.legacy.fingame.ui.screens.ShopScreen]) — and
 * this is where what that pattern has to hold is worked out, out of the items themselves and not out
 * of what any one card happens to measure.
 */
object ShopShelf {

    /**
     * How many rows of effect chips the cards of one shelf keep room for.
     *
     * Every card keeps room for as many rows as the busiest item of the shelf needs: a cake with
     * three effects gives the apple beside it a second row's worth of empty space, and the two end
     * at the same height. A shelf where nothing affects the pet at all — clothes, decorations — keeps
     * no room whatsoever, so nothing is left standing empty on those cards either.
     *
     * @param items the items of the shelf, as the catalog registered them.
     * @param chipsPerRow how many chips one row of a card holds; anything below one is read as one,
     * since a row that holds nothing could never show an effect.
     * @return Rows to keep room for, zero when no item of the shelf affects the pet.
     */
    fun effectRowsOf(items: List<Item>, chipsPerRow: Int): Int {
        val perRow = chipsPerRow.coerceAtLeast(1)
        return items.maxOfOrNull { item -> rowsFor(item.effects.size, perRow) } ?: 0
    }

    /**
     * @param effects how many effects an item has.
     * @param chipsPerRow how many chips one row holds; one or more.
     * @return How many whole rows those effects take, rounded up.
     */
    private fun rowsFor(effects: Int, chipsPerRow: Int): Int =
        (effects + chipsPerRow - 1) / chipsPerRow
}
