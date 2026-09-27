package com.legacy.fingame

import com.legacy.fingame.game.OnboardingGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Whether the first-launch onboarding window shows, and that closing it is remembered — the two
 * things [com.legacy.fingame.utils.OnboardingPreferences] cannot be checked for in a JVM test (see
 * [PlayerPreferencesKeysTest]), so the logic is tested against [FakeOnboardingStore] instead.
 */
class OnboardingGateTest {

    @Test
    fun `a player who has never seen the window is shown it`() {
        val gate = OnboardingGate(FakeOnboardingStore(seen = false))

        assertTrue(gate.isVisible.value)
    }

    @Test
    fun `a player who has already seen the window is not shown it again`() {
        val gate = OnboardingGate(FakeOnboardingStore(seen = true))

        assertFalse(gate.isVisible.value)
    }

    @Test
    fun `dismissing the window hides it`() {
        val gate = OnboardingGate(FakeOnboardingStore(seen = false))

        gate.dismiss()

        assertFalse(gate.isVisible.value)
    }

    @Test
    fun `dismissing the window remembers it for the next launch`() {
        val store = FakeOnboardingStore(seen = false)
        val gate = OnboardingGate(store)

        gate.dismiss()

        assertTrue(store.hasSeenOnboarding())
        // A fresh gate, built the way the next launch would build one, over the very same store.
        assertFalse(OnboardingGate(store).isVisible.value)
    }

    @Test
    fun `dismissing twice writes to the store only once`() {
        val store = FakeOnboardingStore(seen = false)
        val gate = OnboardingGate(store)

        gate.dismiss()
        gate.dismiss()

        assertEquals(1, store.markCalls)
    }

    @Test
    fun `dismissing a window already marked seen elsewhere writes nothing`() {
        // A second gate over a store another one already dismissed, e.g. two screens sharing one
        // player's preferences: the window was never visible to this gate, so its own dismiss has
        // nothing to do.
        val store = FakeOnboardingStore(seen = true)
        val gate = OnboardingGate(store)

        gate.dismiss()

        assertEquals(0, store.markCalls)
    }

    @Test
    fun `a progress reset shows the window again`() {
        val store = FakeOnboardingStore(seen = false)
        val gate = OnboardingGate(store)
        gate.dismiss()

        gate.onProgressReset()

        assertTrue(gate.isVisible.value)
    }

    @Test
    fun `a progress reset clears the store, so the next launch shows the window too`() {
        val store = FakeOnboardingStore(seen = false)
        val gate = OnboardingGate(store)
        gate.dismiss()

        gate.onProgressReset()

        assertFalse(store.hasSeenOnboarding())
        // A fresh gate, built the way the next launch would build one, over the very same store.
        assertTrue(OnboardingGate(store).isVisible.value)
    }

    @Test
    fun `a progress reset shows the window even when it was never dismissed`() {
        // The player resets progress right from the still-open onboarding window itself — an edge
        // case, but one that should leave the window showing rather than throwing.
        val store = FakeOnboardingStore(seen = false)
        val gate = OnboardingGate(store)

        gate.onProgressReset()

        assertTrue(gate.isVisible.value)
        assertFalse(store.hasSeenOnboarding())
    }
}
