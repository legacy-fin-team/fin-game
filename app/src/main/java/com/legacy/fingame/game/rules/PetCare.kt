package com.legacy.fingame.game.rules

import com.legacy.fingame.game.animals.Growth
import com.legacy.fingame.game.stats.PetStats

/**
 * Как питомцу жилось: сколько он вырос и сколько дней подряд им не занимались.
 *
 * Жизнь питомца делится на его собственные дни — по [Growth.STAGE_MILLIS] от момента, когда его
 * взяли, а не по календарю: так день питомца всегда длится ровно сутки, и хорошо ухоженный питомец
 * растёт ровно так же, как рос до этих правил — на стадию в сутки.
 *
 * Каждый прожитый день судится по лучшему индексу ухода, который питомец набрал за этот день
 * ([dayBestCare]): ребёнку достаточно хотя бы раз за день накормить питомца и поиграть с ним, а не
 * держать шкалы полными круглые сутки, — шкалы падают быстро, и пока приложение закрыто, они всё
 * равно пустеют. Как день превращается в рост и в серию, решает [PetCareRules].
 *
 * @property growthMillis сколько питомец вырос, в миллисекундах «нормального» роста: стадия — это
 * [Growth.STAGE_MILLIS] такого роста (см. [Growth.ageOf]).
 * @property judgedDays сколько дней питомца уже закрыто и учтено в [growthMillis] и
 * [neglectStreak]; текущий день — следующий за ними.
 * @property dayBestCare лучший индекс ухода за текущий день питомца (см. [PetCareRules.careIndex]).
 * @property neglectStreak сколько закрытых дней подряд, считая от последнего, питомцем не
 * занимались (см. [PetCareRules.isNeglected]). Хороший день обнуляет серию.
 */
data class PetCare(
    val growthMillis: Long = 0L,
    val judgedDays: Long = 0L,
    val dayBestCare: Double = PetCareRules.FULL_CARE,
    val neglectStreak: Int = 0
) {
    companion object {
        /**
         * Длиннее серия не копится: штрафы упираются в свои потолки гораздо раньше, а счётчик не
         * должен переполниться, если часы устройства улетели на годы вперёд.
         */
        const val MAX_STREAK = 365

        /**
         * Уход питомца из сохранения, сделанного до этих правил: питомец сохраняет возраст, который
         * успел набрать по старому правилу (каждые прожитые сутки — стадия), а серия начинается
         * с нуля — задним числом никого не наказываем.
         *
         * @param bornAtMillis момент, когда питомца взяли, или [Growth.NOT_BORN].
         * @param nowMillis текущий момент.
         * @param stats шкалы питомца сейчас: с них начинается текущий день.
         */
        fun migrated(bornAtMillis: Long, nowMillis: Long, stats: PetStats): PetCare {
            val days = if (bornAtMillis == Growth.NOT_BORN || nowMillis <= bornAtMillis) {
                0L
            } else {
                (nowMillis - bornAtMillis) / Growth.STAGE_MILLIS
            }
            return PetCare(
                growthMillis = days * Growth.STAGE_MILLIS,
                judgedDays = days,
                dayBestCare = PetCareRules.careIndex(stats),
                neglectStreak = 0
            )
        }
    }

    /**
     * Запоминает шкалы, до которых питомца только что довели (накормили, поиграли): если они
     * лучше всего, что было за день, день засчитывается по ним.
     */
    fun noticed(stats: PetStats): PetCare {
        val index = PetCareRules.careIndex(stats)
        return if (index > dayBestCare) copy(dayBestCare = index) else this
    }

    /**
     * Закрывает все дни питомца, которые успели закончиться к [nowMillis].
     *
     * Каждый новый день начинается с тех шкал, до которых питомец успел опустеть к его началу:
     * они считаются из шкал на момент [statsAtMillis] тем же распадом, что и сами шкалы (см.
     * [PetStats.decayedBy]), так что приложение, закрытое на несколько дней, судит каждый из них
     * честно. Когда шкалы опустели до конца, все оставшиеся дни одинаковы и учитываются разом.
     *
     * @param bornAtMillis момент, когда питомца взяли, или [Growth.NOT_BORN] — тогда жить некому.
     * @param stats шкалы питомца на момент [statsAtMillis].
     * @param statsAtMillis момент, к которому относятся [stats].
     * @param nowMillis текущий момент.
     */
    fun lived(
        bornAtMillis: Long,
        stats: PetStats,
        statsAtMillis: Long,
        nowMillis: Long,
        tuning: PetCareTuning = PetCareTuning.DEFAULT
    ): PetCare {
        if (bornAtMillis == Growth.NOT_BORN || nowMillis <= bornAtMillis) return this
        val dueDays = (nowMillis - bornAtMillis) / Growth.STAGE_MILLIS

        var care = this
        var previousStart: PetStats? = null
        while (care.judgedDays < dueDays) {
            val dayStart = bornAtMillis + (care.judgedDays + 1) * Growth.STAGE_MILLIS
            val startStats = stats.decayedBy(PetStats.ticksBetween(statsAtMillis, dayStart))
            if (startStats == previousStart) {
                // Шкалы больше не меняются: каждый оставшийся день такой же, как этот.
                return care.closed(dueDays - care.judgedDays, tuning)
            }
            care = care.closed(1, tuning).copy(dayBestCare = PetCareRules.careIndex(startStats))
            previousStart = startStats
        }
        return care
    }

    /**
     * Закрывает [days] одинаковых дней с индексом [dayBestCare]: следующий день начинается с того
     * же индекса.
     */
    private fun closed(days: Long, tuning: PetCareTuning): PetCare {
        val growth = PetCareRules.growthMultiplierFor(dayBestCare, tuning)
        val gained = (Growth.STAGE_MILLIS * growth).toLong()
        val streak = if (PetCareRules.isNeglected(dayBestCare, tuning)) {
            (neglectStreak + days).coerceAtMost(MAX_STREAK.toLong()).toInt()
        } else {
            0
        }
        return copy(
            growthMillis = saturatedAdd(growthMillis, saturatedMultiply(gained, days)),
            judgedDays = judgedDays + days,
            neglectStreak = streak
        )
    }

    private fun saturatedMultiply(a: Long, b: Long): Long =
        if (a != 0L && b > Long.MAX_VALUE / a) Long.MAX_VALUE else a * b

    private fun saturatedAdd(a: Long, b: Long): Long =
        if (b > 0 && a > Long.MAX_VALUE - b) Long.MAX_VALUE else a + b
}
