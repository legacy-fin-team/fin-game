package com.legacy.fingame.game.localization

import android.content.Context
import android.util.Log

/**
 * Хранилище локализаций.
 *
 * При инициализации загружает **все** доступные языки из assets/locale/.
 * Извне можно только сменить текущий язык через [setLanguage] —
 * изменить содержимое словаря напрямую невозможно.
 */
class LocalizationRegistry(context: Context, defaultLanguage: String = "ru-RU") {

    companion object {
        private const val TAG = "LocalizationRegistry"
    }

    /** Вложенный словарь: язык → (ключ перевода → значение). Неизменяем извне. */
    private val allTranslations: Map<String, Map<String, String>>

    /** Текущий активный язык. */
    private var currentLanguage: String = defaultLanguage

    init {
        val reader = LocalizationReader(context)
        allTranslations = reader.readAllLocales()

        // Если дефолтный язык недоступен, берём первый из загруженных
        if (!allTranslations.containsKey(currentLanguage)) {
            val fallback = allTranslations.keys.firstOrNull()
            if (fallback != null) {
                Log.w(TAG, "Язык '$currentLanguage' не найден, используется '$fallback'")
                currentLanguage = fallback
            } else {
                Log.e(TAG, "Не найдено ни одного языка в assets/locale/")
            }
        }
    }

    /**
     * Меняет текущий язык.
     * @param languageCode код языка (например "ru-RU", "en-US").
     * @return `true` если язык найден и переключён, `false` если такого языка нет.
     */
    fun setLanguage(languageCode: String): Boolean {
        return if (allTranslations.containsKey(languageCode)) {
            currentLanguage = languageCode
            true
        } else {
            Log.e(TAG, "Язык '$languageCode' не найден среди доступных: ${allTranslations.keys}")
            false
        }
    }

    /**
     * Возвращает текущий активный язык.
     */
    fun getCurrentLanguage(): String = currentLanguage

    /**
     * Возвращает список всех доступных языковых кодов.
     */
    fun getAvailableLanguages(): Set<String> = allTranslations.keys

    /**
     * Возвращает переведённую строку по её id для текущего языка.
     * Если ключ не найден — возвращает сам id и пишет в лог.
     */
    fun getString(id: String): String {
        return allTranslations[currentLanguage]?.get(id) ?: run {
            Log.e(TAG, "Строка '$id' не найдена для языка '$currentLanguage'")
            id
        }
    }

    /**
     * Возвращает все строки текущего языка.
     */
    fun getAllStrings(): Map<String, String> {
        return allTranslations[currentLanguage] ?: emptyMap()
    }

    /**
     * Возвращает все строки для конкретного языка, вне зависимости от текущего.
     * @param languageCode код языка.
     */
    fun getAllStringsFor(languageCode: String): Map<String, String> {
        return allTranslations[languageCode] ?: run {
            Log.e(TAG, "Язык '$languageCode' не найден")
            emptyMap()
        }
    }
}
