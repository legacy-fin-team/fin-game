package com.legacy.fingame.game.localization

import android.content.Context
import android.util.Log

/**
 * Хранилище локализаций.
 */
class LocalizationRegistry(context: Context, defaultLanguage: String = "ru-RU") {

    companion object {
        private const val TAG = "LocalizationRegistry"
    }
    
    private var translations: Map<String, String>

    init {
        // При инициализации сразу создаем Reader и загружаем словарь
        val reader = LocalizationReader(context)
        translations = reader.readLocales(defaultLanguage)
    }

    /**
     * Обновляет словарь новыми переводами (например, при смене языка в рантайме).
     */
    fun updateTranslations(newTranslations: Map<String, String>) {
        translations = newTranslations.toMap()
    }

    /**
     * Возвращает переведенную строку по её id.
     */
    fun getString(id: String): String {
        return translations[id] ?: run {
            Log.e(TAG, "String not found for id: '$id'")
            id
        }
    }

    /**
     * Возвращает словарь со всеми загруженными строками.
     */
    fun getAllStrings(): Map<String, String> {
        return translations
    }
}
