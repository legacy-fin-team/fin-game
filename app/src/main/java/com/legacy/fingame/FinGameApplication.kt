package com.legacy.fingame

import android.app.Application
import com.legacy.fingame.game.animals.AnimalRegistry
import com.legacy.fingame.game.items.ItemRegistry
import com.legacy.fingame.game.localization.LocalizationReader
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
        
        // Инициализируем реестр
        localizationRegistry = LocalizationRegistry()
        
        // Считываем переводы с помощью Reader
        val reader = LocalizationReader(applicationContext)
        val translations = reader.readLocales("ru-RU") // по умолчанию загружаем ru-RU
        
        // Сохраняем переводы в реестр
        localizationRegistry.updateTranslations(translations)
    }
}