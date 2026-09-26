package com.legacy.fingame.game.quests

import com.legacy.fingame.game.stats.StatKind

/** Пределы и правила своих квестов взрослого. */
object CustomQuests {

    /** Больше своих квестов не хранится. */
    const val MAX = 20

    const val TITLE_MAX = 24

    const val DESCRIPTION_MAX = 120

    const val MIN_BALANCE_MAX = 999

    /** Сколько ситуаций (шагов) может быть в своём квесте. */
    const val STEPS_MIN = 1

    const val STEPS_MAX = 3

    const val STEP_TEXT_MAX = 120

    /** Задержки до следующего шага, в игровых минутах, — кнопки формы. */
    val DELAYS: List<Int> = listOf(0, 5, 30, 60)

    /** Сколько вариантов может быть в ситуации. */
    const val OPTIONS_MIN = 2

    const val OPTIONS_MAX = 3

    /** Длина надписи на кнопке варианта. */
    const val LABEL_MAX = 16

    /** Длина текста результата. */
    const val RESULT_MAX = 80

    const val MONEY_MIN = -100

    const val MONEY_MAX = 100

    const val MONEY_STEP = 10

    /** Пределы изменения настроения питомца. */
    const val MOOD_MIN = -20

    const val MOOD_MAX = 20

    const val MOOD_STEP = 5

    /** Сдвиги прогресса, из которых выбирает взрослый, в процентах. */
    val PROGRESS_STEPS: List<Int> = listOf(0, 25, 50, 100)

    /** С чего начинается id своего квеста: `custom-<момент>`. */
    const val ID_PREFIX = "custom-"

    /** @return Свой ли это квест взрослого. */
    fun isCustom(questId: String): Boolean = questId.startsWith(ID_PREFIX)

    /**
     * @param index номер ситуации, с нуля.
     * @return Id узла этой ситуации: `s1`, `s2`, `s3`.
     */
    fun nodeIdOf(index: Int): String = "s${index + 1}"

    /**
     * @param text что ввёл взрослый.
     * @return Текст без служебных знаков кодека и переводов строк, без пробелов по краям.
     */
    internal fun clean(text: String): String =
        text.filterNot {
            it == CustomQuestsCodec.RECORD_SEPARATOR || it == CustomQuestsCodec.FIELD_SEPARATOR ||
                it == '\n' || it == '\r'
        }.trim()
}

/**
 * Вариант ситуации в форме.
 *
 * @property label надпись на кнопке.
 * @property resultText что случилось после выбора.
 * @property moneyDelta монеты, со знаком.
 * @property moodDelta настроение питомца, со знаком.
 * @property progressDelta сдвиг прогресса квеста, в процентах.
 */
data class CustomQuestOptionDraft(
    val label: String = "",
    val resultText: String = "",
    val moneyDelta: Int = 0,
    val moodDelta: Int = 0,
    val progressDelta: Int = 0
)

/**
 * Ситуация (шаг) в форме.
 *
 * @property text описание ситуации.
 * @property delayMinutes через сколько игровых минут после выбора откроется следующий шаг.
 * @property options варианты, 2–3.
 */
data class CustomQuestStepDraft(
    val text: String = "",
    val delayMinutes: Int = 0,
    val options: List<CustomQuestOptionDraft> = List(CustomQuests.OPTIONS_MIN) { CustomQuestOptionDraft() }
)

/**
 * То, что взрослый набрал в форме «Новый квест».
 *
 * @property title название.
 * @property description описание.
 * @property minBalance сколько монет нужно на счёте, чтобы взять квест.
 * @property steps ситуации по порядку, 1–3.
 */
data class CustomQuestDraft(
    val title: String = "",
    val description: String = "",
    val minBalance: Int = 0,
    val steps: List<CustomQuestStepDraft> = listOf(CustomQuestStepDraft())
) {

    /** @return Что не так в названии, описании и минимуме; пусто, когда всё верно. */
    fun headerErrors(): List<String> {
        val errors = mutableListOf<String>()
        val cleanTitle = CustomQuests.clean(title)
        if (cleanTitle.isEmpty()) errors += "Впиши название"
        if (cleanTitle.length > CustomQuests.TITLE_MAX) {
            errors += "Название — не длиннее ${CustomQuests.TITLE_MAX} букв"
        }
        val cleanDescription = CustomQuests.clean(description)
        if (cleanDescription.isEmpty()) errors += "Впиши описание"
        if (cleanDescription.length > CustomQuests.DESCRIPTION_MAX) {
            errors += "Описание — не длиннее ${CustomQuests.DESCRIPTION_MAX} букв"
        }
        if (minBalance !in 0..CustomQuests.MIN_BALANCE_MAX) {
            errors += "Минимум монет — от 0 до ${CustomQuests.MIN_BALANCE_MAX}"
        }
        return errors
    }

    /**
     * @param index номер ситуации, с нуля.
     * @return Что не так в этой ситуации, с её номером в начале фразы; пусто, когда всё верно.
     */
    fun stepErrors(index: Int): List<String> {
        val step = steps.getOrNull(index) ?: return emptyList()
        val prefix = "Шаг ${index + 1}"
        val errors = mutableListOf<String>()
        val cleanText = CustomQuests.clean(step.text)
        if (cleanText.isEmpty()) errors += "$prefix: опиши ситуацию"
        if (cleanText.length > CustomQuests.STEP_TEXT_MAX) {
            errors += "$prefix: ситуация — не длиннее ${CustomQuests.STEP_TEXT_MAX} букв"
        }
        if (step.delayMinutes !in CustomQuests.DELAYS) errors += "$prefix: выбери задержку"
        if (step.options.size !in CustomQuests.OPTIONS_MIN..CustomQuests.OPTIONS_MAX) {
            errors += "$prefix: нужно ${CustomQuests.OPTIONS_MIN}–${CustomQuests.OPTIONS_MAX} варианта"
        }
        step.options.forEachIndexed { optionIndex, option ->
            val where = "$prefix, вариант ${optionIndex + 1}"
            val label = CustomQuests.clean(option.label)
            if (label.isEmpty()) errors += "$where: впиши текст кнопки"
            if (label.length > CustomQuests.LABEL_MAX) {
                errors += "$where: кнопка — не длиннее ${CustomQuests.LABEL_MAX} букв"
            }
            val result = CustomQuests.clean(option.resultText)
            if (result.isEmpty()) errors += "$where: впиши результат"
            if (result.length > CustomQuests.RESULT_MAX) {
                errors += "$where: результат — не длиннее ${CustomQuests.RESULT_MAX} букв"
            }
            if (option.moneyDelta !in CustomQuests.MONEY_MIN..CustomQuests.MONEY_MAX ||
                option.moneyDelta % CustomQuests.MONEY_STEP != 0
            ) {
                errors += "$where: монеты — от ${CustomQuests.MONEY_MIN} до +${CustomQuests.MONEY_MAX}"
            }
            if (option.moodDelta !in CustomQuests.MOOD_MIN..CustomQuests.MOOD_MAX ||
                option.moodDelta % CustomQuests.MOOD_STEP != 0
            ) {
                errors += "$where: настроение — от ${CustomQuests.MOOD_MIN} до +${CustomQuests.MOOD_MAX}"
            }
            if (option.progressDelta !in CustomQuests.PROGRESS_STEPS) {
                errors += "$where: выбери прогресс"
            }
        }
        return errors
    }

    /**
     * @param existingCount сколько своих квестов уже есть.
     * @return Что не так, короткими фразами для формы; пусто, когда квест можно сохранить.
     */
    fun validate(existingCount: Int = 0): List<String> {
        val errors = headerErrors().toMutableList()
        if (steps.size !in CustomQuests.STEPS_MIN..CustomQuests.STEPS_MAX) {
            errors += "Шагов — от ${CustomQuests.STEPS_MIN} до ${CustomQuests.STEPS_MAX}"
        }
        steps.indices.forEach { errors += stepErrors(it) }
        if (existingCount >= CustomQuests.MAX) errors += "Своих квестов — не больше ${CustomQuests.MAX}"
        return errors
    }

    /** Есть ли у квеста прогресс: хоть один вариант его двигает. */
    val hasProgress: Boolean get() = steps.any { step -> step.options.any { it.progressDelta > 0 } }

    /**
     * Собирает квест: линейная цепочка `s1 → s2 → … → конец`, каждый вариант ситуации ведёт к
     * следующей, у последней — к [Quest.END_NODE]. Тексты чистятся и обрезаются по пределам.
     *
     * @param id id квеста, `custom-<момент>`.
     * @return Квест игрока ([QuestKind.PLAYER]) без картинки.
     */
    fun toQuest(id: String): Quest {
        val nodes = steps.mapIndexed { index, step ->
            val nodeId = CustomQuests.nodeIdOf(index)
            val next = if (index == steps.lastIndex) Quest.END_NODE else CustomQuests.nodeIdOf(index + 1)
            nodeId to QuestNode(
                id = nodeId,
                text = CustomQuests.clean(step.text).take(CustomQuests.STEP_TEXT_MAX),
                options = step.options.map { option ->
                    QuestOption(
                        label = CustomQuests.clean(option.label).take(CustomQuests.LABEL_MAX),
                        resultText = CustomQuests.clean(option.resultText).take(CustomQuests.RESULT_MAX),
                        nextNodeId = next,
                        statEffects = if (option.moodDelta != 0) {
                            mapOf(StatKind.PLEASURE to option.moodDelta)
                        } else {
                            emptyMap()
                        },
                        moneyDelta = option.moneyDelta,
                        progressDelta = option.progressDelta
                    )
                },
                delayMinutes = step.delayMinutes
            )
        }.toMap()
        return Quest(
            id = id,
            title = CustomQuests.clean(title).take(CustomQuests.TITLE_MAX),
            description = CustomQuests.clean(description).take(CustomQuests.DESCRIPTION_MAX),
            kind = QuestKind.PLAYER,
            firstNodeId = CustomQuests.nodeIdOf(0),
            nodes = nodes,
            hasProgress = hasProgress,
            minBalance = minBalance
        )
    }

    companion object {

        /**
         * @param quest свой квест, собранный [toQuest].
         * @return Черновик, из которого он собран: ситуации — в порядке шагов квеста.
         */
        fun of(quest: Quest): CustomQuestDraft = CustomQuestDraft(
            title = quest.title,
            description = quest.description,
            minBalance = quest.minBalance,
            steps = quest.stepOrder.mapNotNull { quest.node(it) }.map { node ->
                CustomQuestStepDraft(
                    text = node.text,
                    delayMinutes = node.delayMinutes,
                    options = node.options.map { option ->
                        CustomQuestOptionDraft(
                            label = option.label,
                            resultText = option.resultText,
                            moneyDelta = option.moneyDelta,
                            moodDelta = option.statEffects[StatKind.PLEASURE] ?: 0,
                            progressDelta = option.progressDelta
                        )
                    }
                )
            }
        )
    }
}

/**
 * Свои квесты взрослого ([com.legacy.fingame.game.PlayerState.customQuests]) одной строкой: запись
 * от записи — `\u001E`, поле от поля — `\u001F`.
 *
 * Запись: id, название, описание, минимум, число шагов; затем у каждого шага — текст, задержка,
 * число вариантов; у каждого варианта — надпись, результат, монеты, настроение, прогресс. Связи
 * узлов не хранятся: квест линейный и собирается заново через [CustomQuestDraft.toQuest].
 */
object CustomQuestsCodec {

    const val RECORD_SEPARATOR = '\u001E'
    const val FIELD_SEPARATOR = '\u001F'

    /**
     * @param quests свои квесты.
     * @return Одна строка, не больше [CustomQuests.MAX] записей; пустая, когда квестов нет.
     */
    fun encode(quests: List<Quest>): String =
        quests.take(CustomQuests.MAX).joinToString(RECORD_SEPARATOR.toString()) { quest ->
            (listOf(CustomQuests.clean(quest.id)) + draftFields(CustomQuestDraft.of(quest), CustomQuests::clean))
                .joinToString(FIELD_SEPARATOR.toString())
        }

    /**
     * Читает то, что написал [encode]. Запись, которая не разбирается или не проходит проверку,
     * отбрасывается; повторы id — тоже.
     *
     * @param raw строка из настроек, или null, когда её не было.
     * @return Квесты в сохранённом порядке, не больше [CustomQuests.MAX].
     */
    fun decode(raw: String?): List<Quest> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(RECORD_SEPARATOR)
            .mapNotNull { record ->
                val fields = record.split(FIELD_SEPARATOR)
                val id = fields.firstOrNull()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val draft = draftOf(fields.drop(1)) ?: return@mapNotNull null
                if (draft.validate().isNotEmpty()) return@mapNotNull null
                draft.toQuest(id)
            }
            .distinctBy { it.id }
            .take(CustomQuests.MAX)
    }

    /**
     * Черновик формы одной строкой — чтобы форма пережила поворот экрана. Тексты сохраняются как
     * набраны (с пробелами по краям и пустые), убираются только служебные знаки.
     */
    fun encodeDraft(draft: CustomQuestDraft): String =
        draftFields(draft, ::stripSeparators).joinToString(FIELD_SEPARATOR.toString())

    /** @return Черновик из [encodeDraft], или null, когда строка не разбирается. */
    fun decodeDraft(raw: String): CustomQuestDraft? = draftOf(raw.split(FIELD_SEPARATOR))

    private fun stripSeparators(text: String): String =
        text.filterNot { it == RECORD_SEPARATOR || it == FIELD_SEPARATOR }

    private fun draftFields(draft: CustomQuestDraft, clean: (String) -> String): List<String> = buildList {
        add(clean(draft.title))
        add(clean(draft.description))
        add(draft.minBalance.toString())
        add(draft.steps.size.toString())
        draft.steps.forEach { step ->
            add(clean(step.text))
            add(step.delayMinutes.toString())
            add(step.options.size.toString())
            step.options.forEach { option ->
                add(clean(option.label))
                add(clean(option.resultText))
                add(option.moneyDelta.toString())
                add(option.moodDelta.toString())
                add(option.progressDelta.toString())
            }
        }
    }

    private fun draftOf(fields: List<String>): CustomQuestDraft? {
        var cursor = 0
        fun next(): String? = fields.getOrNull(cursor++)
        fun nextInt(): Int? = next()?.toIntOrNull()

        val title = next() ?: return null
        val description = next() ?: return null
        val minBalance = nextInt() ?: return null
        val stepCount = nextInt()?.takeIf { it in 0..CustomQuests.STEPS_MAX } ?: return null
        val steps = List(stepCount) {
            val text = next() ?: return null
            val delay = nextInt() ?: return null
            val optionCount = nextInt()?.takeIf { it in 0..CustomQuests.OPTIONS_MAX } ?: return null
            val options = List(optionCount) {
                CustomQuestOptionDraft(
                    label = next() ?: return null,
                    resultText = next() ?: return null,
                    moneyDelta = nextInt() ?: return null,
                    moodDelta = nextInt() ?: return null,
                    progressDelta = nextInt() ?: return null
                )
            }
            CustomQuestStepDraft(text, delay, options)
        }
        if (cursor != fields.size) return null
        return CustomQuestDraft(title, description, minBalance, steps)
    }
}

/**
 * Квесты игры и, после них, свои квесты взрослого.
 *
 * @param base квесты игры из данных.
 * @param custom свои квесты — читаются при каждом обращении, так что добавленный взрослым квест
 * сразу на экране.
 */
class CompositeQuestCatalog(
    private val base: QuestCatalog,
    private val custom: () -> List<Quest>
) : QuestCatalog {

    override val quests: List<Quest> get() = base.quests + custom()

    override fun findQuestById(questId: String): Quest? =
        base.findQuestById(questId) ?: custom().find { it.id == questId }
}
