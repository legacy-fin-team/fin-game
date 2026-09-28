package com.legacy.fingame.game.adult

/**
 * Правила ручного изменения монет ребёнка взрослым: сколько, в какую сторону и почему.
 *
 * Баланс не уходит ниже нуля: взрослый может убрать не больше, чем есть на текущем счёте (вклад
 * не трогается), — так у ребёнка не бывает «долга», которого в игре нет.
 */
object AdultMoney {

    /** Самая большая сумма за один раз. */
    const val AMOUNT_MAX = 999

    /** Длина причины. */
    const val REASON_MAX = 60

    /**
     * @param reason что вписал взрослый.
     * @return Причина без переводов строк и служебных знаков, без пробелов по краям, не длиннее
     * [REASON_MAX].
     */
    fun cleanReason(reason: String): String =
        reason.filterNot { it == '\n' || it == '\r' || it.code < 0x20 }.trim().take(REASON_MAX)

    /**
     * @param amount сумма, без знака.
     * @param add добавить (true) или убрать (false).
     * @param reason причина.
     * @param balance что сейчас на текущем счёте ребёнка.
     * @return Что не так, короткими фразами для формы; пусто, когда изменение можно записать.
     */
    fun validate(amount: Int, add: Boolean, reason: String, balance: Int): List<String> {
        val errors = mutableListOf<String>()
        if (amount !in 1..AMOUNT_MAX) errors += "Сумма — от 1 до $AMOUNT_MAX"
        if (!add && amount > balance) errors += "Можно убрать не больше, чем есть на счёте: $balance"
        if (cleanReason(reason).isEmpty()) errors += "Впиши причину"
        return errors
    }
}
