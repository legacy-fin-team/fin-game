package com.legacy.fingame.utils

import android.content.Context
import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.PlayerStateStore
import com.legacy.fingame.game.animals.AnimalSelection

/**
 * [PlayerStateStore] backed by SharedPreferences: this is what makes the player's game survive
 * the app being closed and the process being killed.
 *
 * Every value is stored under a key of its own rather than as one blob, so a state that grew a
 * new field still reads back on a device that saved it before the field existed.
 *
 * @param context current local application context. Used to get access to SharedPreferences.
 */
class PlayerPreferences(context: Context) : PlayerStateStore {

    companion object {
        private const val PREFERENCES_NAME = "player"
        private const val KEY_ANIMAL_ID = "selected_animal_id"
        private const val KEY_ANIMAL_VARIANT_ID = "selected_animal_variant_id"
        private const val KEY_SUB_LOCATION_INDEX = "sub_location_index"
    }

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /**
     * @return The state saved by [save], with everything that was never saved — or was saved
     * empty — left at its default. A pet is only restored when both of its ids are there, since
     * one id without the other resolves to no animal at all.
     */
    override fun load(): PlayerState {
        val defaults = PlayerState()

        val animalId = preferences.getString(KEY_ANIMAL_ID, null)
        val variantId = preferences.getString(KEY_ANIMAL_VARIANT_ID, null)
        val selection = if (animalId.isNullOrBlank() || variantId.isNullOrBlank()) {
            defaults.selection
        } else {
            AnimalSelection(animalId = animalId, variantId = variantId)
        }

        return PlayerState(
            selection = selection,
            subLocationIndex = preferences.getInt(KEY_SUB_LOCATION_INDEX, defaults.subLocationIndex)
        )
    }

    /**
     * Writes the whole state in a single edit, so a save can never leave half of it behind.
     *
     * @param state the state to remember for the next launch.
     */
    override fun save(state: PlayerState) {
        preferences.edit()
            .putString(KEY_ANIMAL_ID, state.selection?.animalId)
            .putString(KEY_ANIMAL_VARIANT_ID, state.selection?.variantId)
            .putInt(KEY_SUB_LOCATION_INDEX, state.subLocationIndex)
            .apply()
    }
}
