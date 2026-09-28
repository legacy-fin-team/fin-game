package com.legacy.fingame

import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestKind
import com.legacy.fingame.game.quests.QuestReader
import com.legacy.fingame.game.stats.StatKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Квесты так, как их читает игра из XML: полный квест со всеми частями, необязательные части по
 * умолчанию и квесты со сломанными данными, которые отбрасываются целиком, не задевая соседей.
 */
class QuestReaderTest {

    /** Исправный квест, который кладётся рядом со сломанным: он должен прочитаться всегда. */
    private val goodQuest = """
        <quest id="good" title="Хороший" kind="random" start="only">
            <node id="only">
                <text>Текст.</text>
                <option label="Ок" next="end"><result>Готово.</result></option>
            </node>
        </quest>
    """

    /**
     * @param quests теги `<quest>` целиком.
     * @return Квесты, прочитанные из файла, в котором лежат ровно эти теги.
     */
    private fun read(vararg quests: String): List<Quest> {
        val xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<quests>\n" +
            quests.joinToString("\n") + "\n</quests>"
        return QuestReader().readQuests(xml.byteInputStream())
    }

    @Test
    fun `reads a quest with all its parts`() {
        val quests = read(
            """
            <quest id="picnic" title="Пикник" kind="player" progress="true" min-balance="100"
                   start="food" image="quests/picnic.webp">
                <description>Позови друзей
                    на пикник.</description>
                <node id="food" delay-minutes="1">
                    <text>Что взять из еды?</text>
                    <option label="Фрукты" next="games" money="-40" progress="40">
                        <result>Вкусно и полезно.</result>
                        <effect stat="hunger" value="20" />
                        <effect stat="health" value="5" />
                    </option>
                    <option label="Ничего" next="games">
                        <result>Все голодные.</result>
                    </option>
                </node>
                <node id="games">
                    <text>Во что играем?</text>
                    <option label="Мяч" next="end" progress="60">
                        <result>Играли до заката!</result>
                    </option>
                </node>
            </quest>
            """
        )

        val picnic = quests.single()
        assertEquals("picnic", picnic.id)
        assertEquals("Пикник", picnic.title)
        assertEquals("Позови друзей на пикник.", picnic.description)
        assertEquals(QuestKind.PLAYER, picnic.kind)
        assertTrue(picnic.hasProgress)
        assertEquals(100, picnic.minBalance)
        assertEquals("food", picnic.firstNodeId)
        assertEquals("quests/picnic.webp", picnic.imagePath)
        assertEquals(listOf("food", "games"), picnic.nodes.keys.toList())

        val food = picnic.node("food")!!
        assertEquals("Что взять из еды?", food.text)
        assertEquals(1, food.delayMinutes)
        assertNull(food.imagePath)
        assertEquals(listOf("Фрукты", "Ничего"), food.options.map { it.label })

        val fruit = food.options[0]
        assertEquals("Вкусно и полезно.", fruit.resultText)
        assertEquals("games", fruit.nextNodeId)
        assertEquals(mapOf(StatKind.HUNGER to 20, StatKind.HEALTH to 5), fruit.statEffects)
        assertEquals(-40, fruit.moneyDelta)
        assertEquals(40, fruit.progressDelta)

        val nothing = food.options[1]
        assertEquals(emptyMap<StatKind, Int>(), nothing.statEffects)
        assertEquals(0, nothing.moneyDelta)
        assertEquals(0, nothing.progressDelta)

        val games = picnic.node("games")!!
        assertEquals(0, games.delayMinutes)
        assertEquals(Quest.END_NODE, games.options.single().nextNodeId)

        assertEquals(2, picnic.stepCount)
        assertEquals(1, picnic.stepNumberOf("food"))
        assertEquals(2, picnic.stepNumberOf("games"))
        assertEquals(0, picnic.stepNumberOf("nowhere"))
    }

    @Test
    fun `optional parts read as their defaults`() {
        val quest = read(goodQuest).single()

        assertEquals(QuestKind.RANDOM, quest.kind)
        assertFalse(quest.hasProgress)
        assertEquals(0, quest.minBalance)
        assertEquals("", quest.description)
        assertNull(quest.imagePath)
        assertEquals(0, quest.node("only")!!.delayMinutes)
        assertTrue(quest.repeatable)
        assertEquals(Quest.DEFAULT_COOLDOWN_MINUTES, quest.cooldownMinutes)
        assertEquals(0, quest.stageDelayMinutes)
    }

    @Test
    fun `repeatable, cooldown and stage delay read from their attributes`() {
        val quest = read(
            """
            <quest id="piggy" title="Копилка" kind="player" start="a" repeatable="false"
                   cooldown-minutes="180" stage-delay-minutes="2">
                <node id="a">
                    <text>Текст.</text>
                    <option label="Ок" next="end"><result>Готово.</result></option>
                </node>
            </quest>
            """
        ).single()

        assertFalse(quest.repeatable)
        assertEquals(180, quest.cooldownMinutes)
        assertEquals(2, quest.stageDelayMinutes)
    }

    @Test
    fun `a node without its own delay falls back to the quest's stage delay`() {
        val quests = read(
            """
            <quest id="staged" title="Этапы" kind="player" start="a" stage-delay-minutes="3">
                <node id="a">
                    <text>Текст.</text>
                    <option label="Дальше" next="b"><result>Готово.</result></option>
                </node>
                <node id="b" delay-minutes="7">
                    <text>Текст.</text>
                    <option label="Ок" next="end"><result>Готово.</result></option>
                </node>
            </quest>
            """
        )

        val quest = quests.single()
        // У узла "a" нет своей задержки — она берётся из stage-delay-minutes квеста.
        assertEquals(3, quest.node("a")!!.delayMinutes)
        // У узла "b" задержка своя — она главнее квестовой.
        assertEquals(7, quest.node("b")!!.delayMinutes)
    }

    @Test
    fun `an improper repeatable, cooldown or stage delay drops the quest`() {
        val quests = read(
            """
            <quest id="bad-repeatable" title="А" kind="player" start="a" repeatable="maybe">
                <node id="a"><text>Т.</text><option label="Ок" next="end"><result>Г.</result></option></node>
            </quest>
            """,
            """
            <quest id="bad-cooldown" title="Б" kind="player" start="a" cooldown-minutes="-1">
                <node id="a"><text>Т.</text><option label="Ок" next="end"><result>Г.</result></option></node>
            </quest>
            """,
            """
            <quest id="bad-stage-delay" title="В" kind="player" start="a" stage-delay-minutes="слова">
                <node id="a"><text>Т.</text><option label="Ок" next="end"><result>Г.</result></option></node>
            </quest>
            """,
            goodQuest
        )

        assertEquals(listOf("good"), quests.map { it.id })
    }

    @Test
    fun `a missing picture reads as none`() {
        val quests = read(
            """
            <quest id="blank" title="Пустая картинка" kind="player" start="a" image="  ">
                <node id="a" image="">
                    <text>Текст.</text>
                    <option label="Ок" next="end"><result>Готово.</result></option>
                </node>
            </quest>
            """
        )

        assertNull(quests.single().imagePath)
        assertNull(quests.single().node("a")!!.imagePath)
    }

    @Test
    fun `a node without options drops its quest and only its quest`() {
        val quests = read(
            """
            <quest id="broken" title="Сломанный" kind="player" start="a">
                <node id="a"><text>Тупик.</text></node>
            </quest>
            """,
            goodQuest
        )

        assertEquals(listOf("good"), quests.map { it.id })
    }

    @Test
    fun `an option leading to a node the quest does not have drops the quest`() {
        val quests = read(
            """
            <quest id="broken" title="Сломанный" kind="player" start="a">
                <node id="a">
                    <text>Куда?</text>
                    <option label="Туда" next="nowhere"><result>Никуда.</result></option>
                </node>
            </quest>
            """,
            goodQuest
        )

        assertEquals(listOf("good"), quests.map { it.id })
    }

    @Test
    fun `a quest starting at a node it does not have is dropped`() {
        val quests = read(
            """
            <quest id="broken" title="Сломанный" kind="player" start="missing">
                <node id="a">
                    <text>Текст.</text>
                    <option label="Ок" next="end"><result>Готово.</result></option>
                </node>
            </quest>
            """,
            goodQuest
        )

        assertEquals(listOf("good"), quests.map { it.id })
    }

    @Test
    fun `a quest of an unknown kind is dropped`() {
        val quests = read(
            """
            <quest id="broken" title="Сломанный" kind="daily" start="a">
                <node id="a">
                    <text>Текст.</text>
                    <option label="Ок" next="end"><result>Готово.</result></option>
                </node>
            </quest>
            """,
            goodQuest
        )

        assertEquals(listOf("good"), quests.map { it.id })
    }

    @Test
    fun `a negative delay or a non-number drops the quest`() {
        val quests = read(
            """
            <quest id="negative" title="Минус" kind="player" start="a">
                <node id="a" delay-minutes="-1">
                    <text>Текст.</text>
                    <option label="Ок" next="end"><result>Готово.</result></option>
                </node>
            </quest>
            """,
            """
            <quest id="words" title="Слова" kind="player" start="a">
                <node id="a">
                    <text>Текст.</text>
                    <option label="Ок" next="end" money="много"><result>Готово.</result></option>
                </node>
            </quest>
            """,
            goodQuest
        )

        assertEquals(listOf("good"), quests.map { it.id })
    }

    @Test
    fun `a node cannot be called end`() {
        val quests = read(
            """
            <quest id="broken" title="Сломанный" kind="player" start="end">
                <node id="end">
                    <text>Текст.</text>
                    <option label="Ок" next="end"><result>Готово.</result></option>
                </node>
            </quest>
            """,
            goodQuest
        )

        assertEquals(listOf("good"), quests.map { it.id })
    }

    @Test
    fun `an option without a result drops the quest`() {
        val quests = read(
            """
            <quest id="broken" title="Сломанный" kind="player" start="a">
                <node id="a">
                    <text>Текст.</text>
                    <option label="Ок" next="end" />
                </node>
            </quest>
            """,
            goodQuest
        )

        assertEquals(listOf("good"), quests.map { it.id })
    }

    @Test
    fun `the second quest with the same id is dropped`() {
        val quests = read(
            goodQuest,
            """
            <quest id="good" title="Двойник" kind="player" start="b">
                <node id="b">
                    <text>Текст.</text>
                    <option label="Ок" next="end"><result>Готово.</result></option>
                </node>
            </quest>
            """
        )

        assertEquals("Хороший", quests.single().title)
    }

    @Test
    fun `an effect on an unknown stat is skipped, the quest is kept`() {
        val quests = read(
            """
            <quest id="kept" title="Остался" kind="player" start="a">
                <node id="a">
                    <text>Текст.</text>
                    <option label="Ок" next="end">
                        <result>Готово.</result>
                        <effect stat="luck" value="10" />
                        <effect stat="pleasure" value="-5" />
                    </option>
                </node>
            </quest>
            """
        )

        val option = quests.single().node("a")!!.options.single()
        assertEquals(mapOf(StatKind.PLEASURE to -5), option.statEffects)
    }
}
