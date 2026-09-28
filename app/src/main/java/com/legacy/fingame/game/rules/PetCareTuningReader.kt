package com.legacy.fingame.game.rules

import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Читает переопределение правил ухода из `assets/data/care.xml`: чтобы подкрутить пороги и
 * штрафы, не нужно трогать код.
 *
 * Файл — один элемент `<care>` с атрибутами по именам полей [PetCareTuning]. Атрибут, которого нет
 * или который не читается как число в допустимых пределах, остаётся значением по умолчанию, так
 * что опечатка в файле не ломает игру, а только не срабатывает.
 */
object PetCareTuningReader {

    /** Путь к файлу внутри assets. */
    const val ASSET_PATH = "data/care.xml"

    /**
     * @param inputStream содержимое `care.xml`.
     * @return Правила ухода: из файла, где он их задаёт, и по умолчанию во всём остальном; при
     * нечитаемом файле — [PetCareTuning.DEFAULT] целиком.
     */
    fun read(inputStream: InputStream): PetCareTuning {
        val root = runCatching {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
                .documentElement
        }.getOrNull() ?: return PetCareTuning.DEFAULT

        val defaults = PetCareTuning.DEFAULT
        return PetCareTuning(
            stopGrowthBelow = root.fraction("stopGrowthBelow", defaults.stopGrowthBelow),
            slowGrowthBelow = root.fraction("slowGrowthBelow", defaults.slowGrowthBelow),
            slowGrowthMultiplier = root.fraction(
                "slowGrowthMultiplier",
                defaults.slowGrowthMultiplier
            ),
            neglectBelow = root.fraction("neglectBelow", defaults.neglectBelow),
            incomePenaltyPercentPerDay = root.percent(
                "incomePenaltyPercentPerDay",
                defaults.incomePenaltyPercentPerDay
            ),
            maxIncomePenaltyPercent = root.percent(
                "maxIncomePenaltyPercent",
                defaults.maxIncomePenaltyPercent
            ),
            optionalMarkupPercentPerDay = root.percent(
                "optionalMarkupPercentPerDay",
                defaults.optionalMarkupPercentPerDay
            ),
            maxOptionalMarkupPercent = root.percent(
                "maxOptionalMarkupPercent",
                defaults.maxOptionalMarkupPercent,
                max = MAX_MARKUP_PERCENT
            )
        )
    }

    /** Наценка больше этой уже не «мягкий» штраф, а опечатка. */
    private const val MAX_MARKUP_PERCENT = 1_000

    private fun Element.fraction(name: String, default: Double): Double =
        getAttribute(name).trim().toDoubleOrNull()?.takeIf { it in 0.0..1.0 } ?: default

    private fun Element.percent(name: String, default: Int, max: Int = 100): Int =
        getAttribute(name).trim().toIntOrNull()?.takeIf { it in 0..max } ?: default
}
