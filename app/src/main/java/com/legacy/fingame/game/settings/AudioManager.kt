package com.legacy.fingame.game.settings

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import kotlin.random.Random

/**
 * Менеджер аудио: управляет фоновой музыкой (через [MediaPlayer])
 * и звуковыми эффектами (через [SoundPool]).
 *
 * Музыка проигрывается непрерывно на фоне (loop).
 * Звуки — короткие реакции на действия пользователя.
 *
 * Файлы ожидаются в assets:
 * - Музыка: список треков в `data/audio.xml` (см. [AudioReader]), запасной вариант —
 *   первый по имени файл из `audio/music/background/`
 * - Звуки животных: `audio/sounds/animal/`
 */
class AudioManager(context: Context) {

    private val context: Context = context.applicationContext

    companion object {
        private const val TAG = "AudioManager"
        private const val AUDIO_DIR = "audio"
        private const val MUSIC_BACKGROUND_DIR = "audio/music/background"
        private const val AUDIO_DATA = "data/audio.xml"
        private const val SOUNDS_ANIMAL_DIR = "audio/sounds/animal"
        private const val MAX_STREAMS = 4

        /**
         * Запасной звук «погладил» при нажатии на питомца (`audio/sounds/animal/pat.ogg`) —
         * для животного, у которого нет своих звуков.
         */
        const val SOUND_PAT = "pat"

        /**
         * Звуки животного: ключи вида `<animalId><цифры>` (`cat1`, `cat2`, `cat10`).
         * `category` для `cat` не подходит — после id должны идти только цифры.
         *
         * @param all все ключи загруженных звуков (имена файлов без расширения).
         * @param animalId id животного из `data/animals.xml`.
         * @return Ключи звуков животного, отсортированные по номеру.
         */
        fun animalSoundKeys(all: Collection<String>, animalId: String): List<String> {
            if (animalId.isEmpty()) return emptyList()
            return all.filter { key ->
                key.length > animalId.length &&
                    key.startsWith(animalId) &&
                    key.substring(animalId.length).all { it.isDigit() }
            }.sortedBy { it.substring(animalId.length).toBigInteger() }
        }

        /**
         * Выбирает случайный трек фоновой музыки.
         *
         * @param declared треки из `data/audio.xml` (пути относительно `assets/audio/`).
         * @param folderFiles имена файлов в `audio/music/background/`.
         * @param random источник случайности для выбора трека.
         * @param exists есть ли такой путь (относительно `assets/`) на самом деле.
         * @return Путь относительно `assets/`: случайный из объявленных треков, которые существуют,
         *   иначе случайный файл из папки, иначе null — музыки нет.
         */
        fun chooseBackgroundTrack(
            declared: List<String>,
            folderFiles: List<String>,
            random: Random = Random.Default,
            exists: (String) -> Boolean
        ): String? {
            val validDeclared = declared
                .map { "$AUDIO_DIR/${it.trimStart('/')}" }
                .filter(exists)
            if (validDeclared.isNotEmpty()) {
                return validDeclared.random(random)
            }
            val validFolderFiles = folderFiles
                .filter { exists("$MUSIC_BACKGROUND_DIR/$it") }
            if (validFolderFiles.isNotEmpty()) {
                return "$MUSIC_BACKGROUND_DIR/${validFolderFiles.random(random)}"
            }
            return null
        }
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
     * Запускает фоновую музыку: первый трек из `data/audio.xml`, а если его нет —
     * первый по имени файл из `audio/music/background/`.
     */
    fun startMusic() {
        if (!musicEnabled) return
        if (mediaPlayer != null) return // уже играет

        try {
            val musicFile = findBackgroundTrack()
            if (musicFile == null) {
                Log.w(TAG, "Нет фоновой музыки ни в $AUDIO_DATA, ни в $MUSIC_BACKGROUND_DIR")
                return
            }
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
        Log.d(TAG, "Звук: $soundKey")
    }

    /**
     * Проигрывает случайный звук животного из его набора (`cat1`, `cat2`, `cat3` для `cat`),
     * а если своих звуков у животного нет — запасной [SOUND_PAT].
     *
     * @param animalId id животного из `data/animals.xml`.
     * @param random источник случайности (подменяется в тестах).
     */
    fun playAnimalSound(animalId: String, random: Random = Random.Default) {
        if (!soundEnabled) return
        val keys = animalSoundKeys(loadedSounds.keys, animalId)
        playSound(if (keys.isEmpty()) SOUND_PAT else keys.random(random))
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
     * Ищет трек фоновой музыки по данным и по папке (см. [chooseBackgroundTrack]).
     */
    private fun findBackgroundTrack(): String? {
        val declared = try {
            context.assets.open(AUDIO_DATA).use { AudioReader().readBackgroundTracks(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Нет $AUDIO_DATA, музыка берётся из папки")
            emptyList()
        }
        val folderFiles = assetList(MUSIC_BACKGROUND_DIR)
        return chooseBackgroundTrack(declared, folderFiles) { path ->
            path.substringAfterLast('/') in assetList(path.substringBeforeLast('/', ""))
        }
    }

    /** Имена файлов в папке assets, пусто если папки нет. */
    private fun assetList(dir: String): List<String> =
        try {
            context.assets.list(dir)?.toList().orEmpty()
        } catch (e: Exception) {
            emptyList()
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
                if (!file.endsWith(".ogg", ignoreCase = true)) continue
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
