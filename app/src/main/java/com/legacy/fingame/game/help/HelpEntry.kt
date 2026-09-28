package com.legacy.fingame.game.help

/**
 * One term of the in-game glossary shown on the help screen: a word the player runs into elsewhere
 * in the game, and a short, plain explanation of it written for an 8-to-12-year-old.
 *
 * @property id id of the term, as the data names it; not shown to the player, only used to tell two
 * entries apart.
 * @property title the term itself, e.g. "Бюджет".
 * @property text one or two short sentences explaining it.
 */
data class HelpEntry(
    val id: String,
    val title: String,
    val text: String
)
