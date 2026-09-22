package com.legacy.fingame

import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.game.items.Cart
import com.legacy.fingame.game.items.CartLine
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

    @Test
    fun `a mixed cart is split between the two plans it is paid from`() {
        val lines = listOf(
            CartLine(item = TestItems.APPLE, variantId = "red", quantity = 2),
            CartLine(item = TestItems.BALL, variantId = "red", quantity = 1),
            CartLine(item = TestItems.LAMP, variantId = "default", quantity = 1)
        )

        // Два яблока по 15 и мячик за 60 — обязательное; лампа за 150 — нет.
        assertEquals(
            mapOf(SpendKind.MUST to 90, SpendKind.WANT to 150),
            Cart.spendByKindOf(lines)
        )
    }

    @Test
    fun `an empty cart costs neither plan anything`() {
        assertEquals(emptyMap<SpendKind, Int>(), Cart.spendByKindOf(emptyList()))
    }

    @Test
    fun `a cart of one kind says nothing about the other`() {
        val lines = listOf(CartLine(item = TestItems.FISH, variantId = "default", quantity = 3))

        // Именно нет ключа, а не ноль: по этому и видно, что необязательного в корзине нет вовсе.
        assertEquals(mapOf(SpendKind.MUST to 75), Cart.spendByKindOf(lines))
    }
}
