package com.legacy.fingame

import com.legacy.fingame.game.Screen
import com.legacy.fingame.game.hints.Hint
import com.legacy.fingame.game.hints.HintKeys
import com.legacy.fingame.game.hints.HintReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Подсказки к экранам: как они читаются из XML, что файл, с которым выходит игра, полон, и какая
 * подсказка показывается на каком экране.
 */
class HintReaderTest {

    private fun readHints(xml: String): List<Hint> =
        HintReader().readHints(xml.trimIndent().byteInputStream())

    /** @return Подсказки из настоящего `data/hints.xml`, прочитанные так же, как при запуске. */
    private fun readShippedHints(): List<Hint> {
        val file = listOf(
            File("src/main/assets/data/hints.xml"),
            File("app/src/main/assets/data/hints.xml")
        ).firstOrNull { it.exists() }
        assertTrue("data/hints.xml is not in the assets", file != null)

        return file!!.inputStream().use { stream -> HintReader().readHints(stream) }
    }

    @Test
    fun `reads a hint's key, icon, title and paragraphs`() {
        val hints = readHints(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <hints>
                <hint key="shop" icon="🛒" title="Магазин">
                    <p>Первый абзац.</p>
                    <p>Второй абзац.</p>
                </hint>
            </hints>
            """
        )

        assertEquals(
            listOf(
                Hint(
                    key = "shop",
                    icon = "🛒",
                    title = "Магазин",
                    paragraphs = listOf("Первый абзац.", "Второй абзац.")
                )
            ),
            hints
        )
    }

    @Test
    fun `line breaks inside a paragraph become single spaces`() {
        val hints = readHints(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <hints>
                <hint key="log" title="Журнал">
                    <p>Здесь записано
                        всё.</p>
                </hint>
            </hints>
            """
        )

        assertEquals(listOf("Здесь записано всё."), hints.single().paragraphs)
        assertEquals("", hints.single().icon)
    }

    @Test
    fun `a hint without a key, a title or any text is dropped, the rest is read`() {
        val hints = readHints(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <hints>
                <hint title="Без ключа"><p>Текст.</p></hint>
                <hint key="no_title"><p>Текст.</p></hint>
                <hint key="no_text" title="Пусто"><p>   </p></hint>
                <hint key="fine" title="Хорошая"><p>Текст.</p></hint>
                <hint key="fine" title="Повтор"><p>Другой текст.</p></hint>
            </hints>
            """
        )

        assertEquals(listOf("fine"), hints.map { it.key })
        assertEquals("Хорошая", hints.single().title)
    }

    @Test
    fun `the shipped data has every screen's hint, none of them blank or too long`() {
        val hints = readShippedHints()

        assertEquals(HintKeys.ALL.toSet(), hints.map { it.key }.toSet())
        assertEquals(hints.size, hints.map { it.key }.distinct().size)
        hints.forEach { hint ->
            assertTrue("'${hint.key}' has a blank title", hint.title.isNotBlank())
            assertTrue("'${hint.key}' has a blank icon", hint.icon.isNotBlank())
            assertTrue(
                "'${hint.key}' must have one to three paragraphs",
                hint.paragraphs.size in 1..3
            )
            hint.paragraphs.forEach { paragraph ->
                assertTrue("'${hint.key}' has a blank paragraph", paragraph.isNotBlank())
                // Окно подсказки маленькое и читает его ребёнок: абзац — пара предложений.
                assertTrue(
                    "'${hint.key}' has a paragraph longer than 260 chars",
                    paragraph.length <= 260
                )
            }
        }
    }

    @Test
    fun `the home hint names the pet's real stat bars and points at the help screen`() {
        val home = readShippedHints().single { it.key == HintKeys.HOME }
        val text = home.paragraphs.joinToString(" ")

        listOf("Здоровье", "Голод", "Удовольствие", "Помощь").forEach { word ->
            assertTrue("home hint does not mention «$word»", word in text)
        }
    }

    @Test
    fun `before a pet is picked, the pet selection hint is the one to show`() {
        assertEquals(
            HintKeys.PET_SELECT,
            HintKeys.pending(hasPet = false, screen = Screen.MAIN, seen = emptySet())
        )
        assertNull(
            HintKeys.pending(hasPet = false, screen = Screen.MAIN, seen = setOf(HintKeys.PET_SELECT))
        )
    }

    @Test
    fun `every game screen with a hint shows its own one until it is seen`() {
        val expected = mapOf(
            Screen.MAIN to HintKeys.HOME,
            Screen.SHOP to HintKeys.SHOP,
            Screen.INVENTORY to HintKeys.INVENTORY,
            Screen.BUDGET to HintKeys.BUDGET,
            Screen.LOG to HintKeys.LOG,
            Screen.QUESTS to HintKeys.QUESTS
        )
        expected.forEach { (screen, key) ->
            assertEquals(key, HintKeys.pending(hasPet = true, screen = screen, seen = emptySet()))
            assertNull(HintKeys.pending(hasPet = true, screen = screen, seen = setOf(key)))
        }
    }

    @Test
    fun `settings and help show no hint of their own`() {
        listOf(Screen.OPTIONS, Screen.HELP, Screen.ADULT_LOCK, Screen.ADULT_MODE).forEach { screen ->
            assertNull(HintKeys.pending(hasPet = true, screen = screen, seen = emptySet()))
        }
    }
}
