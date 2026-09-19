package com.legacy.fingame.game.localization

import android.util.Log

/**
 * Хранилище локализаций.
 */
class LocalizationRegistry {

    companion object {
        private const val TAG = "LocalizationRegistry"
    }
    
    private var translations: Map<String, String> = emptyMap()

    /**
     * Обновляет словарь новыми переводами, очищая старые.
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
