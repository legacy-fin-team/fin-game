package com.legacy.fingame.game.quests

import com.legacy.fingame.game.stats.StatKind

/**
 * Кто начинает квест.
 *
 * @property xmlName как вид квеста записан в атрибуте `kind` файла `data/quests.xml`.
 */
enum class QuestKind(val xmlName: String) {
    /** Квест берёт сам игрок кнопкой «Взять» на экране квестов. */
    PLAYER("player"),

    /** Квест выпадает сам, пока игрок занят питомцем (см. [QuestEngine.maybeSpawnRandom]). */
    RANDOM("random");

    companion object {

        /**
         * @param value значение атрибута `kind`.
         * @return Вид квеста с этим именем, или null, когда такого вида нет.
         */
        fun fromString(value: String): QuestKind? =
            entries.find { it.xmlName.equals(value.trim(), ignoreCase = true) }
    }
}

/**
 * Один вариант действия в ситуации квеста — то, что написано на кнопке.
 *
 * @property label надпись на кнопке.
 * @property resultText что случилось после выбора; игрок видит это только после нажатия.
 * @property nextNodeId узел, в который ведёт вариант, или [Quest.END_NODE], когда квест на этом
 * заканчивается.
 * @property statEffects на сколько меняются полоски питомца, со знаком.
 * @property moneyDelta сколько монет добавляется (плюс) или списывается (минус).
 * @property progressDelta на сколько процентов двигается прогресс квеста; учитывается только у
 * квестов с [Quest.hasProgress].
 */
data class QuestOption(
    val label: String,
    val resultText: String,
    val nextNodeId: String,
    val statEffects: Map<StatKind, Int> = emptyMap(),
    val moneyDelta: Int = 0,
    val progressDelta: Int = 0
)

/**
 * Одна ситуация квеста.
 *
 * @property id идентификатор узла внутри квеста.
 * @property text описание ситуации.
 * @property options варианты действия, минимум один.
 * @property delayMinutes через сколько игровых минут после выбора станет доступен СЛЕДУЮЩИЙ узел;
 * ноль — сразу.
 * @property imagePath картинка ситуации относительно `assets/textures/`, или null, когда её нет.
 */
data class QuestNode(
    val id: String,
    val text: String,
    val options: List<QuestOption>,
    val delayMinutes: Int = 0,
    val imagePath: String? = null
)

/**
 * Квест: набор ситуаций, через которые проходит игрок.
 *
 * @property id идентификатор квеста.
 * @property title название на карточке.
 * @property description описание, видно в раскрытой карточке.
 * @property kind кто начинает квест.
 * @property firstNodeId узел, с которого квест начинается.
 * @property nodes все узлы квеста по id, в порядке файла.
 * @property hasProgress есть ли у квеста прогресс 0..100 %, который двигают выборы игрока.
 * @property minBalance сколько монет должно лежать на счёте, чтобы взять квест. Не тратится.
 * @property imagePath картинка квеста относительно `assets/textures/`, или null.
 * @property repeatable можно ли брать квест снова после того, как он пройден. У квеста с
 * `false` после первого прохождения [QuestEngine.canStart] откажет, пока взрослый не вызовет
 * [QuestEngine.enable] — экран для этого будет в другой ветке.
 * @property cooldownMinutes сколько реальных минут должно пройти с момента завершения квеста до
 * следующей возможности его начать. Считается по настенным часам игры ([nowMillis] движка — тем
 * же самым, каким устройство меряет реальное время, лишь бы демо-сборка его не подвинула), а не по
 * условным «игровым» единицам: кулдаун нельзя обойти, ускорив квест другим способом. Действует
 * только когда [repeatable] истинно; для одноразового квеста блокировка снимается не временем, а
 * только через [QuestEngine.enable].
 * @property stageDelayMinutes сколько реальных минут ждать по умолчанию между этапами квеста —
 * то же самое время, что и [cooldownMinutes]. Это дефолт для узла, у которого нет своего
 * [QuestNode.delayMinutes]; узел со своим значением задержки его переопределяет.
 * @property requiresAdultCheck проверяет ли выполнение взрослый. Тогда выбор варианта не
 * засчитывается сам: этап ждёт проверки ([QuestCheck.WAITING]), награда (деньги, шкалы питомца,
 * прогресс) выдаётся только после «Засчитать» во взрослом режиме ([QuestEngine.approve]), а «Не
 * засчитано» возвращает этап в работу ([QuestEngine.reject]).
 */
data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val kind: QuestKind,
    val firstNodeId: String,
    val nodes: Map<String, QuestNode>,
    val hasProgress: Boolean = false,
    val minBalance: Int = 0,
    val imagePath: String? = null,
    val repeatable: Boolean = true,
    val cooldownMinutes: Int = DEFAULT_COOLDOWN_MINUTES,
    val stageDelayMinutes: Int = 0,
    val requiresAdultCheck: Boolean = false
) {

    /**
     * Узлы в порядке шагов: от первого, по вариантам в порядке файла, каждый узел один раз.
     * У линейного квеста это ровно его путь; у ветвящегося — порядок обхода в ширину.
     */
    val stepOrder: List<String> by lazy {
        val order = mutableListOf<String>()
        val queue = ArrayDeque(listOf(firstNodeId))
        while (queue.isNotEmpty()) {
            val nodeId = queue.removeFirst()
            if (nodeId == END_NODE || nodeId in order) continue
            val node = nodes[nodeId] ?: continue
            order += nodeId
            node.options.forEach { queue.addLast(it.nextNodeId) }
        }
        order.toList()
    }

    /** Сколько шагов в квесте — знаменатель «Шаг 2 из 3». */
    val stepCount: Int get() = stepOrder.size

    /**
     * @param nodeId узел квеста.
     * @return Номер шага этого узла, начиная с 1, или 0, когда такого узла в квесте нет.
     */
    fun stepNumberOf(nodeId: String): Int = stepOrder.indexOf(nodeId) + 1

    /**
     * @param nodeId узел квеста.
     * @return Узел, или null, когда его нет (например, данные поменялись между запусками).
     */
    fun node(nodeId: String): QuestNode? = nodes[nodeId]

    companion object {

        /** Условный узел «конец квеста»: вариант с `next="end"` завершает квест. */
        const val END_NODE = "end"

        /** Наименьший прогресс квеста. */
        const val MIN_PROGRESS = 0

        /** Наибольший прогресс квеста. */
        const val MAX_PROGRESS = 100

        /** Дефолт [Quest.cooldownMinutes], когда `cooldown-minutes` нет в данных. */
        const val DEFAULT_COOLDOWN_MINUTES = 60
    }
}
