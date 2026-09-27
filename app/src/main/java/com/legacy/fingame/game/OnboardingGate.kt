package com.legacy.fingame.game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Where the "has the player already seen the onboarding window" flag lives and is changed.
 *
 * Kept as an interface — mirroring [PlayerStateStore] — so [OnboardingGate] can be tested without
 * Android's SharedPreferences; see `OnboardingPreferences` for the real implementation.
 */
interface OnboardingStore {
    /** @return True once [markOnboardingSeen] has been called at least once before. */
    fun hasSeenOnboarding(): Boolean

    /** Remembers that the player closed the onboarding window, so it never shows again. */
    fun markOnboardingSeen()

    /**
     * Forgets that the player has ever seen the window, so [hasSeenOnboarding] answers `false`
     * again — called when the player's whole progress is reset, since a fresh start shows the
     * same welcome a genuinely new player gets.
     */
    fun clearOnboardingSeen()
}

/**
 * Decides whether the first-launch onboarding window — the one explaining the pet and the three
 * things money can be spent on — should be shown right now, and remembers the player's answer once
 * they close it.
 *
 * The window is shown exactly once per player: the very first time the app is ever launched, and
 * never again afterwards, including across restarts, since [store] survives the app being closed.
 * A player whose progress is reset is shown it again too — see [onProgressReset], called alongside
 * [com.legacy.fingame.game.GameViewModel.resetProgress] — since starting over from nothing is, as
 * far as this window is concerned, the same thing as being new.
 *
 * @param store where the flag is read from and written to.
 */
class OnboardingGate(private val store: OnboardingStore) {

    private val _isVisible = MutableStateFlow(!store.hasSeenOnboarding())

    /** Whether the onboarding window should be shown right now. */
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

    /**
     * Closes the window for good: from this call on, and on every launch after it,
     * [isVisible] is false.
     *
     * Calling this again once the window is already closed does nothing — in particular, it does
     * not write to [store] a second time.
     */
    fun dismiss() {
        if (!_isVisible.value) return
        store.markOnboardingSeen()
        _isVisible.value = false
    }

    /**
     * Treats the player as a brand new one again: called alongside
     * [com.legacy.fingame.game.GameViewModel.resetProgress], since that drops the pet, the money
     * and everything else the player had, and a player starting over from nothing should see the
     * same welcome a genuinely new player does.
     *
     * Unlike [dismiss], this always clears [store] and always shows the window again, whether or
     * not it was showing already — a reset makes the player new regardless of what this particular
     * gate happened to be displaying at the time.
     */
    fun onProgressReset() {
        store.clearOnboardingSeen()
        _isVisible.value = true
    }
}
