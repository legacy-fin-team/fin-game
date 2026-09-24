package com.legacy.fingame.game.settings

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log

/**
 * Менеджер аудио: управляет фоновой музыкой (через [MediaPlayer])
 * и звуковыми эффектами (через [SoundPool]).
 *
 * Музыка проигрывается непрерывно на фоне (loop).
 * Звуки — короткие реакции на действия пользователя.
 *
 * Файлы ожидаются в assets:
 * - Музыка: `audio/music/`
 * - Звуки животных: `audio/sounds/animal/`
 */
class AudioManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioManager"
        private const val MUSIC_DIR = "audio/music"
        private const val SOUNDS_ANIMAL_DIR = "audio/sounds/animal"
        private const val MAX_STREAMS = 4
    }

    private var mediaPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null

    /** Загруженные id звуков в SoundPool: имя файла (без расширения) → sound id. */
    private val loadedSounds = mutableMapOf<String, Int>()

    private var musicEnabled = true
    private var soundEnabled = true

    /**
     * Инициализирует SoundPool и загружает все звуки из assets.
     * Вызывать при старте приложения.
     */
    fun init() {
        soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .build()
        loadAllSounds()
    }

    /**
     * Применяет текущие настройки звука и музыки.
     */
    fun applySettings(settings: GameSettings) {
        val wasMusicEnabled = musicEnabled
        musicEnabled = settings.musicEnabled
        soundEnabled = settings.soundEnabled

        if (musicEnabled && !wasMusicEnabled) {
            startMusic()
        } else if (!musicEnabled && wasMusicEnabled) {
            stopMusic()
        }
    }

    /**
     * Запускает фоновую музыку. Берёт первый файл из `audio/music/`.
     */
    fun startMusic() {
        if (!musicEnabled) return
        if (mediaPlayer != null) return // уже играет

        try {
            val musicFiles = context.assets.list(MUSIC_DIR)
            if (musicFiles.isNullOrEmpty()) {
                Log.w(TAG, "Нет музыкальных файлов в $MUSIC_DIR")
                return
            }
            val musicFile = "$MUSIC_DIR/${musicFiles[0]}"
            val afd: AssetFileDescriptor = context.assets.openFd(musicFile)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
            Log.i(TAG, "Музыка запущена: $musicFile")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при запуске музыки", e)
        }
    }

    /**
     * Останавливает фоновую музыку и освобождает MediaPlayer.
     */
    fun stopMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    /**
     * Ставит музыку на паузу (например при уходе приложения в фон).
     */
    fun pauseMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause()
        }
    }

    /**
     * Возобновляет музыку после паузы.
     */
    fun resumeMusic() {
        if (!musicEnabled) return
        mediaPlayer?.start()
    }

    /**
     * Проигрывает звуковой эффект по ключу.
     *
     * @param soundKey ключ звука — имя файла без расширения
     *   (например, "cat" для `audio/sounds/animal/cat.ogg`).
     */
    fun playSound(soundKey: String) {
        if (!soundEnabled) return
        val soundId = loadedSounds[soundKey]
        if (soundId == null) {
            Log.w(TAG, "Звук '$soundKey' не загружен")
            return
        }
        soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    /**
     * Освобождает все ресурсы. Вызывать при уничтожении приложения.
     */
    fun release() {
        stopMusic()
        soundPool?.release()
        soundPool = null
        loadedSounds.clear()
    }

    /**
     * Загружает все звуки из `audio/sounds/animal/`.
     */
    private fun loadAllSounds() {
        val sp = soundPool ?: return
        try {
            val files = context.assets.list(SOUNDS_ANIMAL_DIR)
            if (files.isNullOrEmpty()) {
                Log.i(TAG, "Нет звуковых файлов в $SOUNDS_ANIMAL_DIR")
                return
            }
            for (file in files) {
                val path = "$SOUNDS_ANIMAL_DIR/$file"
                try {
                    val afd = context.assets.openFd(path)
                    val soundId = sp.load(afd, 1)
                    afd.close()
                    // Ключ — имя файла без расширения
                    val key = file.substringBeforeLast('.')
                    loadedSounds[key] = soundId
                    Log.d(TAG, "Звук загружен: $key (id=$soundId)")
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка загрузки звука: $path", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сканировании звуковых файлов", e)
        }
    }
}
