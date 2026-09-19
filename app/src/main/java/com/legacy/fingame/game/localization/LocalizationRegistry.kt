package com.legacy.fingame.game.localization

/**
 * Хранилище локализаций.
 */
class LocalizationRegistry {
    
    private val translations = mutableMapOf<String, String>()

    /**
     * Обновляет словарь новыми переводами, очищая старые.
     */
    fun updateTranslations(newTranslations: Map<String, String>) {
        translations.clear()
        translations.putAll(newTranslations)
    }

    /**
     * Возвращает переведенную строку по её id.
     */
    fun getString(id: String): String? {
        return translations[id]
    }

    /**
     * Возвращает словарь со всеми загруженными строками.
     */
    fun getAllStrings(): Map<String, String> {
        return translations.toMap()
    }
}
