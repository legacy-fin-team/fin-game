package com.legacy.fingame.game.adult

import kotlin.random.Random

/** Наименьший множитель в примере замка: таблица умножения без «на один» и «на два». */
const val LOCK_MIN_FACTOR = 3

/** Наибольший множитель в примере замка. */
const val LOCK_MAX_FACTOR = 9

/** Сколько примеров показывает замок. */
const val LOCK_TASK_COUNT = 3

/**
 * Пример на умножение перед режимом взрослого. Защищает от случайного нажатия ребёнком, а не от
 * взлома: ничего не хранит и каждый раз новый.
 *
 * @property a первый множитель.
 * @property b второй множитель.
 */
data class LockTask(val a: Int, val b: Int) {
    /** Правильный ответ. */
    val answer: Int get() = a * b
}

/**
 * @param random кости; в тестах — заранее заданные.
 * @param count сколько примеров нужно.
 * @return [count] примеров с множителями от [LOCK_MIN_FACTOR] до [LOCK_MAX_FACTOR].
 */
fun lockTasksOf(random: Random, count: Int = LOCK_TASK_COUNT): List<LockTask> =
    List(count) {
        LockTask(
            a = random.nextInt(LOCK_MIN_FACTOR, LOCK_MAX_FACTOR + 1),
            b = random.nextInt(LOCK_MIN_FACTOR, LOCK_MAX_FACTOR + 1)
        )
    }

/**
 * @param tasks примеры замка.
 * @param answers что ввёл взрослый, по примеру на строку; пустая строка — ответа ещё нет.
 * @return Решены ли все примеры: ответов столько же, сколько примеров, и каждый верный.
 */
fun isSolved(tasks: List<LockTask>, answers: List<String>): Boolean =
    tasks.size == answers.size &&
        tasks.zip(answers).all { (task, answer) -> answer.toIntOrNull() == task.answer }
