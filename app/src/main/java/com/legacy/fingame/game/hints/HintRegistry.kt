package com.legacy.fingame.game.hints

import android.content.Context

/**
 * Подсказки к экранам, прочитанные один раз из `assets/data/hints.xml` при запуске — как
 * [com.legacy.fingame.game.help.HelpRegistry].
 *
 * @param context контекст приложения, нужен для доступа к assets.
 */
class HintRegistry(context: Context) {

    private val hints: List<Hint> = context.assets.open("data/hints.xml").use { inputStream ->
        HintReader().readHints(inputStream)
    }

    /** @return Все подсказки в порядке файла — в нём их и листают на экране «Помощь». */
    fun getHints(): List<Hint> = hints

    /**
     * @param key ключ экрана, одно из [HintKeys.ALL].
     * @return Подсказка к этому экрану, или null, когда её нет в данных.
     */
    fun find(key: String): Hint? = hints.find { it.key == key }
}
