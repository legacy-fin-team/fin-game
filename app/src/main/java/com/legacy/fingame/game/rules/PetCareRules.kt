package com.legacy.fingame.game.rules

import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind

/**
 * Все числа правил ухода в одном месте: пороги индекса ухода, скорость роста и мягкие штрафы.
 *
 * Индекс ухода — число от 0 до 1 (см. [PetCareRules.careIndex]). Пороги сравниваются с лучшим
 * индексом, который питомец набрал за свой день (см. [PetCare]).
 *
 * Значения по умолчанию можно переопределить в `assets/data/care.xml`
 * (см. [PetCareTuningReader]), не трогая код.
 *
 * @property stopGrowthBelow ниже этого индекса питомец за день не растёт совсем.
 * @property slowGrowthBelow ниже этого индекса (но не ниже [stopGrowthBelow]) питомец растёт
 * медленнее — с множителем [slowGrowthMultiplier].
 * @property slowGrowthMultiplier во сколько раз медленнее растёт питомец, за которым ухаживали
 * вполсилы.
 * @property neglectBelow день с индексом ниже этого порога считается днём без заботы: он
 * продлевает серию [PetCare.neglectStreak], день не ниже — обнуляет её.
 * @property incomePenaltyPercentPerDay на сколько процентов меньше бонус дня за каждый день серии.
 * @property maxIncomePenaltyPercent больше этого процента бонус дня не уменьшается никогда.
 * @property optionalMarkupPercentPerDay на сколько процентов дороже необязательные товары за
 * каждый день серии.
 * @property maxOptionalMarkupPercent больше этого процента необязательные товары не дорожают.
 */
data class PetCareTuning(
    val stopGrowthBelow: Double = 0.25,
    val slowGrowthBelow: Double = 0.5,
    val slowGrowthMultiplier: Double = 0.5,
    val neglectBelow: Double = 0.5,
    val incomePenaltyPercentPerDay: Int = 10,
    val maxIncomePenaltyPercent: Int = 40,
    val optionalMarkupPercentPerDay: Int = 10,
    val maxOptionalMarkupPercent: Int = 50
) {
    companion object {
        /** Правила, по которым играет обычная сборка, пока `care.xml` их не переопределил. */
        val DEFAULT = PetCareTuning()
    }
}

/**
 * Точка правды для правил ухода за питомцем: как уход влияет на рост и какие мягкие штрафы
 * получает игрок, когда питомцем долго не занимаются.
 *
 * Здесь только чистые функции: ни часов, ни сохранения, ни Android. Где и когда их применять,
 * решают [PetCare] (рост и серия дней без заботы), [CarePricedCatalog] (цены) и
 * [com.legacy.fingame.game.GameViewModel] (бонус дня). Поменять правило — значит поменять
 * функцию здесь или число в [PetCareTuning].
 */
object PetCareRules {

    /** Индекс ухода питомца, у которого все шкалы полные. */
    const val FULL_CARE = 1.0

    /** Сотня процентов: в них считаются штрафы, чтобы монеты не страдали от дробей. */
    private const val PERCENT = 100

    /**
     * Индекс ухода: среднее всех шкал питомца, приведённое к `0.0..1.0`.
     *
     * Среднее, а не минимум: здоровье почти нечем поднять в магазине, и минимум сделал бы день
     * «плохим» из-за шкалы, которую ребёнок исправить не может. Среднее засчитывает заботу в
     * целом — накормил и поиграл, — а какую шкалу подтянуть, подсказывает [explain].
     *
     * @param stats шкалы питомца.
     * @return 0.0 — все шкалы пустые, 1.0 — все полные.
     */
    fun careIndex(stats: PetStats): Double {
        val kinds = StatKind.entries
        if (kinds.isEmpty()) return FULL_CARE
        return kinds.sumOf { stats[it] }.toDouble() / (kinds.size * PetStats.MAX_VALUE)
    }

    /**
     * Правило роста: во сколько раз быстрее обычного растёт питомец с такими шкалами.
     *
     * @param stats шкалы питомца.
     * @return 0.0 — рост остановлен, 1.0 — растёт как обычно, между — растёт медленнее.
     */
    fun growthMultiplier(stats: PetStats, tuning: PetCareTuning = PetCareTuning.DEFAULT): Double =
        growthMultiplierFor(careIndex(stats), tuning)

    /**
     * То же правило роста, что и [growthMultiplier], но по уже посчитанному индексу ухода — им
     * [PetCare] судит прожитый день по лучшему индексу за этот день.
     *
     * @param careIndex индекс ухода, см. [careIndex].
     * @return Множитель скорости роста, `0.0..1.0`.
     */
    fun growthMultiplierFor(
        careIndex: Double,
        tuning: PetCareTuning = PetCareTuning.DEFAULT
    ): Double = when {
        careIndex < tuning.stopGrowthBelow -> 0.0
        careIndex < tuning.slowGrowthBelow -> tuning.slowGrowthMultiplier
        else -> 1.0
    }

    /**
     * @param careIndex лучший индекс ухода за день.
     * @return Был ли день днём без заботы, то есть продлевает ли он серию дней без заботы.
     */
    fun isNeglected(careIndex: Double, tuning: PetCareTuning = PetCareTuning.DEFAULT): Boolean =
        careIndex < tuning.neglectBelow

    /**
     * @param neglectStreak сколько дней подряд питомцем не занимались.
     * @return Доля бонуса дня, которую игрок получает: 1.0 — весь бонус.
     */
    fun incomeMultiplier(neglectStreak: Int, tuning: PetCareTuning = PetCareTuning.DEFAULT): Double =
        (PERCENT - incomePenaltyPercent(neglectStreak, tuning)).toDouble() / PERCENT

    /**
     * @param neglectStreak сколько дней подряд питомцем не занимались.
     * @return Во сколько раз дороже необязательные товары: 1.0 — по обычной цене.
     */
    fun optionalPriceMultiplier(
        neglectStreak: Int,
        tuning: PetCareTuning = PetCareTuning.DEFAULT
    ): Double = (PERCENT + optionalMarkupPercent(neglectStreak, tuning)).toDouble() / PERCENT

    /**
     * Бонус дня с учётом штрафа: [incomeMultiplier], округлённый до целой монеты.
     *
     * @param base бонус дня без штрафа.
     * @param neglectStreak сколько дней подряд питомцем не занимались.
     * @return Сколько монет получит игрок.
     */
    fun dailyIncome(
        base: Int,
        neglectStreak: Int,
        tuning: PetCareTuning = PetCareTuning.DEFAULT
    ): Int = percentOf(base, PERCENT - incomePenaltyPercent(neglectStreak, tuning))

    /**
     * Цена товара с учётом наценки: необязательное дорожает по [optionalPriceMultiplier],
     * обязательное (еда, игрушки) — никогда, чтобы ребёнку всегда было чем накормить питомца.
     *
     * @param basePrice цена товара из каталога.
     * @param kind к чему относится покупка в плане: обязательное или необязательное.
     * @param neglectStreak сколько дней подряд питомцем не занимались.
     * @return Цена, которую игрок видит и платит, округлённая до целой монеты.
     */
    fun priceOf(
        basePrice: Int,
        kind: SpendKind,
        neglectStreak: Int,
        tuning: PetCareTuning = PetCareTuning.DEFAULT
    ): Int = when (kind) {
        SpendKind.MUST -> basePrice
        SpendKind.WANT -> percentOf(basePrice, PERCENT + optionalMarkupPercent(neglectStreak, tuning))
    }

    /**
     * Короткое доброе объяснение для ребёнка: почему питомец растёт медленнее и почему бонус
     * меньше, а наряды дороже.
     *
     * @param stats шкалы питомца сейчас: по самой пустой из них подсказка говорит, чего ему не
     * хватает.
     * @param dayBestCare лучший индекс ухода за текущий день питомца, см. [PetCare.dayBestCare]:
     * по нему решается, как питомец вырастет за этот день.
     * @param neglectStreak сколько дней подряд питомцем не занимались.
     * @return Текст подсказки, или null, когда всё хорошо и говорить не о чем.
     */
    fun explain(
        stats: PetStats,
        dayBestCare: Double,
        neglectStreak: Int,
        tuning: PetCareTuning = PetCareTuning.DEFAULT
    ): String? {
        val parts = mutableListOf<String>()
        val growth = growthMultiplierFor(dayBestCare, tuning)
        if (growth < 1.0) {
            val need = needOf(stats)
            parts += if (growth <= 0.0) {
                "Питомец $need и перестал расти. Позаботься о нём!"
            } else {
                "Питомец $need — он растёт медленнее."
            }
        }
        if (neglectStreak > 0) {
            parts += "Питомец ${daysText(neglectStreak)} без заботы — бонус дня меньше, " +
                "а наряды и декор дороже."
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    private fun incomePenaltyPercent(neglectStreak: Int, tuning: PetCareTuning): Int =
        penaltyPercent(neglectStreak, tuning.incomePenaltyPercentPerDay, tuning.maxIncomePenaltyPercent)

    private fun optionalMarkupPercent(neglectStreak: Int, tuning: PetCareTuning): Int =
        penaltyPercent(neglectStreak, tuning.optionalMarkupPercentPerDay, tuning.maxOptionalMarkupPercent)

    private fun penaltyPercent(streak: Int, perDay: Int, max: Int): Int {
        if (streak <= 0) return 0
        return (streak.toLong() * perDay).coerceIn(0L, max.toLong()).toInt()
    }

    /** [value] * [percent] / 100, округлённое до ближайшей целой монеты (половина — вверх). */
    private fun percentOf(value: Int, percent: Int): Int =
        ((value.toLong() * percent + PERCENT / 2) / PERCENT).toInt()

    /**
     * Чего питомцу не хватает больше всего — по самой пустой шкале. При равенстве сначала
     * называется то, что ребёнок может исправить сам: голод, потом скука, потом здоровье.
     */
    private fun needOf(stats: PetStats): String {
        val order = listOf(StatKind.HUNGER, StatKind.PLEASURE, StatKind.HEALTH)
        val weakest = StatKind.entries
            .sortedWith(compareBy({ stats[it] }, { order.indexOf(it) }))
            .first()
        return when (weakest) {
            StatKind.HUNGER -> "голодный"
            StatKind.PLEASURE -> "скучает"
            StatKind.HEALTH -> "приболел"
        }
    }

    /** «1 день», «2 дня», «5 дней», «21 день». */
    internal fun daysText(days: Int): String {
        val lastTwo = days % 100
        val last = days % 10
        val word = when {
            lastTwo in 11..14 -> "дней"
            last == 1 -> "день"
            last in 2..4 -> "дня"
            else -> "дней"
        }
        return "$days $word"
    }
}
