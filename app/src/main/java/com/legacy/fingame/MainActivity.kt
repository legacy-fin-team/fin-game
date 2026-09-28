package com.legacy.fingame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.game.settings.AudioManager
import com.legacy.fingame.game.settings.GameSettingsRepository
import com.legacy.fingame.game.settings.ThemeMode
import com.legacy.fingame.ui.ClickSound
import com.legacy.fingame.ui.FinGameApp
import com.legacy.fingame.ui.theme.FinGameTheme

/**
 * App entry point: sets up edge-to-edge display, initializes the audio system,
 * loads persisted settings, and hosts [FinGameApp] under [FinGameTheme].
 */
class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: GameSettingsRepository

    /** Общий на процесс менеджер звука из [FinGameApplication]: переживает поворот экрана. */
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Настройки
        settingsRepository = GameSettingsRepository(applicationContext)
        val savedSettings = settingsRepository.load()

        // Аудио живёт в Application: при повороте экрана менеджер уже запущен и музыка
        // не прерывается — только применяем настройки
        audioManager = (application as FinGameApplication).audioManager
        if (!audioManager.isInitialized) {
            audioManager.init()
            audioManager.applySettings(savedSettings)
            audioManager.startMusic()
        } else {
            audioManager.applySettings(savedSettings)
        }

        // Щелчок кнопок слушается настройки «Звуки» с самого первого нажатия.
        ClickSound.enabled = savedSettings.soundEnabled

        setContent {
            // Сохранённые настройки попадают в состояние сразу при создании ViewModel, поэтому
            // эффект ниже с первого кадра видит их, а не значения по умолчанию: музыка не
            // вспыхивает, а сохранённое не перезаписывается дефолтом.
            val vm: GameViewModel = viewModel(
                factory = with(application as FinGameApplication) {
                    GameViewModel.factory(
                        store = playerPreferences,
                        catalog = itemRegistry,
                        settings = savedSettings
                    )
                }
            )
            val state by vm.state.collectAsStateWithLifecycle()
            val currentSettings = state.settings

            // Сохраняем настройки и обновляем аудио и щелчок кнопок при каждом изменении
            LaunchedEffect(currentSettings) {
                settingsRepository.save(currentSettings)
                audioManager.applySettings(currentSettings)
                ClickSound.enabled = currentSettings.soundEnabled
            }

            val isDark = when (currentSettings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> isSystemInDarkTheme()
            }

            FinGameTheme(darkTheme = isDark) {
                FinGameApp(
                    vm = vm,
                    onPlaySound = { soundKey -> audioManager.playSound(soundKey) },
                    onPlayAnimalSound = { animalId -> audioManager.playAnimalSound(animalId) }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        audioManager.pauseMusic()
    }

    override fun onResume() {
        super.onResume()
        audioManager.resumeMusic()
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            audioManager.release()
        }
        super.onDestroy()
    }
}
