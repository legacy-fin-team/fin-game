package com.legacy.fingame.utils

import android.content.Context
import com.legacy.fingame.game.animals.AnimalSelection

/**
 * Keeps the player's choices that have to survive app restarts, backed by SharedPreferences.
 *
 * Right now the only such choice is the animal the player picked on the first launch.
 *
 * @param context current local application context. Used to get access to SharedPreferences.
 */
class PlayerPreferences(context: Context) {

    companion object {
        private const val PREFERENCES_NAME = "player"
        private const val KEY_ANIMAL_ID = "selected_animal_id"
        private const val KEY_ANIMAL_VARIANT_ID = "selected_animal_variant_id"
    }

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /**
     * @return The [AnimalSelection] saved by [saveSelectedAnimal] or null if the player hasn't
     * picked an animal yet, i.e. this is the first launch.
     */
    fun getSelectedAnimal(): AnimalSelection? {
        val animalId = preferences.getString(KEY_ANIMAL_ID, null)
        val variantId = preferences.getString(KEY_ANIMAL_VARIANT_ID, null)

        if (animalId.isNullOrBlank() || variantId.isNullOrBlank()) {
            return null
        }

        return AnimalSelection(animalId = animalId, variantId = variantId)
    }

    /**
     * Saves the animal the player picked, so the selection screen isn't shown again.
     *
     * @param selection the animal and the variant the player picked.
     */
    fun saveSelectedAnimal(selection: AnimalSelection) {
        preferences.edit()
            .putString(KEY_ANIMAL_ID, selection.animalId)
            .putString(KEY_ANIMAL_VARIANT_ID, selection.variantId)
            .apply()
    }
}
