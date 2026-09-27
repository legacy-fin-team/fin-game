package com.legacy.fingame.utils

import android.content.Context
import com.legacy.fingame.game.OnboardingStore

/**
 * [OnboardingStore] backed by SharedPreferences, so the flag survives the app being closed and the
 * process being killed, exactly like the rest of the player's save.
 *
 * Kept in [PlayerPreferences.PREFERENCES_NAME] — the very same file [PlayerPreferences] itself
 * writes to — on purpose: the flag has no field of its own in [com.legacy.fingame.game.PlayerState],
 * so it is not touched by [PlayerPreferences.save] or [PlayerPreferences.load] at all, but a
 * player whose save is wiped by a future "reset progress" should still be shown the window again,
 * as if they were new. Sharing the file is what makes that automatic instead of asking a reset
 * feature to also remember this key by name.
 *
 * @param context current local application context. Used to get access to SharedPreferences.
 */
class OnboardingPreferences(context: Context) : OnboardingStore {

    companion object {
        private const val KEY_ONBOARDING_SEEN = "onboarding_seen"
    }

    private val preferences =
        context.getSharedPreferences(PlayerPreferences.PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun hasSeenOnboarding(): Boolean =
        preferences.getBoolean(KEY_ONBOARDING_SEEN, false)

    override fun markOnboardingSeen() {
        preferences.edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply()
    }
}
