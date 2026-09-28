package com.legacy.fingame

import android.app.Application
import com.legacy.fingame.game.animals.AnimalRegistry
import com.legacy.fingame.game.help.HelpRegistry
import com.legacy.fingame.game.hints.HintRegistry
import com.legacy.fingame.game.items.ItemRegistry
import com.legacy.fingame.game.quests.QuestRegistry
import com.legacy.fingame.game.rules.PetCareTuning
import com.legacy.fingame.game.rules.PetCareTuningReader
import com.legacy.fingame.game.settings.AudioManager
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

    lateinit var hintRegistry: HintRegistry
        private set

    lateinit var helpRegistry: HelpRegistry
        private set

    /**
     * Правила ухода за питомцем: по умолчанию, с переопределениями из
     * [PetCareTuningReader.ASSET_PATH], если такой файл есть.
     */
    var careTuning: PetCareTuning = PetCareTuning.DEFAULT
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
        questRegistry = QuestRegistry(applicationContext)
        playerPreferences = PlayerPreferences(applicationContext)
        hintRegistry = HintRegistry(applicationContext)
        helpRegistry = HelpRegistry(applicationContext)
        careTuning = runCatching {
            assets.open(PetCareTuningReader.ASSET_PATH).use(PetCareTuningReader::read)
        }.getOrDefault(PetCareTuning.DEFAULT)
        audioManager = AudioManager(applicationContext)
    }
}
