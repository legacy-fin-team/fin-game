package com.legacy.fingame.utils

import android.content.Context
import com.legacy.fingame.game.OnboardingStore

/**
 * [OnboardingStore] backed by SharedPreferences, so the flag survives the app being closed and the
 * process being killed, exactly like the rest of the player's save.
 *
 * Kept in [PlayerPreferences.PREFERENCES_NAME] — the very same file [PlayerPreferences] itself
 * writes to — rather than a file of its own, since the flag is, in spirit, part of the player's
 * save even though it has no field of its own in [com.legacy.fingame.game.PlayerState]:
 * [PlayerPreferences.save] only ever rewrites the keys it knows about
 * ([PlayerPreferences.LIVE_KEYS]), so it neither touches nor clears this one, and a "reset
 * progress" is what calls [com.legacy.fingame.game.OnboardingGate.onProgressReset] to clear it in
 * step with everything [PlayerPreferences.save] does reset.
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

    override fun clearOnboardingSeen() {
        preferences.edit().remove(KEY_ONBOARDING_SEEN).apply()
    }
}
