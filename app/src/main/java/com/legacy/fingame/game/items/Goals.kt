package com.legacy.fingame.game.items

/**
 * Одна цель игрока, сведённая с каталогом: товар в том варианте, на который он копит.
 *
 * @property item товар, как его знает каталог — с именем и ценой.
 * @property variantId вариант, отмеченный звёздочкой; см. [ItemSelection].
 */
data class GoalLine(
    val item: Item,
    val variantId: String
) {
    /** Товар и вариант одним значением — так цели лежат в состоянии игрока. */
    val selection: ItemSelection get() = ItemSelection(item.id, variantId)
}

/**
 * Превращает цели игрока в строки, которые показывает главный экран.
 *
 * Сами цели лежат в [com.legacy.fingame.game.GameUiState.goals] голыми id — этого хватает, чтобы их
 * сохранить; здесь id встречаются с каталогом и становятся товарами с именем и ценой.
 */
object Goals {

    /**
     * @param goals цели в том порядке, в каком игрок их отмечал.
     * @param catalog что игра знает о товарах.
     * @return Строки в том же порядке — порядок и есть смысл: карточки не прыгают, новая встаёт в
     * конец. Товар, которого больше нет, и вариант, которого у товара больше нет, пропускаются;
     * повтор цели — тоже: карусель целей различает карточки по цели, и две одинаковые сломали бы её.
     */
    fun linesOf(goals: List<ItemSelection>, catalog: ItemCatalog): List<GoalLine> =
        goals.distinct().mapNotNull { goal ->
            val item = catalog.findItemById(goal.itemId) ?: return@mapNotNull null
            if (goal.variantId !in item.variantIds) return@mapNotNull null
            GoalLine(item = item, variantId = goal.variantId)
        }
}
