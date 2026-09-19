package com.legacy.fingame

import android.app.Application
import com.legacy.fingame.game.animals.AnimalRegistry


class FinGameApplication : Application() {

    lateinit var animalRegistry: AnimalRegistry
        private set

    override fun onCreate() {
        super.onCreate()

        animalRegistry = AnimalRegistry(applicationContext)
    }
}