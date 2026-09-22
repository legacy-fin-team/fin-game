package com.legacy.fingame

import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.game.items.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Какая полка магазина считается обязательной тратой, а какая — необязательной. */
class SpendKindTest {

    @Test
    fun `food and toys are what the pet cannot do without`() {
        assertEquals(SpendKind.MUST, ItemCategory.FOOD.spendKind)
        assertEquals(SpendKind.MUST, ItemCategory.TOYS.spendKind)
    }

    @Test
    fun `clothes and decorations are bought for the looks`() {
        assertEquals(SpendKind.WANT, ItemCategory.CLOTHES.spendKind)
        assertEquals(SpendKind.WANT, ItemCategory.DECOR.spendKind)
    }

    @Test
    fun `every shop section says which kind of spending it is`() {
        // Новая категория в данных не должна остаться без классификации: без неё покупка не
        // попала бы ни в одну строку плана, и отчёт периода молча разошёлся бы с балансом.
        ItemCategory.entries.forEach { category ->
            assertNotNull(category.spendKind)
        }
    }

    @Test
    fun `each kind of spending has a name for the player`() {
        assertEquals("Обязательные", SpendKind.MUST.title)
        assertEquals("Необязательные", SpendKind.WANT.title)
    }
}
