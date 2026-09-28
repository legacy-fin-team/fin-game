package com.legacy.fingame.game.help

import android.content.Context

/**
 * The help glossary, read once out of `assets/data/help.xml` at startup — mirrors
 * [com.legacy.fingame.game.items.ItemRegistry].
 *
 * @param context current local application context. Used to get access to the app's assets.
 */
class HelpRegistry(context: Context) {

    private val entries: List<HelpEntry> = context.assets.open("data/help.xml").use { inputStream ->
        HelpReader().readEntries(inputStream)
    }

    /** @return Every term of the glossary, in the order the help screen lists them. */
    fun getEntries(): List<HelpEntry> = entries
}
