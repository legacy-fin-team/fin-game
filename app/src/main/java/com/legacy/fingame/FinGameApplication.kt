package com.legacy.fingame

import android.app.Application
import com.legacy.fingame.game.animals.AnimalRegistry
import com.legacy.fingame.game.items.ItemRegistry
import com.legacy.fingame.game.rules.PetCareTuning
import com.legacy.fingame.game.rules.PetCareTuningReader
import com.legacy.fingame.utils.PlayerPreferences


class FinGameApplication : Application() {

    lateinit var animalRegistry: AnimalRegistry
        private set

    lateinit var itemRegistry: ItemRegistry
        private set

    lateinit var playerPreferences: PlayerPreferences
        private set

    /**
     * Правила ухода за питомцем: по умолчанию, с переопределениями из
     * [PetCareTuningReader.ASSET_PATH], если такой файл есть.
     */
    var careTuning: PetCareTuning = PetCareTuning.DEFAULT
        private set


    override fun onCreate() {
        super.onCreate()

        animalRegistry = AnimalRegistry(applicationContext)
        itemRegistry = ItemRegistry(applicationContext)
        playerPreferences = PlayerPreferences(applicationContext)
        careTuning = runCatching {
            assets.open(PetCareTuningReader.ASSET_PATH).use(PetCareTuningReader::read)
        }.getOrDefault(PetCareTuning.DEFAULT)
    }
}
