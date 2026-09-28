package com.legacy.fingame

import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestBoard
import com.legacy.fingame.game.quests.QuestProgress
import com.legacy.fingame.game.quests.QuestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Какие карточки видит игрок на экране квестов и в каком порядке. */
class QuestBoardTest {

    private val now = FakeGameClock.DEFAULT_MILLIS

    @Test
    fun `player quests are listed from the start, random ones once they came up`() {
        assertEquals(
            listOf("picnic", "piggy_bank", "ice_cream"),
            QuestBoard.entriesOf(TestQuests.CATALOG, emptyList()).map { it.quest.id }
        )

        val withWallet = QuestBoard.entriesOf(
            TestQuests.CATALOG,
            listOf(QuestProgress("lost_wallet", "found", now))
        )
        assertEquals(
            listOf("lost_wallet", "picnic", "piggy_bank", "ice_cream"),
            withWallet.map { it.quest.id }
        )
    }

    @Test
    fun `running quests come first, then the ones to take, then the finished`() {
        val quests = listOf(
            QuestProgress("picnic", Quest.END_NODE, now, progress = 80, status = QuestStatus.FINISHED),
            QuestProgress("ice_cream", "shop", now)
        )

        assertEquals(
            listOf("ice_cream", "piggy_bank", "picnic"),
            QuestBoard.entriesOf(TestQuests.CATALOG, quests).map { it.quest.id }
        )
    }

    @Test
    fun `an entry carries the saved progress of its quest`() {
        val running = QuestProgress("picnic", "food", now)

        val entries = QuestBoard.entriesOf(TestQuests.CATALOG, listOf(running))

        assertEquals(running, entries.single { it.quest.id == "picnic" }.progress)
        assertNull(entries.single { it.quest.id == "piggy_bank" }.progress)
    }

    @Test
    fun `progress of a quest the data no longer has is not listed`() {
        val entries = QuestBoard.entriesOf(
            TestQuests.CATALOG,
            listOf(QuestProgress("gone", "somewhere", now))
        )

        assertEquals(listOf("picnic", "piggy_bank", "ice_cream"), entries.map { it.quest.id })
    }
}
