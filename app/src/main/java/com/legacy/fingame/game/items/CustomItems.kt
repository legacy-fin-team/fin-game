package com.legacy.fingame.game.items

import com.legacy.fingame.game.stats.StatKind

/** Пределы и правила своих предметов взрослого. */
object CustomItems {

    /** Больше своих предметов не хранится. */
    const val MAX = 50

    /** Длина названия. */
    const val NAME_MAX = 20

    const val PRICE_MIN = 1

    const val PRICE_MAX = 999

    const val EFFECT_MIN = -50

    const val EFFECT_MAX = 50

    /** Шаг кнопок «−/+» у эффектов. */
    const val EFFECT_STEP = 5

    /** С чего начинается id своего предмета: `custom-<момент>`. */
    const val ID_PREFIX = "custom-"

    /** Единственный вариант своего предмета. */
    const val VARIANT_ID = "default"

    /** Разделы, в которые взрослый может положить предмет, в порядке кнопок формы. */
    val CATEGORIES: List<ItemCategory> = listOf(
        ItemCategory.FOOD,
        ItemCategory.TOYS,
        ItemCategory.CLOTHES,
        ItemCategory.DECOR,
        ItemCategory.OTHER
    )

    /**
     * @param category раздел предмета.
     * @return Есть ли у предметов раздела эффекты: их едят или с ними играют. Одежда, декор и
     * «Другое» питомца не меняют.
     */
    fun hasEffects(category: ItemCategory): Boolean =
        category == ItemCategory.FOOD || category == ItemCategory.TOYS

    /** @return Свой ли это предмет взрослого. */
    fun isCustom(itemId: String): Boolean = itemId.startsWith(ID_PREFIX)

    /**
     * @param text что ввёл взрослый.
     * @return Текст без служебных знаков кодека и лишних пробелов по краям.
     */
    internal fun clean(text: String): String =
        text.filterNot { it == CustomItemsCodec.RECORD_SEPARATOR || it == CustomItemsCodec.FIELD_SEPARATOR }
            .trim()
}

/**
 * Иконки, из которых взрослый выбирает картинку своего предмета: только готовые пиксельные спрайты
 * игры, чтобы свой предмет не выбивался из стиля. Пути — от `assets/textures/`.
 */
object CustomItemIcons {
    val ALL: List<String> = listOf(
        "shop/categories/other.webp",
        "shop/categories/food.webp",
        "shop/categories/toys.webp",
        "shop/categories/clothes.webp",
        "shop/categories/decor.webp",
        "ui/coin.webp",
        "ui/star-on.webp",
        "ui/stats/health.webp",
        "ui/stats/hunger.webp",
        "ui/stats/pleasure.webp",
        "ui/budget.webp",
        "ui/quests.webp",
        "ui/shop.webp",
        "ui/inventory.webp",
        "ui/log.webp"
    )
}

/**
 * То, что взрослый набрал в форме «Новый предмет».
 *
 * @property name название.
 * @property price цена в монетах.
 * @property category раздел магазина.
 * @property effects что предмет делает с питомцем; учитывается только у еды и игрушек.
 * @property iconPath иконка из [CustomItemIcons.ALL].
 */
data class CustomItemDraft(
    val name: String = "",
    val price: Int = 0,
    val category: ItemCategory = ItemCategory.OTHER,
    val effects: Map<StatKind, Int> = emptyMap(),
    val iconPath: String = CustomItemIcons.ALL.first()
) {
    /**
     * @param existingCount сколько своих предметов уже есть.
     * @return Что не так, короткими фразами для формы; пусто, когда предмет можно сохранить.
     */
    fun validate(existingCount: Int = 0): List<String> {
        val errors = mutableListOf<String>()
        val cleanName = CustomItems.clean(name)
        if (cleanName.isEmpty()) errors += "Впиши название"
        if (cleanName.length > CustomItems.NAME_MAX) {
            errors += "Название — не длиннее ${CustomItems.NAME_MAX} букв"
        }
        if (price !in CustomItems.PRICE_MIN..CustomItems.PRICE_MAX) {
            errors += "Цена — от ${CustomItems.PRICE_MIN} до ${CustomItems.PRICE_MAX}"
        }
        if (CustomItems.hasEffects(category) &&
            effects.values.any { it !in CustomItems.EFFECT_MIN..CustomItems.EFFECT_MAX }
        ) {
            errors += "Эффект — от ${CustomItems.EFFECT_MIN} до +${CustomItems.EFFECT_MAX}"
        }
        if (iconPath.isBlank()) errors += "Выбери иконку"
        if (existingCount >= CustomItems.MAX) errors += "Своих предметов — не больше ${CustomItems.MAX}"
        return errors
    }
}

/**
 * Собирает свой предмет из черновика.
 *
 * @param draft что набрал взрослый.
 * @param id id нового предмета, `custom-<момент>`.
 * @return Предмет с одним вариантом и иконкой из черновика, или null, когда черновик не проходит
 * [CustomItemDraft.validate]. Нулевые эффекты не хранятся, а у одежды, декора и «Другого» их нет.
 */
fun customItemOf(draft: CustomItemDraft, id: String): Item? {
    if (draft.validate().isNotEmpty()) return null
    val effects = if (CustomItems.hasEffects(draft.category)) {
        draft.effects.filterValues { it != 0 }
    } else {
        emptyMap()
    }
    return Item(
        id = id,
        name = CustomItems.clean(draft.name),
        price = draft.price,
        category = draft.category,
        variantIds = listOf(CustomItems.VARIANT_ID),
        declaredEffects = effects,
        iconOverride = draft.iconPath
    )
}

/**
 * Свои предметы взрослого ([com.legacy.fingame.game.PlayerState.customItems]) одной строкой, по
 * образцу остальных кодеков: запись от записи — `\u001E`, поле от поля — `\u001F`.
 *
 * Поля записи, всегда шесть: id, название, цена, раздел, эффекты (`health=10;hunger=-5`), иконка.
 */
object CustomItemsCodec {

    const val RECORD_SEPARATOR = '\u001E'
    const val FIELD_SEPARATOR = '\u001F'

    private const val FIELDS_PER_RECORD = 6
    private const val EFFECT_SEPARATOR = ';'
    private const val EFFECT_VALUE_SEPARATOR = '='

    /**
     * @param items свои предметы.
     * @return Одна строка, не больше [CustomItems.MAX] записей; пустая, когда предметов нет.
     */
    fun encode(items: List<Item>): String =
        items.take(CustomItems.MAX).joinToString(RECORD_SEPARATOR.toString()) { item ->
            listOf(
                CustomItems.clean(item.id),
                CustomItems.clean(item.name),
                item.price.toString(),
                item.category.xmlName,
                item.declaredEffects.entries.joinToString(EFFECT_SEPARATOR.toString()) { (stat, value) ->
                    "${stat.xmlName}$EFFECT_VALUE_SEPARATOR$value"
                },
                CustomItems.clean(item.iconOverride.orEmpty())
            ).joinToString(FIELD_SEPARATOR.toString())
        }

    /**
     * Читает то, что написал [encode]. Запись, которая не разбирается, отбрасывается.
     *
     * @param raw строка из настроек, или null, когда её не было.
     * @return Предметы в сохранённом порядке, не больше [CustomItems.MAX].
     */
    fun decode(raw: String?): List<Item> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(RECORD_SEPARATOR)
            .mapNotNull(::decodeRecord)
            .distinctBy { it.id }
            .take(CustomItems.MAX)
    }

    private fun decodeRecord(record: String): Item? {
        val fields = record.split(FIELD_SEPARATOR)
        if (fields.size != FIELDS_PER_RECORD) return null
        val (id, name, priceText, categoryText, effectsText, iconPath) = fields
        val price = priceText.toIntOrNull() ?: return null
        val category = ItemCategory.fromString(categoryText) ?: return null
        if (id.isBlank() || name.isBlank()) return null
        return Item(
            id = id,
            name = name,
            price = price,
            category = category,
            variantIds = listOf(CustomItems.VARIANT_ID),
            declaredEffects = decodeEffects(effectsText),
            iconOverride = iconPath.ifBlank { CustomItemIcons.ALL.first() }
        )
    }

    private fun decodeEffects(text: String): Map<StatKind, Int> =
        text.split(EFFECT_SEPARATOR).mapNotNull { pair ->
            val parts = pair.split(EFFECT_VALUE_SEPARATOR)
            if (parts.size != 2) return@mapNotNull null
            val stat = StatKind.fromString(parts[0]) ?: return@mapNotNull null
            val value = parts[1].toIntOrNull() ?: return@mapNotNull null
            stat to value
        }.toMap()
}

private operator fun <T> List<T>.component6(): T = this[5]

/**
 * Что продаётся: предметы игры и, в конце каждого раздела, свои предметы взрослого.
 *
 * @param base предметы игры из данных.
 * @param custom свои предметы — читаются при каждом обращении, так что добавленный взрослым предмет
 * сразу на полке.
 */
class CompositeItemCatalog(
    private val base: ItemCatalog,
    private val custom: () -> List<Item>
) : ItemCatalog {

    override fun getItemsByCategory(category: ItemCategory): List<Item> =
        base.getItemsByCategory(category) + custom().filter { it.category == category }

    override fun findItemById(itemId: String): Item? =
        base.findItemById(itemId) ?: custom().find { it.id == itemId }
}
