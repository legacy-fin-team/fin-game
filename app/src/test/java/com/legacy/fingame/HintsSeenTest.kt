package com.legacy.fingame

import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.hints.HintKeys
import com.legacy.fingame.utils.PlayerPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Какие подсказки к экранам игрок уже закрыл: [com.legacy.fingame.game.GameViewModel.markHintSeen],
 * [com.legacy.fingame.game.GameViewModel.resetHints], сохранение набора между запусками и сброс
 * прогресса.
 */
class HintsSeenTest {

    @Test
    fun `a new player has seen no hints`() {
        val vm = testGameViewModel()

        assertEquals(emptySet<String>(), vm.state.value.hintsSeen)
    }

    @Test
    fun `a closed hint is remembered in the state and in the store`() {
        val store = FakePlayerStateStore()
        val vm = testGameViewModel(store = store)

        vm.markHintSeen(HintKeys.SHOP)

        assertEquals(setOf(HintKeys.SHOP), vm.state.value.hintsSeen)
        assertEquals(setOf(HintKeys.SHOP), store.state.hintsSeen)
    }

    @Test
    fun `closed hints come back after a restart`() {
        val store = FakePlayerStateStore()
        val vm = testGameViewModel(store = store)
        vm.markHintSeen(HintKeys.PET_SELECT)
        vm.markHintSeen(HintKeys.HOME)

        val restarted = testGameViewModel(store = store)

        assertEquals(setOf(HintKeys.PET_SELECT, HintKeys.HOME), restarted.state.value.hintsSeen)
    }

    @Test
    fun `closing the same hint twice changes nothing`() {
        val store = FakePlayerStateStore()
        val vm = testGameViewModel(store = store)
        vm.markHintSeen(HintKeys.LOG)
        val saved = store.state

        vm.markHintSeen(HintKeys.LOG)

        assertEquals(setOf(HintKeys.LOG), vm.state.value.hintsSeen)
        // Второй раз в хранилище ничего не пишется: сохранённое состояние — тот же объект.
        assertTrue(saved === store.state)
    }

    @Test
    fun `resetting hints brings every one of them back, the rest of the game stays`() {
        val store = FakePlayerStateStore(
            PlayerState(
                selection = AnimalSelection(animalId = "cat", variantId = "grey"),
                balance = 77,
                hintsSeen = setOf(HintKeys.HOME, HintKeys.SHOP, HintKeys.BUDGET)
            )
        )
        val vm = testGameViewModel(store = store)

        vm.resetHints()

        assertEquals(emptySet<String>(), vm.state.value.hintsSeen)
        assertEquals(emptySet<String>(), store.state.hintsSeen)
        assertEquals(77, vm.state.value.balance)
        assertEquals(AnimalSelection(animalId = "cat", variantId = "grey"), vm.state.value.selection)
    }

    @Test
    fun `a progress reset clears the hints, so the pet selection hint shows again`() {
        val store = FakePlayerStateStore(
            PlayerState(
                selection = AnimalSelection(animalId = "cat", variantId = "grey"),
                hintsSeen = HintKeys.ALL.toSet()
            )
        )
        val vm = testGameViewModel(store = store)

        vm.resetProgress()

        assertEquals(emptySet<String>(), vm.state.value.hintsSeen)
        assertEquals(emptySet<String>(), store.state.hintsSeen)
        assertEquals(
            HintKeys.PET_SELECT,
            HintKeys.pending(
                hasPet = vm.state.value.selection != null,
                screen = vm.state.value.screen,
                seen = vm.state.value.hintsSeen
            )
        )
    }

    @Test
    fun `hints are saved under a key of their own, the old welcome flag is dropped`() {
        assertTrue("hints_seen" in PlayerPreferences.LIVE_KEYS)
        assertTrue("onboarding_seen" in PlayerPreferences.RETIRED_KEYS)
        assertFalse("onboarding_seen" in PlayerPreferences.LIVE_KEYS)
    }
}
