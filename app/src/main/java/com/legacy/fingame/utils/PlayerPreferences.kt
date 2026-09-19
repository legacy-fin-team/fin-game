package com.legacy.fingame.utils

import android.content.Context
import android.util.Log
import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.PlayerStateStore
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.items.ItemSelection

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
        private const val TAG = "PlayerPreferences"

        private const val PREFERENCES_NAME = "player"
        private const val KEY_ANIMAL_ID = "selected_animal_id"
        private const val KEY_ANIMAL_VARIANT_ID = "selected_animal_variant_id"
        private const val KEY_SUB_LOCATION_INDEX = "sub_location_index"
        private const val KEY_BALANCE = "balance"
        private const val KEY_LAST_DAILY_BONUS_DAY = "last_daily_bonus_day"
        private const val KEY_OWNED_ITEMS = "owned_items"

        /**
         * Separators of one owned-item record, stored as `itemId:variantId=count`: item and variant
         * ids come from the data files and hold neither of these characters.
         */
        private const val VARIANT_SEPARATOR = ':'
        private const val COUNT_SEPARATOR = '='
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
            subLocationIndex = preferences.getInt(KEY_SUB_LOCATION_INDEX, defaults.subLocationIndex),
            balance = preferences.getInt(KEY_BALANCE, defaults.balance),
            lastDailyBonusDay = preferences.getLong(
                KEY_LAST_DAILY_BONUS_DAY,
                defaults.lastDailyBonusDay
            ),
            owned = readOwned(defaults.owned)
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
            .putInt(KEY_BALANCE, state.balance)
            .putLong(KEY_LAST_DAILY_BONUS_DAY, state.lastDailyBonusDay)
            .putStringSet(KEY_OWNED_ITEMS, state.owned.map(::encodeOwned).toSet())
            .apply()
    }

    /**
     * Reads back what [save] wrote for [PlayerState.owned].
     *
     * A record that doesn't parse is dropped rather than failing the whole load: losing one line of
     * the inventory is a far smaller loss to the player than losing the pet and the money with it.
     *
     * @param defaults value to fall back to when nothing was ever saved.
     * @return The owned items, keyed by item and variant.
     */
    private fun readOwned(defaults: Map<ItemSelection, Int>): Map<ItemSelection, Int> {
        val records = preferences.getStringSet(KEY_OWNED_ITEMS, null) ?: return defaults

        val owned = mutableMapOf<ItemSelection, Int>()
        records.forEach { record ->
            val selection = decodeOwned(record)
            if (selection == null) {
                Log.e(TAG, "Dropped a malformed owned item record: '$record'")
            } else {
                owned[selection.first] = selection.second
            }
        }
        return owned.toMap()
    }

    /**
     * @param owned one entry of [PlayerState.owned].
     * @return The entry as a single string, in the `itemId:variantId=count` form.
     */
    private fun encodeOwned(owned: Map.Entry<ItemSelection, Int>): String =
        "${owned.key.itemId}$VARIANT_SEPARATOR${owned.key.variantId}$COUNT_SEPARATOR${owned.value}"

    /**
     * @param record a record written by [encodeOwned].
     * @return The item with the number of them owned, or null when the record is not in the
     * `itemId:variantId=count` form or holds no usable count.
     */
    private fun decodeOwned(record: String): Pair<ItemSelection, Int>? {
        val countSeparator = record.lastIndexOf(COUNT_SEPARATOR)
        if (countSeparator <= 0) return null

        val count = record.substring(countSeparator + 1).toIntOrNull()
        if (count == null || count <= 0) return null

        val selection = record.substring(0, countSeparator)
        val variantSeparator = selection.indexOf(VARIANT_SEPARATOR)
        if (variantSeparator <= 0 || variantSeparator == selection.lastIndex) return null

        return ItemSelection(
            itemId = selection.substring(0, variantSeparator),
            variantId = selection.substring(variantSeparator + 1)
        ) to count
    }
}
