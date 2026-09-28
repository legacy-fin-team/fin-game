package com.legacy.fingame

import android.app.Application
import com.legacy.fingame.game.animals.AnimalRegistry
import com.legacy.fingame.game.items.ItemRegistry
import com.legacy.fingame.game.settings.AudioManager
import com.legacy.fingame.utils.PlayerPreferences


class FinGameApplication : Application() {

    lateinit var animalRegistry: AnimalRegistry
        private set

    lateinit var itemRegistry: ItemRegistry
        private set

    lateinit var playerPreferences: PlayerPreferences
        private set

    /**
     * Единственный на процесс менеджер звука. Живёт в Application, а не в Activity, чтобы музыка
     * не прерывалась при повороте экрана, и не в статическом поле, чтобы не держать Context.
     */
    lateinit var audioManager: AudioManager
        private set


    override fun onCreate() {
        super.onCreate()

        animalRegistry = AnimalRegistry(applicationContext)
        itemRegistry = ItemRegistry(applicationContext)
        playerPreferences = PlayerPreferences(applicationContext)
        audioManager = AudioManager(applicationContext)
    }
}
