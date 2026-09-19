package com.legacy.fingame.game.localization

import android.content.Context
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/**
 * Загрузчик локализаций.
 * Отвечает за поиск, чтение и парсинг XML файлов из assets.
 */
class LocalizationReader(private val context: Context) {

    /**
     * Загружает все XML файлы для указанного языка из папки "locale/<languageCode>".
     * @param languageCode код языка (название папки), например "ru-RU" или "en-US".
     * @return Словарь (Map) найденных ключей и их значений.
     */
    fun readLocales(languageCode: String): Map<String, String> {
        val translations = mutableMapOf<String, String>()
        val basePath = "locale/$languageCode"
        val xmlFiles = findXmlFiles(basePath)
        
        for (filePath in xmlFiles) {
            try {
                context.assets.open(filePath).use { inputStream ->
                    parseXml(inputStream, translations)
                }
            } catch (e: Exception) {
                Log.e("LocalizationReader", "Ошибка при чтении файла: $filePath", e)
            }
        }
        Log.d("LocalizationReader", "Считано ${translations.size} строк из ${xmlFiles.size} файлов.")
        return translations
    }

    /**
     * Рекурсивный поиск XML файлов в папке assets.
     */
    private fun findXmlFiles(path: String): List<String> {
        val result = mutableListOf<String>()
        try {
            val list = context.assets.list(path)
            if (list != null && list.isNotEmpty()) {
                for (file in list) {
                    val fullPath = if (path.isEmpty()) file else "$path/$file"
                    result.addAll(findXmlFiles(fullPath))
                }
            } else {
                if (path.endsWith(".xml", ignoreCase = true)) {
                    result.add(path)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalizationReader", "Ошибка при получении списка файлов: $path", e)
        }
        return result
    }

    /**
     * Парсинг конкретного XML файла и добавление найденных ключей в переданный словарь.
     */
    private fun parseXml(inputStream: InputStream, translations: MutableMap<String, String>) {
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
            Log.e("LocalizationReader", "Ошибка при парсинге XML", e)
        }
    }
}
