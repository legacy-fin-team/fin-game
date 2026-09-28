package com.legacy.fingame

import com.legacy.fingame.game.items.ItemCategory
import org.junit.Assert.assertEquals
import org.junit.Test

/** Какое слово подписывает кнопку, что кладёт предмет категории на питомца или в комнату, и убирает его оттуда. */
class ItemCategoryTest {

    @Test
    fun `clothes are put on and taken off`() {
        assertEquals("Надеть", ItemCategory.CLOTHES.wearActionTitle(worn = false))
        assertEquals("Снять", ItemCategory.CLOTHES.wearActionTitle(worn = true))
    }

    @Test
    fun `a decoration is stood in the room and taken away from it, not worn`() {
        assertEquals("Поставить", ItemCategory.DECOR.wearActionTitle(worn = false))
        assertEquals("Убрать", ItemCategory.DECOR.wearActionTitle(worn = true))
    }
}
