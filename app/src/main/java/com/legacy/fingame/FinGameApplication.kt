package com.legacy.fingame

import android.app.Application
import com.legacy.fingame.game.animals.AnimalRegistry
import com.legacy.fingame.game.help.HelpRegistry
import com.legacy.fingame.game.items.ItemRegistry
import com.legacy.fingame.utils.OnboardingPreferences
import com.legacy.fingame.utils.PlayerPreferences


class FinGameApplication : Application() {

    lateinit var animalRegistry: AnimalRegistry
        private set

    lateinit var itemRegistry: ItemRegistry
        private set

    lateinit var playerPreferences: PlayerPreferences
        private set

    lateinit var onboardingPreferences: OnboardingPreferences
        private set

    lateinit var helpRegistry: HelpRegistry
        private set


    override fun onCreate() {
        super.onCreate()

        animalRegistry = AnimalRegistry(applicationContext)
        itemRegistry = ItemRegistry(applicationContext)
        playerPreferences = PlayerPreferences(applicationContext)
        onboardingPreferences = OnboardingPreferences(applicationContext)
        helpRegistry = HelpRegistry(applicationContext)
    }
}
