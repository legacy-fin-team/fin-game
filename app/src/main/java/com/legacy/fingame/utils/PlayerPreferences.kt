package com.legacy.fingame.utils

import android.content.Context
import android.util.Log
import com.legacy.fingame.game.PlayerState
import com.legacy.fingame.game.PlayerStateStore
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind

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
        private const val KEY_PET_NAME = "pet_name"
        private const val KEY_SUB_LOCATION_INDEX = "sub_location_index"
        private const val KEY_BALANCE = "balance"
        private const val KEY_LAST_DAILY_BONUS_DAY = "last_daily_bonus_day"
        private const val KEY_OWNED_ITEMS = "owned_items"
        private const val KEY_WORN_ITEMS = "worn_items"
        private const val KEY_STATS_UPDATED_AT = "stats_updated_at"
        private const val KEY_PET_BORN_AT = "pet_born_at"
        private const val KEY_GAME_NOW = "game_now"

        /** Prefix of the key one stat bar is stored under, completed by [StatKind.xmlName]. */
        private const val KEY_STAT_PREFIX = "stat_"

        /**
         * Separators of one owned-item record, stored as `itemId:variantId=count`: item and variant
         * ids come from the data files and hold neither of these characters. A worn item is stored
         * as the `itemId:variantId` part alone, since there is nothing to count about it.
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
            petName = preferences.getString(KEY_PET_NAME, null) ?: defaults.petName,
            subLocationIndex = preferences.getInt(KEY_SUB_LOCATION_INDEX, defaults.subLocationIndex),
            balance = preferences.getInt(KEY_BALANCE, defaults.balance),
            lastDailyBonusDay = preferences.getLong(
                KEY_LAST_DAILY_BONUS_DAY,
                defaults.lastDailyBonusDay
            ),
            owned = readOwned(defaults.owned),
            worn = readWorn(defaults.worn),
            stats = readStats(defaults.stats),
            statsUpdatedAtMillis = preferences.getLong(
                KEY_STATS_UPDATED_AT,
                defaults.statsUpdatedAtMillis
            ),
            petBornAtMillis = preferences.getLong(KEY_PET_BORN_AT, defaults.petBornAtMillis),
            gameNowMillis = preferences.getLong(KEY_GAME_NOW, defaults.gameNowMillis)
        )
    }

    /**
     * Writes the whole state in a single edit, so a save can never leave half of it behind.
     *
     * @param state the state to remember for the next launch.
     */
    override fun save(state: PlayerState) {
        val editor = preferences.edit()
            .putString(KEY_ANIMAL_ID, state.selection?.animalId)
            .putString(KEY_ANIMAL_VARIANT_ID, state.selection?.variantId)
            .putString(KEY_PET_NAME, state.petName)
            .putInt(KEY_SUB_LOCATION_INDEX, state.subLocationIndex)
            .putInt(KEY_BALANCE, state.balance)
            .putLong(KEY_LAST_DAILY_BONUS_DAY, state.lastDailyBonusDay)
            .putStringSet(KEY_OWNED_ITEMS, state.owned.map(::encodeOwned).toSet())
            .putStringSet(KEY_WORN_ITEMS, state.worn.map(::encodeSelection).toSet())
            .putLong(KEY_STATS_UPDATED_AT, state.statsUpdatedAtMillis)
            .putLong(KEY_PET_BORN_AT, state.petBornAtMillis)
            .putLong(KEY_GAME_NOW, state.gameNowMillis)

        StatKind.entries.forEach { stat ->
            editor.putInt(KEY_STAT_PREFIX + stat.xmlName, state.stats[stat])
        }

        editor.apply()
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
     * Reads back what [save] wrote for [PlayerState.worn].
     *
     * A record that doesn't parse is dropped the same way [readOwned] drops one: the pet then simply
     * comes back with that one thing taken off, which the player can put back on.
     *
     * @param defaults value to fall back to when nothing was ever saved.
     * @return The items the pet has on.
     */
    private fun readWorn(defaults: Set<ItemSelection>): Set<ItemSelection> {
        val records = preferences.getStringSet(KEY_WORN_ITEMS, null) ?: return defaults

        val worn = mutableSetOf<ItemSelection>()
        records.forEach { record ->
            val selection = decodeSelection(record)
            if (selection == null) {
                Log.e(TAG, "Dropped a malformed worn item record: '$record'")
            } else {
                worn.add(selection)
            }
        }
        return worn.toSet()
    }

    /**
     * Reads back what [save] wrote for [PlayerState.stats].
     *
     * Every bar is stored under a key of its own, so a stat the game only just gained is missing
     * rather than breaking the read, and one it no longer has is left where it is: the value is keyed
     * by the stat's name, and a name nothing answers to is never asked for again.
     *
     * @param defaults value to fall back to for a bar that was never saved.
     * @return The pet's stat bars.
     */
    private fun readStats(defaults: PetStats): PetStats = PetStats(
        StatKind.entries.associateWith { stat ->
            preferences.getInt(KEY_STAT_PREFIX + stat.xmlName, defaults[stat])
                .coerceIn(PetStats.MIN_VALUE, PetStats.MAX_VALUE)
        }
    )

    /**
     * @param owned one entry of [PlayerState.owned].
     * @return The entry as a single string, in the `itemId:variantId=count` form.
     */
    private fun encodeOwned(owned: Map.Entry<ItemSelection, Int>): String =
        "${encodeSelection(owned.key)}$COUNT_SEPARATOR${owned.value}"

    /**
     * @param selection an item in one of its variants.
     * @return The item as a single string, in the `itemId:variantId` form.
     */
    private fun encodeSelection(selection: ItemSelection): String =
        "${selection.itemId}$VARIANT_SEPARATOR${selection.variantId}"

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

        val selection = decodeSelection(record.substring(0, countSeparator)) ?: return null
        return selection to count
    }

    /**
     * @param record a record written by [encodeSelection].
     * @return The item and its variant, or null when the record is not in the `itemId:variantId`
     * form, i.e. one of the two ids is missing.
     */
    private fun decodeSelection(record: String): ItemSelection? {
        val variantSeparator = record.indexOf(VARIANT_SEPARATOR)
        if (variantSeparator <= 0 || variantSeparator == record.lastIndex) return null

        return ItemSelection(
            itemId = record.substring(0, variantSeparator),
            variantId = record.substring(variantSeparator + 1)
        )
    }
}
