package com.legacy.fingame

import android.content.SharedPreferences
import com.legacy.fingame.game.settings.GameSettings
import com.legacy.fingame.game.settings.GameSettingsRepository
import com.legacy.fingame.game.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Настройки игры переживают перезапуск, а старые сохранения читаются как раньше. */
class GameSettingsRepositoryTest {

    @Test
    fun `animations are on by default`() {
        assertTrue(GameSettings().animationsEnabled)
        assertTrue(GameSettingsRepository(MemoryPreferences()).load().animationsEnabled)
    }

    @Test
    fun `turned off animations are saved and read back`() {
        val prefs = MemoryPreferences()
        val saved = GameSettings(
            soundVolume = 40,
            musicVolume = 0,
            themeMode = ThemeMode.DARK,
            animationsEnabled = false
        )

        GameSettingsRepository(prefs).save(saved)

        assertEquals(saved, GameSettingsRepository(prefs).load())
    }

    @Test
    fun `turning animations back on is saved too`() {
        val prefs = MemoryPreferences()
        val repository = GameSettingsRepository(prefs)

        repository.save(GameSettings(animationsEnabled = false))
        repository.save(GameSettings(animationsEnabled = true))

        assertTrue(GameSettingsRepository(prefs).load().animationsEnabled)
    }

    @Test
    fun `a save made before the animations setting keeps animations on`() {
        // Так выглядел файл настроек до появления переключателя «Анимации».
        val prefs = MemoryPreferences(
            mutableMapOf(
                "sound_volume" to 0,
                "music_volume" to 70,
                "sound_enabled" to false,
                "music_enabled" to true,
                "theme_mode" to ThemeMode.LIGHT.name
            )
        )

        val loaded = GameSettingsRepository(prefs).load()

        assertTrue(loaded.animationsEnabled)
        assertEquals(0, loaded.soundVolume)
        assertEquals(70, loaded.musicVolume)
        assertEquals(ThemeMode.LIGHT, loaded.themeMode)
        assertFalse(loaded.soundEnabled)
    }
}

/** [SharedPreferences] в памяти: хранит то, что в них записали, и больше ничего не умеет. */
private class MemoryPreferences(
    private val values: MutableMap<String, Any?> = mutableMapOf()
) : SharedPreferences {

    override fun getAll(): Map<String, *> = values.toMap()
    override fun getString(key: String, defValue: String?) = values[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: Set<String>?) =
        values[key] as? Set<String> ?: defValues
    override fun getInt(key: String, defValue: Int) = values[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long) = values[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float) = values[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean) = values[key] as? Boolean ?: defValue
    override fun contains(key: String) = key in values
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private var clear = false

        override fun putString(key: String, value: String?) = apply { pending[key] = value }
        override fun putStringSet(key: String, values: Set<String>?) =
            apply { pending[key] = values }
        override fun putInt(key: String, value: Int) = apply { pending[key] = value }
        override fun putLong(key: String, value: Long) = apply { pending[key] = value }
        override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
        override fun remove(key: String) = apply { pending[key] = null }
        override fun clear() = apply { clear = true }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            if (clear) values.clear()
            pending.forEach { (key, value) ->
                if (value == null) values.remove(key) else values[key] = value
            }
        }
    }
}
