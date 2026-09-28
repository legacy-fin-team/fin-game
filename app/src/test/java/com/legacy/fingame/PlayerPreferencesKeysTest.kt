package com.legacy.fingame

import com.legacy.fingame.game.stats.StatKind
import com.legacy.fingame.utils.PlayerPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Имена, под которыми состояние игрока лежит в настройках. Сами настройки в JVM-тестах не
 * работают, но списки ключей — обычные строки, и их можно держать в узде здесь.
 */
class PlayerPreferencesKeysTest {

    @Test
    fun `no key is both written and retired`() {
        // Отставные ключи снимаются в той же транзакции, в которой пишутся живые, и снимаются
        // последними. Ключ, попавший в оба списка, не пережил бы ни одного запуска: его бы
        // записали и тут же стёрли, а игрок увидел бы ноль там, где были его деньги.
        val both = PlayerPreferences.LIVE_KEYS.intersect(PlayerPreferences.RETIRED_KEYS.toSet())

        assertEquals(emptySet<String>(), both)
    }

    @Test
    fun `a key is written under one name only`() {
        assertEquals(PlayerPreferences.LIVE_KEYS.size, PlayerPreferences.LIVE_KEYS.toSet().size)
        assertEquals(
            PlayerPreferences.RETIRED_KEYS.size,
            PlayerPreferences.RETIRED_KEYS.toSet().size
        )
    }

    @Test
    fun `no key collides with the ones the stat bars take`() {
        val statKeys = StatKind.entries.map { PlayerPreferences.KEY_STAT_PREFIX + it.xmlName }

        assertTrue(PlayerPreferences.LIVE_KEYS.none { it in statKeys })
        assertTrue(PlayerPreferences.RETIRED_KEYS.none { it in statKeys })
    }

    @Test
    fun `the quest keys are among the live ones`() {
        // Квесты, момент последнего взгляда на экран квестов и момент последнего случайного
        // квеста пишутся при каждом сохранении; ключ вне этого списка не пережил бы запуска.
        assertTrue(PlayerPreferences.KEY_QUESTS in PlayerPreferences.LIVE_KEYS)
        assertTrue(PlayerPreferences.KEY_QUESTS_SEEN_AT in PlayerPreferences.LIVE_KEYS)
        assertTrue(PlayerPreferences.KEY_LAST_RANDOM_QUEST_AT in PlayerPreferences.LIVE_KEYS)
    }

    @Test
    fun `the goals are written under a key of their own`() {
        assertTrue("goals" in PlayerPreferences.LIVE_KEYS)
    }

    @Test
    fun `the adult mode histories are among the live keys`() {
        // История бюджета и журнал квестов пишутся при каждом сохранении, как журнал денег.
        assertTrue(PlayerPreferences.KEY_BUDGET_HISTORY in PlayerPreferences.LIVE_KEYS)
        assertTrue(PlayerPreferences.KEY_QUEST_LOG in PlayerPreferences.LIVE_KEYS)
        assertEquals("budget_history", PlayerPreferences.KEY_BUDGET_HISTORY)
        assertEquals("quest_log", PlayerPreferences.KEY_QUEST_LOG)
    }

    @Test
    fun `the custom items are among the live keys`() {
        // Свои предметы взрослого пишутся при каждом сохранении одной строкой.
        assertTrue(PlayerPreferences.KEY_CUSTOM_ITEMS in PlayerPreferences.LIVE_KEYS)
        assertEquals("custom_items", PlayerPreferences.KEY_CUSTOM_ITEMS)
    }

    @Test
    fun `the custom quests are among the live keys`() {
        // Свои квесты взрослого пишутся при каждом сохранении одной строкой.
        assertTrue(PlayerPreferences.KEY_CUSTOM_QUESTS in PlayerPreferences.LIVE_KEYS)
        assertEquals("custom_quests", PlayerPreferences.KEY_CUSTOM_QUESTS)
    }

    @Test
    fun `the reward usage log and its last look are among the live keys`() {
        assertTrue(PlayerPreferences.KEY_REWARD_USAGE_LOG in PlayerPreferences.LIVE_KEYS)
        assertTrue(PlayerPreferences.KEY_REWARD_USAGE_SEEN_AT in PlayerPreferences.LIVE_KEYS)
        assertEquals("reward_usage_log", PlayerPreferences.KEY_REWARD_USAGE_LOG)
        assertEquals("reward_usage_seen_at", PlayerPreferences.KEY_REWARD_USAGE_SEEN_AT)
    }
}
