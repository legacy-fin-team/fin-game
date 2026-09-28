package com.legacy.fingame

import com.legacy.fingame.game.items.CompositeItemCatalog
import com.legacy.fingame.game.items.CustomItemDraft
import com.legacy.fingame.game.items.CustomItemIcons
import com.legacy.fingame.game.items.CustomItems
import com.legacy.fingame.game.items.CustomItemsCodec
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.customItemOf
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Свои предметы взрослого: черновик, сборка предмета, кодек и общий каталог. */
class CustomItemsTest {

    private val puzzleDraft = CustomItemDraft(
        name = "Пазл",
        price = 40,
        category = ItemCategory.OTHER,
        iconPath = CustomItemIcons.ALL.first()
    )

    private val cookieDraft = CustomItemDraft(
        name = "Печенье",
        price = 10,
        category = ItemCategory.FOOD,
        effects = mapOf(StatKind.HUNGER to 15, StatKind.HEALTH to -5, StatKind.PLEASURE to 0),
        iconPath = "shop/categories/food.webp"
    )

    @Test
    fun `a valid draft has no errors`() {
        assertEquals(emptyList<String>(), puzzleDraft.validate())
        assertEquals(emptyList<String>(), cookieDraft.validate())
    }

    @Test
    fun `a draft without a name, with a bad price or a bad effect is refused`() {
        assertTrue(puzzleDraft.copy(name = "   ").validate().isNotEmpty())
        assertTrue(puzzleDraft.copy(name = "a".repeat(CustomItems.NAME_MAX + 1)).validate().isNotEmpty())
        assertTrue(puzzleDraft.copy(price = 0).validate().isNotEmpty())
        assertTrue(puzzleDraft.copy(price = CustomItems.PRICE_MAX + 1).validate().isNotEmpty())
        assertTrue(cookieDraft.copy(effects = mapOf(StatKind.HUNGER to 55)).validate().isNotEmpty())
        assertTrue(puzzleDraft.copy(iconPath = "").validate().isNotEmpty())
        assertTrue(puzzleDraft.validate(existingCount = CustomItems.MAX).isNotEmpty())
        assertEquals(2, puzzleDraft.copy(name = "", price = 0).validate().size)
    }

    @Test
    fun `an item is built out of a valid draft`() {
        val item = customItemOf(cookieDraft, id = "custom-1")!!

        assertEquals("custom-1", item.id)
        assertEquals("Печенье", item.name)
        assertEquals(10, item.price)
        assertEquals(ItemCategory.FOOD, item.category)
        // Нулевой эффект не хранится.
        assertEquals(mapOf(StatKind.HUNGER to 15, StatKind.HEALTH to -5), item.effects)
        assertEquals("shop/categories/food.webp", item.getIconPath(item.defaultVariantId))
        assertTrue(CustomItems.isCustom(item.id))
    }

    @Test
    fun `effects are dropped for categories that have none and the name is trimmed`() {
        val item = customItemOf(
            puzzleDraft.copy(name = "  Па\u001Fзл ", effects = mapOf(StatKind.HUNGER to 10)),
            id = "custom-2"
        )!!

        assertEquals("Пазл", item.name)
        assertEquals(emptyMap<StatKind, Int>(), item.declaredEffects)
    }

    @Test
    fun `an invalid draft builds no item`() {
        assertNull(customItemOf(puzzleDraft.copy(name = ""), id = "custom-3"))
    }

    @Test
    fun `the codec reads back what it wrote`() {
        val items = listOf(
            customItemOf(puzzleDraft, "custom-1")!!,
            customItemOf(cookieDraft, "custom-2")!!
        )

        val decoded = CustomItemsCodec.decode(CustomItemsCodec.encode(items))

        assertEquals(items, decoded)
    }

    @Test
    fun `the codec drops broken records and keeps the rest`() {
        val good = customItemOf(puzzleDraft, "custom-1")!!
        val raw = CustomItemsCodec.encode(listOf(good)) +
            "\u001Ecustom-9\u001FБита\u001Fnot-a-price\u001Fother\u001F\u001Fui/coin.webp" +
            "\u001Ecustom-8\u001FБита\u001F10\u001Fmystery\u001F\u001Fui/coin.webp" +
            "\u001Eплохо"

        assertEquals(listOf(good), CustomItemsCodec.decode(raw))
        assertEquals(emptyList<Any>(), CustomItemsCodec.decode(null))
        assertEquals(emptyList<Any>(), CustomItemsCodec.decode(""))
    }

    @Test
    fun `the codec keeps no more than the limit`() {
        val many = (1..CustomItems.MAX + 5).map { customItemOf(puzzleDraft, "custom-$it")!! }

        assertEquals(CustomItems.MAX, CustomItemsCodec.decode(CustomItemsCodec.encode(many)).size)
    }

    @Test
    fun `the composite catalog puts custom items after the game's own`() {
        val puzzle = customItemOf(puzzleDraft, "custom-1")!!
        val cookie = customItemOf(cookieDraft, "custom-2")!!
        var custom = listOf(puzzle)
        val catalog = CompositeItemCatalog(FakeItemCatalog()) { custom }

        assertEquals(listOf(puzzle), catalog.getItemsByCategory(ItemCategory.OTHER))
        assertEquals(TestItems.APPLE, catalog.findItemById("apple"))
        assertEquals(puzzle, catalog.findItemById("custom-1"))
        assertNull(catalog.findItemById("custom-2"))

        custom = custom + cookie
        val food = catalog.getItemsByCategory(ItemCategory.FOOD)
        assertEquals(cookie, food.last())
        assertEquals(FakeItemCatalog().getItemsByCategory(ItemCategory.FOOD), food.dropLast(1))
        assertNotNull(catalog.findItemById("custom-2"))
    }

    @Test
    fun `the icon set is not empty and has no repeats`() {
        assertTrue(CustomItemIcons.ALL.size in 12..16)
        assertEquals(CustomItemIcons.ALL.size, CustomItemIcons.ALL.toSet().size)
    }
}
