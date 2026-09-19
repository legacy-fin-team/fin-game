package com.legacy.fingame.game.localization

import android.content.Context
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/**
 * Менеджер локализации.
 */
class LocalizationManager(private val context: Context) {

    private val translations = mutableMapOf<String, String>()
    /**
     * Загружает все XML файлы для указанного языка из папки "locale/<languageCode>".
     * @param languageCode код языка (название папки), например "ru-RU" или "en-US".
     */
    fun loadLocales(languageCode: String) {
        translations.clear()
        val basePath = "locale/$languageCode"
        val xmlFiles = findXmlFiles(basePath)
        
        for (filePath in xmlFiles) {
            try {
                context.assets.open(filePath).use { inputStream ->
                    parseXml(inputStream)
                }
            } catch (e: Exception) {
                Log.e("LocalizationManager", "Ошибка при чтении файла: $filePath", e)
            }
        }
        Log.d("LocalizationManager", "Загружено ${translations.size} строк из ${xmlFiles.size} файлов.")
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
    /**
     * Рекурсивный поиск XML файлов в папке assets.
     */
    private fun findXmlFiles(path: String): List<String> {
        val result = mutableListOf<String>()
        try {
            val list = context.assets.list(path)
            if (list != null && list.isNotEmpty()) {
                // Если это папка, рекурсивно обходим её содержимое
                for (file in list) {
                    val fullPath = if (path.isEmpty()) file else "$path/$file"
                    result.addAll(findXmlFiles(fullPath))
                }
            } else {
                // Если это файл (или пустая папка), проверяем расширение
                if (path.endsWith(".xml", ignoreCase = true)) {
                    result.add(path)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalizationManager", "Ошибка при получении списка файлов по пути: $path", e)
        }
        return result
    }

    /**
     * Парсинг конкретного XML файла и добавление найденных ключей в словарь translations.
     */
    private fun parseXml(inputStream: InputStream) {
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                    val id = parser.getAttributeValue(null, "id")
                    if (id != null) {
                        val text = parser.nextText()
                        translations[id] = text
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("LocalizationManager", "Ошибка при парсинге XML", e)
        }
    }
}
