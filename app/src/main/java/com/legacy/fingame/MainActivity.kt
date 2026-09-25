package com.legacy.fingame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.game.settings.AudioManager
import com.legacy.fingame.game.settings.GameSettingsRepository
import com.legacy.fingame.game.settings.ThemeMode
import com.legacy.fingame.ui.FinGameApp
import com.legacy.fingame.ui.theme.FinGameTheme

/**
 * App entry point: sets up edge-to-edge display, initializes the audio system,
 * loads persisted settings, and hosts [FinGameApp] under [FinGameTheme].
 */
class MainActivity : ComponentActivity() {

    private var audioManager: AudioManager? = null
    private lateinit var settingsRepository: GameSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Настройки
        settingsRepository = GameSettingsRepository(applicationContext)
        val savedSettings = settingsRepository.load()

        // Аудио
        val audio = AudioManager(applicationContext)
        audio.init()
        audio.applySettings(savedSettings)
        audio.startMusic()
        audioManager = audio

        window.decorView.isSoundEffectsEnabled = savedSettings.soundEnabled

        setContent {
            val vm: GameViewModel = viewModel(
                factory = with(application as FinGameApplication) {
                    GameViewModel.factory(store = playerPreferences, catalog = itemRegistry)
                }
            )
            val state by vm.state.collectAsStateWithLifecycle()
            val currentSettings = state.settings
            val view = LocalView.current

            // Загружаем сохранённые настройки в ViewModel при первом запуске
            LaunchedEffect(Unit) {
                vm.updateSettings(savedSettings)
            }

            // Сохраняем настройки и обновляем аудио/звуки кнопок при каждом изменении
            LaunchedEffect(currentSettings) {
                settingsRepository.save(currentSettings)
                audioManager?.applySettings(currentSettings)
                window.decorView.isSoundEffectsEnabled = currentSettings.soundEnabled
                view.isSoundEffectsEnabled = currentSettings.soundEnabled
            }

            val isDark = when (currentSettings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> isSystemInDarkTheme()
            }

            FinGameTheme(darkTheme = isDark) {
                FinGameApp(vm = vm)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        audioManager?.pauseMusic()
    }

    override fun onResume() {
        super.onResume()
        audioManager?.resumeMusic()
    }

    override fun onDestroy() {
        audioManager?.release()
        audioManager = null
        super.onDestroy()
    }
}
