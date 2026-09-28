package com.legacy.fingame.game.hints

import com.legacy.fingame.game.Screen

/**
 * Небольшая подсказка к одному экрану: показывается один раз при первом входе на него и лежит в
 * разделе «Подсказки по экранам» на экране «Помощь», где её можно перечитать.
 *
 * @property key какой экран описывает подсказка, одно из [HintKeys.ALL].
 * @property icon эмодзи над заголовком; украшение, TalkBack его не читает.
 * @property title заголовок окна подсказки.
 * @property paragraphs абзацы текста, от одного до трёх, простыми словами для 8–12 лет.
 */
data class Hint(
    val key: String,
    val icon: String,
    val title: String,
    val paragraphs: List<String>
)

/**
 * Ключи подсказок — те же, что атрибут `key` в `data/hints.xml`, — и правило, какую из них
 * показать прямо сейчас.
 */
object HintKeys {
    /** Экран выбора питомца: самое первое, что видит новый игрок. */
    const val PET_SELECT = "pet_select"

    /** Главный экран с питомцем и его шкалами. */
    const val HOME = "home"

    /** Магазин. */
    const val SHOP = "shop"

    /** Инвентарь. */
    const val INVENTORY = "inventory"

    /** Квесты. */
    const val QUESTS = "quests"

    /** Журнал изменений денег. */
    const val LOG = "log"

    /** Планирование бюджета. */
    const val BUDGET = "budget"

    /** Все ключи в том порядке, в каком игрок обычно встречает экраны. */
    val ALL: List<String> = listOf(PET_SELECT, HOME, SHOP, INVENTORY, QUESTS, LOG, BUDGET)

    /**
     * @param screen открытый экран игры (питомец уже выбран).
     * @return Ключ подсказки к этому экрану, или null, когда у экрана подсказки нет: у настроек,
     * помощи, замка и режима взрослого своих подсказок нет.
     */
    fun forScreen(screen: Screen): String? = when (screen) {
        Screen.MAIN -> HOME
        Screen.SHOP -> SHOP
        Screen.INVENTORY -> INVENTORY
        Screen.BUDGET -> BUDGET
        Screen.LOG -> LOG
        Screen.QUESTS -> QUESTS
        Screen.OPTIONS,
        Screen.ADULT_LOCK,
        Screen.ADULT_MODE,
        Screen.HELP -> null
    }

    /**
     * Какую подсказку показать прямо сейчас.
     *
     * @param hasPet выбран ли питомец; пока нет, игрок на экране выбора питомца.
     * @param screen открытый экран игры; до выбора питомца не важен.
     * @param seen ключи подсказок, которые игрок уже закрыл.
     * @return Ключ подсказки, которую пора показать, или null, когда показывать нечего.
     */
    fun pending(hasPet: Boolean, screen: Screen, seen: Set<String>): String? {
        val key = if (hasPet) forScreen(screen) else PET_SELECT
        return key?.takeUnless { it in seen }
    }
}
