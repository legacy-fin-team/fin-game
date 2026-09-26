package com.legacy.fingame

import android.app.Application
import com.legacy.fingame.game.animals.AnimalRegistry
import com.legacy.fingame.game.items.ItemRegistry
import com.legacy.fingame.game.quests.QuestRegistry
import com.legacy.fingame.utils.PlayerPreferences


class FinGameApplication : Application() {

    lateinit var animalRegistry: AnimalRegistry
        private set

    lateinit var itemRegistry: ItemRegistry
        private set

    lateinit var questRegistry: QuestRegistry
        private set

    lateinit var playerPreferences: PlayerPreferences
        private set


    override fun onCreate() {
        super.onCreate()

        animalRegistry = AnimalRegistry(applicationContext)
        itemRegistry = ItemRegistry(applicationContext)
        questRegistry = QuestRegistry(applicationContext)
        playerPreferences = PlayerPreferences(applicationContext)
    }
}
