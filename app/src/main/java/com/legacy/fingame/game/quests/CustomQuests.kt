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

    /** Самая длинная пауза между этапами — сутки, в минутах. */
    const val STAGE_DELAY_MAX = 24 * 60

    /**
     * Значения паузы между этапами, по которым ходят кнопки «−/+» формы, в минутах. Своё
     * значение из этого ряда не обязано — годится любое от нуля до [STAGE_DELAY_MAX].
     */
    val STAGE_DELAYS: List<Int> = listOf(0, 1, 2, 5, 10, 15, 30, 45, 60, 90, 120, 180, 240, 360, 720, 1440)

    /** Самый длинный кулдаун многоразового квеста — неделя, в минутах. */
    const val COOLDOWN_MAX = 7 * 24 * 60

    /** Значения кулдауна для кнопок «−/+» формы, в минутах; как и паузе, ряд — лишь шаги. */
    val COOLDOWNS: List<Int> = listOf(0, 5, 10, 15, 30, 60, 120, 180, 360, 720, 1440, 2880, 4320, 10080)

    /**
     * @param presets ряд значений по возрастанию.
     * @param value текущее значение.
     * @return Ближайшее значение ряда больше [value], или [value], когда больше нет.
     */
    fun nextPreset(presets: List<Int>, value: Int): Int = presets.firstOrNull { it > value } ?: value

    /**
     * @param presets ряд значений по возрастанию.
     * @param value текущее значение.
     * @return Ближайшее значение ряда меньше [value], или [value], когда меньше нет.
     */
    fun previousPreset(presets: List<Int>, value: Int): Int = presets.lastOrNull { it < value } ?: value

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

    /** Пределы изменения любой шкалы питомца — сытости, здоровья, настроения. */
    const val STAT_MIN = -20

    const val STAT_MAX = 20

    const val STAT_STEP = 5

    /** Сдвиги прогресса, из которых выбирает взрослый, в процентах. */
    val PROGRESS_STEPS: List<Int> = listOf(0, 25, 50, 100)

    /** С чего начинается id своего квеста: `custom-<момент>`. */
    const val ID_PREFIX = "custom-"

    /** @return Свой ли это квест взрослого. */
    fun isCustom(questId: String): Boolean = questId.startsWith(ID_PREFIX)

    /** Шкалы питомца в порядке строк формы: сытость, здоровье, настроение. */
    val STATS: List<StatKind> = listOf(StatKind.HUNGER, StatKind.HEALTH, StatKind.PLEASURE)

    /** @return Как шкала называется в форме: «Сытость», «Здоровье», «Настроение». */
    fun statTitle(stat: StatKind): String = when (stat) {
        StatKind.HUNGER -> "Сытость"
        StatKind.HEALTH -> "Здоровье"
        StatKind.PLEASURE -> "Настроение"
    }

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
 * @property moodDelta настроение питомца ([StatKind.PLEASURE]), со знаком.
 * @property progressDelta сдвиг прогресса квеста, в процентах.
 * @property hungerDelta сытость питомца ([StatKind.HUNGER]), со знаком.
 * @property healthDelta здоровье питомца ([StatKind.HEALTH]), со знаком.
 */
data class CustomQuestOptionDraft(
    val label: String = "",
    val resultText: String = "",
    val moneyDelta: Int = 0,
    val moodDelta: Int = 0,
    val progressDelta: Int = 0,
    val hungerDelta: Int = 0,
    val healthDelta: Int = 0
) {

    /** @return Изменение шкалы [stat] этим вариантом, со знаком. */
    fun deltaOf(stat: StatKind): Int = when (stat) {
        StatKind.HUNGER -> hungerDelta
        StatKind.HEALTH -> healthDelta
        StatKind.PLEASURE -> moodDelta
    }

    /** @return Этот вариант, у которого шкала [stat] меняется на [value]. */
    fun withDelta(stat: StatKind, value: Int): CustomQuestOptionDraft = when (stat) {
        StatKind.HUNGER -> copy(hungerDelta = value)
        StatKind.HEALTH -> copy(healthDelta = value)
        StatKind.PLEASURE -> copy(moodDelta = value)
    }

    /** @return Эффекты варианта на шкалы питомца, без нулевых — как их получит [QuestOption]. */
    fun statEffects(): Map<StatKind, Int> =
        StatKind.entries.associateWith { deltaOf(it) }.filterValues { it != 0 }

    companion object {

        /**
         * @param effects эффекты на шкалы питомца, как в [QuestOption.statEffects].
         * @return Вариант формы с этими эффектами; шкалы, которых форма не знает, пропускаются.
         */
        fun of(
            label: String,
            resultText: String,
            moneyDelta: Int,
            effects: Map<StatKind, Int>,
            progressDelta: Int
        ): CustomQuestOptionDraft = CustomQuestOptionDraft(
            label = label,
            resultText = resultText,
            moneyDelta = moneyDelta,
            moodDelta = effects[StatKind.PLEASURE] ?: 0,
            progressDelta = progressDelta,
            hungerDelta = effects[StatKind.HUNGER] ?: 0,
            healthDelta = effects[StatKind.HEALTH] ?: 0
        )
    }
}

/**
 * Ситуация (шаг) в форме. Пауза до следующей ситуации у всех шагов одна — квестовая
 * [CustomQuestDraft.stageDelayMinutes].
 *
 * @property text описание ситуации.
 * @property options варианты, 2–3.
 */
data class CustomQuestStepDraft(
    val text: String = "",
    val options: List<CustomQuestOptionDraft> = List(CustomQuests.OPTIONS_MIN) { CustomQuestOptionDraft() }
)

/**
 * То, что взрослый набрал в форме «Новый квест».
 *
 * @property title название.
 * @property description описание.
 * @property minBalance сколько монет нужно на счёте, чтобы взять квест.
 * @property steps ситуации по порядку, 1–3.
 * @property repeatable многоразовый ли квест ([Quest.repeatable]); одноразовый после прохождения
 * снова доступен, только когда взрослый включит его («Включить снова»).
 * @property cooldownMinutes сколько минут многоразовый квест ждёт после прохождения
 * ([Quest.cooldownMinutes]); у одноразового не используется.
 * @property stageDelayMinutes пауза между этапами, в минутах ([Quest.stageDelayMinutes]): через
 * столько после выбора откроется следующая ситуация.
 * @property requiresAdultCheck проверяет ли выполнение взрослый ([Quest.requiresAdultCheck]).
 * @property topic тема финансовой грамотности ([Quest.topic]), или null — без темы.
 */
data class CustomQuestDraft(
    val title: String = "",
    val description: String = "",
    val minBalance: Int = 0,
    val steps: List<CustomQuestStepDraft> = listOf(CustomQuestStepDraft()),
    val repeatable: Boolean = true,
    val cooldownMinutes: Int = Quest.DEFAULT_COOLDOWN_MINUTES,
    val stageDelayMinutes: Int = 0,
    val requiresAdultCheck: Boolean = false,
    val topic: QuestTopic? = null
) {

    /** @return Что не так в правилах квеста — паузе и кулдауне; пусто, когда всё верно. */
    fun rulesErrors(): List<String> {
        val errors = mutableListOf<String>()
        if (stageDelayMinutes !in 0..CustomQuests.STAGE_DELAY_MAX) {
            errors += "Пауза между этапами — от 0 до ${CustomQuests.STAGE_DELAY_MAX} минут"
        }
        if (repeatable && cooldownMinutes !in 0..CustomQuests.COOLDOWN_MAX) {
            errors += "Кулдаун — от 0 до ${CustomQuests.COOLDOWN_MAX} минут"
        }
        return errors
    }

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
            StatKind.entries.forEach { stat ->
                val delta = option.deltaOf(stat)
                if (delta !in CustomQuests.STAT_MIN..CustomQuests.STAT_MAX || delta % CustomQuests.STAT_STEP != 0) {
                    errors += "$where: ${CustomQuests.statTitle(stat).lowercase()} — от ${CustomQuests.STAT_MIN} " +
                        "до +${CustomQuests.STAT_MAX}"
                }
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
        val errors = (headerErrors() + rulesErrors()).toMutableList()
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
     * следующей, у последней — к [Quest.END_NODE]. Тексты чистятся и обрезаются по пределам. Пауза
     * между этапами записывается и в квест, и в каждый узел — движок берёт её у узла.
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
                        statEffects = option.statEffects(),
                        moneyDelta = option.moneyDelta,
                        progressDelta = option.progressDelta
                    )
                },
                delayMinutes = stageDelayMinutes
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
            minBalance = minBalance,
            repeatable = repeatable,
            cooldownMinutes = cooldownMinutes,
            stageDelayMinutes = stageDelayMinutes,
            requiresAdultCheck = requiresAdultCheck,
            topic = topic
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
            repeatable = quest.repeatable,
            cooldownMinutes = quest.cooldownMinutes,
            stageDelayMinutes = quest.stageDelayMinutes,
            requiresAdultCheck = quest.requiresAdultCheck,
            topic = quest.topic,
            steps = quest.stepOrder.mapNotNull { quest.node(it) }.map { node ->
                CustomQuestStepDraft(
                    text = node.text,
                    options = node.options.map { option ->
                        CustomQuestOptionDraft.of(
                            label = option.label,
                            resultText = option.resultText,
                            moneyDelta = option.moneyDelta,
                            effects = option.statEffects,
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
 * Запись нынешнего вида (v2): id, метка версии [VERSION_2], свойства квеста (`ключ=значение`
 * через `;`: многоразовый, кулдаун, пауза между этапами), название, описание, минимум, число
 * шагов (свойства: многоразовый, кулдаун, пауза между этапами, проверка взрослым, тема); затем у каждого
 * шага — текст и число вариантов; у каждого варианта — надпись, результат,
 * монеты, эффекты на шкалы питомца (`pleasure=-5;hunger=10`) и прогресс. Свойство или эффект с
 * незнакомым ключом пропускается: так новые свойства добавляются без новой версии записи.
 *
 * Запись прежнего вида (v1, без метки): id, название, описание, минимум, число шагов; у шага —
 * текст, своя задержка, число вариантов; у варианта — надпись, результат, монеты, настроение,
 * прогресс. Она читается и переводится в нынешний вид: квест многоразовый с кулдауном по
 * умолчанию, пауза между этапами — наибольшая из задержек его шагов (у квеста из формы
 * одна пауза на все этапы).
 *
 * Связи узлов не хранятся: квест линейный и собирается заново через [CustomQuestDraft.toQuest].
 */
object CustomQuestsCodec {

    const val RECORD_SEPARATOR = '\u001E'
    const val FIELD_SEPARATOR = '\u001F'

    /**
     * Метка записи v2 сразу после id. Начинается с управляющего символа, который в форме не
     * набрать, — старая запись с таким названием невозможна.
     */
    internal const val VERSION_2 = "\u0002v2"

    private const val PAIR_SEPARATOR = ';'
    private const val VALUE_SEPARATOR = '='

    private const val KEY_REPEATABLE = "repeatable"
    private const val KEY_COOLDOWN = "cooldown"
    private const val KEY_STAGE_DELAY = "stage-delay"
    private const val KEY_ADULT_CHECK = "adult-check"
    private const val KEY_TOPIC = "topic"

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
     * Читает то, что написал [encode], — и записи прежнего вида (v1). Запись, которая не
     * разбирается или не проходит проверку, отбрасывается; повторы id — тоже.
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

    /** Текст без служебных знаков пар `ключ=значение`: они идут в поле свойств или эффектов. */
    private fun pairsOf(pairs: List<Pair<String, String>>): String =
        pairs.joinToString(PAIR_SEPARATOR.toString()) { (key, value) ->
            "$key$VALUE_SEPARATOR${value.filterNot { it == PAIR_SEPARATOR || it == VALUE_SEPARATOR }}"
        }

    /** @return Пары из [pairsOf]; пара без `=` пропускается. */
    private fun decodePairs(text: String): Map<String, String> =
        text.split(PAIR_SEPARATOR).mapNotNull { pair ->
            val parts = pair.split(VALUE_SEPARATOR)
            if (parts.size != 2 || parts[0].isBlank()) null else parts[0] to parts[1]
        }.toMap()

    private fun propertiesOf(draft: CustomQuestDraft): List<Pair<String, String>> = listOf(
        KEY_REPEATABLE to if (draft.repeatable) "1" else "0",
        KEY_COOLDOWN to draft.cooldownMinutes.toString(),
        KEY_STAGE_DELAY to draft.stageDelayMinutes.toString(),
        KEY_ADULT_CHECK to if (draft.requiresAdultCheck) "1" else "0",
        KEY_TOPIC to draft.topic?.xmlName.orEmpty()
    )

    private fun effectsOf(option: CustomQuestOptionDraft): List<Pair<String, String>> =
        option.statEffects().map { (stat, value) -> stat.xmlName to value.toString() }

    private fun draftFields(draft: CustomQuestDraft, clean: (String) -> String): List<String> = buildList {
        add(VERSION_2)
        add(pairsOf(propertiesOf(draft)))
        add(clean(draft.title))
        add(clean(draft.description))
        add(draft.minBalance.toString())
        add(draft.steps.size.toString())
        draft.steps.forEach { step ->
            add(clean(step.text))
            add(step.options.size.toString())
            step.options.forEach { option ->
                add(clean(option.label))
                add(clean(option.resultText))
                add(option.moneyDelta.toString())
                add(pairsOf(effectsOf(option)))
                add(option.progressDelta.toString())
            }
        }
    }

    private fun draftOf(fields: List<String>): CustomQuestDraft? =
        if (fields.firstOrNull() == VERSION_2) draftOfV2(fields.drop(1)) else draftOfV1(fields)

    private fun draftOfV2(fields: List<String>): CustomQuestDraft? {
        var cursor = 0
        fun next(): String? = fields.getOrNull(cursor++)
        fun nextInt(): Int? = next()?.toIntOrNull()

        val properties = decodePairs(next() ?: return null)
        val title = next() ?: return null
        val description = next() ?: return null
        val minBalance = nextInt() ?: return null
        val stepCount = nextInt()?.takeIf { it in 0..CustomQuests.STEPS_MAX } ?: return null
        val steps = List(stepCount) {
            val text = next() ?: return null
            val optionCount = nextInt()?.takeIf { it in 0..CustomQuests.OPTIONS_MAX } ?: return null
            val options = List(optionCount) {
                val label = next() ?: return null
                val resultText = next() ?: return null
                val moneyDelta = nextInt() ?: return null
                val effects = decodePairs(next() ?: return null)
                val progressDelta = nextInt() ?: return null
                CustomQuestOptionDraft.of(
                    label = label,
                    resultText = resultText,
                    moneyDelta = moneyDelta,
                    effects = effects.mapNotNull { (key, value) ->
                        val stat = StatKind.fromString(key) ?: return@mapNotNull null
                        stat to (value.toIntOrNull() ?: return null)
                    }.toMap(),
                    progressDelta = progressDelta
                )
            }
            CustomQuestStepDraft(text, options)
        }
        if (cursor != fields.size) return null
        val defaults = CustomQuestDraft()
        return CustomQuestDraft(
            title = title,
            description = description,
            minBalance = minBalance,
            steps = steps,
            repeatable = flagOf(properties[KEY_REPEATABLE], defaults.repeatable) ?: return null,
            requiresAdultCheck = flagOf(properties[KEY_ADULT_CHECK], defaults.requiresAdultCheck)
                ?: return null,
            // Тема, которой больше нет в игре, читается как «без темы»: квест от этого не теряется.
            topic = properties[KEY_TOPIC]?.let { QuestTopic.fromString(it) },
            cooldownMinutes = properties[KEY_COOLDOWN]?.let { it.toIntOrNull() ?: return null }
                ?: defaults.cooldownMinutes,
            stageDelayMinutes = properties[KEY_STAGE_DELAY]?.let { it.toIntOrNull() ?: return null }
                ?: defaults.stageDelayMinutes
        )
    }

    /** @return Флаг из `1`/`0`; [default], когда его нет; null, когда там что-то другое. */
    private fun flagOf(value: String?, default: Boolean): Boolean? = when (value) {
        null -> default
        "1" -> true
        "0" -> false
        else -> null
    }

    /** Запись прежнего вида: у каждого шага своя задержка, у варианта — только настроение. */
    private fun draftOfV1(fields: List<String>): CustomQuestDraft? {
        var cursor = 0
        fun next(): String? = fields.getOrNull(cursor++)
        fun nextInt(): Int? = next()?.toIntOrNull()

        val title = next() ?: return null
        val description = next() ?: return null
        val minBalance = nextInt() ?: return null
        val stepCount = nextInt()?.takeIf { it in 0..CustomQuests.STEPS_MAX } ?: return null
        val delays = mutableListOf<Int>()
        val steps = List(stepCount) {
            val text = next() ?: return null
            delays += nextInt() ?: return null
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
            CustomQuestStepDraft(text, options)
        }
        if (cursor != fields.size) return null
        return CustomQuestDraft(
            title = title,
            description = description,
            minBalance = minBalance,
            steps = steps,
            // Задержка последнего шага ни на что не влияла: после него квест кончается.
            stageDelayMinutes = delays.dropLast(1).maxOrNull() ?: 0
        )
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
