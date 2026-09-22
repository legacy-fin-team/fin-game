package com.legacy.fingame

import android.app.Application
import com.legacy.fingame.game.animals.AnimalRegistry
import com.legacy.fingame.game.items.ItemRegistry
import com.legacy.fingame.game.localization.LocalizationRegistry


class FinGameApplication : Application() {

    lateinit var animalRegistry: AnimalRegistry
        private set

    lateinit var itemRegistry: ItemRegistry
        private set

    lateinit var localizationRegistry: LocalizationRegistry
        private set

    override fun onCreate() {
        super.onCreate()

        animalRegistry = AnimalRegistry(applicationContext)
        itemRegistry = ItemRegistry(applicationContext)
        localizationRegistry = LocalizationRegistry(applicationContext)
    }
}
