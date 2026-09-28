package com.legacy.fingame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.quests.CustomQuestDraft
import com.legacy.fingame.game.quests.CustomQuestOptionDraft
import com.legacy.fingame.game.quests.CustomQuestStepDraft
import com.legacy.fingame.game.quests.CustomQuests
import com.legacy.fingame.game.quests.CustomQuestsCodec
import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestEntry
import com.legacy.fingame.game.quests.QuestOutcome
import com.legacy.fingame.game.quests.QuestProgress
import com.legacy.fingame.game.quests.QuestTopic
import com.legacy.fingame.game.stats.StatKind
import com.legacy.fingame.ui.components.GameDialog
import com.legacy.fingame.ui.components.GameDialogBlock
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.PillStyle
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors

/** Иконка своего квеста в списке. */
private val QuestListIconSize = 40.dp

/** Иконка в строке «−/+». */
private val StepperIconSize = 24.dp

/** Кнопки «−/+». */
private val StepperButtonSize = 40.dp

/** Столбец значения между «−» и «+»: ширина не прыгает от «0» к «−100». */
private val StepperValueWidth = 52.dp

/** Черновик формы переживает поворот экрана одной строкой, см. [CustomQuestsCodec.encodeDraft]. */
private val DraftSaver = Saver<CustomQuestDraft, String>(
    save = { CustomQuestsCodec.encodeDraft(it) },
    restore = { CustomQuestsCodec.decodeDraft(it) }
)

/** «1 шаг», «2 шага». */
internal fun stepsText(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> "$count шаг"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "$count шага"
    else -> "$count шагов"
}

/** Страница «Правила» формы квеста — сразу после «Основного». */
private const val RulesPageIndex = 1

/** С этой страницы формы начинаются ситуации. */
private const val FirstStepPage = 2

/** Минут в сутках — для подписи длинных пауз и кулдаунов. */
private const val MinutesPerDay = 24 * 60

/** Столбец значения паузы и кулдауна: «1 ч 30 мин» не прыгает по ширине. */
private val DurationValueWidth = 96.dp

/** Длительность в минутах так, как её читает взрослый: «нет», «5 мин», «1 ч 30 мин», «2 сут». */
internal fun minutesTitle(minutes: Int): String = when {
    minutes <= 0 -> "нет"
    minutes < 60 -> "$minutes мин"
    minutes % MinutesPerDay == 0 -> "${minutes / MinutesPerDay} сут"
    minutes % 60 == 0 -> "${minutes / 60} ч"
    else -> "${minutes / 60} ч ${minutes % 60} мин"
}

/** Текст поля: без переводов строк и служебных знаков, не длиннее [max]. */
private fun typed(text: String, max: Int): String =
    text.filterNot { it == '\n' || it.code < 0x20 }.take(max)

/**
 * Раздел «Свои квесты» над историей выборов: заголовок раздела.
 */
@Composable
internal fun AdultSectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

/**
 * Свой квест в списке: иконка, название, число шагов и минимум; «Изменить» и «Убрать» — строкой
 * ниже, чтобы название на узком экране с крупным шрифтом не рвалось посреди слова.
 */
@Composable
internal fun CustomQuestCard(quest: Quest, onEdit: () -> Unit, onRemove: () -> Unit) {
    AdultCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Sprite(
                assetPath = Sprites.QUESTS,
                contentDescription = null,
                modifier = Modifier.size(QuestListIconSize)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(stepsText(quest.stepCount))
                        if (quest.minBalance > 0) append(" · от ${quest.minBalance} монет")
                        if (!quest.repeatable) append(" · одноразовый")
                        if (quest.requiresAdultCheck) append(" · с проверкой")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillButton(text = "Изменить", onClick = onEdit, style = PillStyle.Tonal, compact = true)
            PillButton(text = "Убрать", onClick = onRemove, style = PillStyle.Outlined, compact = true)
        }
    }
}

/**
 * Этап, сданный ребёнком на проверку: квест, шаг и выбранный вариант, что ребёнок получит, и
 * кнопки «Засчитать» / «Не засчитано».
 */
@Composable
internal fun QuestCheckCard(
    quest: Quest,
    progress: QuestProgress,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val outcome = progress.lastChoice ?: return
    AdultCard {
        Text(
            text = quest.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Шаг ${quest.stepNumberOf(progress.nodeId).coerceAtLeast(1)}: ${outcome.optionLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (outcome.resultText.isNotBlank()) {
            Text(
                text = outcome.resultText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Награда: ${outcomeRewardText(outcome)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(
                text = "Засчитать",
                onClick = onApprove,
                style = PillStyle.Primary,
                compact = true,
                modifier = Modifier.weight(1f)
            )
            PillButton(
                text = "Не засчитано",
                onClick = onReject,
                style = PillStyle.Outlined,
                compact = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** «+20 монет, +10 сытости, +50 %» — что даст засчитанный этап; без изменений — «без изменений». */
internal fun outcomeRewardText(outcome: QuestOutcome): String = optionEffectsText(
    CustomQuestOptionDraft.of(
        label = outcome.optionLabel,
        resultText = outcome.resultText,
        moneyDelta = outcome.moneyDelta,
        effects = outcome.statEffects,
        progressDelta = outcome.progressDelta
    )
)

/** Подтверждение «Убрать квест?». */
@Composable
internal fun RemoveCustomQuestDialog(quest: Quest, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    GameDialog(onDismiss = onDismiss) {
        GameDialogBlock(
            title = "Убрать «${quest.title}»?",
            closeDescription = "Не убирать",
            onDismiss = onDismiss,
            actions = {
                PillButton(
                    text = "Убрать",
                    onClick = onConfirm,
                    style = PillStyle.Primary,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PillButton(
                    text = "Отмена",
                    onClick = onDismiss,
                    style = PillStyle.Text,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) {
            Text(
                text = "Квест пропадёт с экрана квестов. Если ребёнок его проходит, прохождение " +
                    "прервётся; выборы останутся в истории.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Форма «Новый квест» по шагам: сначала название, описание и минимум монет, потом каждая ситуация
 * со своими вариантами, в конце — как квест увидит ребёнок, и «Сохранить». Черновик переживает
 * поворот экрана; всё прокручивается, клавиатура телефона не закрывает поля.
 *
 * @param onSave сохранить; true — сохранено, форма закрывается.
 * @param onCancel закрыть без сохранения.
 * @param existingCount сколько своих квестов уже есть — для предела.
 * @param initialDraft черновик при открытии: свой квест, который взрослый меняет, или пустой.
 * @param initialPage страница при открытии (для превью): 0 — основное, 1 — правила, дальше
 * ситуации, в конце проверка.
 * @param title заголовок формы: «Новый квест» или «Изменить квест».
 * @param notice предупреждение над страницами — например, что ребёнок этот квест сейчас
 * проходит; null — предупреждать не о чем.
 */
@Composable
internal fun CustomQuestForm(
    onSave: (CustomQuestDraft) -> Boolean,
    onCancel: () -> Unit,
    existingCount: Int,
    initialDraft: CustomQuestDraft = CustomQuestDraft(),
    initialPage: Int = 0,
    title: String = "Новый квест",
    notice: String? = null
) {
    var draft by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf(initialDraft) }
    var pageState by rememberSaveable { mutableIntStateOf(initialPage) }
    // Страницы, с которых взрослый уже уходил «Дальше»: на них видно, чего не хватает.
    var checkedPages by rememberSaveable { mutableIntStateOf(0) }
    var minText by rememberSaveable {
        mutableStateOf(if (initialDraft.minBalance > 0) initialDraft.minBalance.toString() else "")
    }
    val focusManager = LocalFocusManager.current
    val scroll = rememberScrollState()

    val stepCount = draft.steps.size
    val previewPage = stepCount + FirstStepPage
    val page = pageState.coerceIn(0, previewPage)
    val stepIndex = page - FirstStepPage

    fun goTo(target: Int) {
        focusManager.clearFocus()
        checkedPages = checkedPages or (1 shl page)
        pageState = target
    }

    fun updateStep(index: Int, change: (CustomQuestStepDraft) -> CustomQuestStepDraft) {
        draft = draft.copy(steps = draft.steps.mapIndexed { i, step -> if (i == index) change(step) else step })
    }

    val pageTitle = when (page) {
        0 -> "Основное"
        RulesPageIndex -> "Правила"
        previewPage -> "Проверка"
        else -> "Ситуация ${stepIndex + 1} из $stepCount"
    }
    val pageErrors = when (page) {
        0 -> draft.headerErrors()
        RulesPageIndex -> draft.rulesErrors()
        previewPage -> draft.validate(existingCount)
        else -> draft.stepErrors(stepIndex)
    }
    val showErrors = page == previewPage || checkedPages and (1 shl page) != 0

    // Новая страница открывается сверху.
    LaunchedEffect(page) { scroll.scrollTo(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scroll)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // На узком экране с крупным шрифтом заголовок ужимается целиком, а не переносится.
            ShrinkText(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                minSize = 12.dp
            )
            PillButton(text = "Отмена", onClick = onCancel, style = PillStyle.Text, compact = true)
        }
        Text(
            text = "Страница ${page + 1} из ${previewPage + 1} · $pageTitle",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (notice != null) {
            Text(
                text = notice,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        when (page) {
            0 -> HeaderPage(
                draft = draft,
                minText = minText,
                onDraftChange = { draft = it },
                onMinTextChange = { text ->
                    minText = text
                    draft = draft.copy(minBalance = text.toIntOrNull() ?: 0)
                }
            )

            RulesPageIndex -> RulesPage(draft = draft, onDraftChange = { draft = it })

            previewPage -> PreviewPage(draft)

            else -> StepPage(
                index = stepIndex,
                step = draft.steps[stepIndex],
                onChange = { change -> updateStep(stepIndex, change) }
            )
        }

        if (page in FirstStepPage until previewPage) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (stepCount < CustomQuests.STEPS_MAX) {
                    PillButton(
                        text = "Добавить шаг",
                        onClick = {
                            draft = draft.copy(steps = draft.steps + CustomQuestStepDraft())
                            goTo(stepCount + FirstStepPage)
                        },
                        style = PillStyle.Outlined,
                        compact = true,
                        autoShrink = false
                    )
                }
                if (stepCount > CustomQuests.STEPS_MIN) {
                    PillButton(
                        text = "Удалить шаг",
                        onClick = {
                            focusManager.clearFocus()
                            draft = draft.copy(steps = draft.steps.filterIndexed { i, _ -> i != stepIndex })
                            // Отметки проверенных страниц после удалённой сдвигаются вместе с ними.
                            checkedPages = checkedPages and ((1 shl page) - 1)
                            pageState = page.coerceAtMost(stepCount - 1 + FirstStepPage - 1)
                                .coerceAtLeast(FirstStepPage)
                        },
                        style = PillStyle.Text,
                        compact = true,
                        autoShrink = false
                    )
                }
            }
        }

        if (showErrors && pageErrors.isNotEmpty()) {
            Text(
                text = pageErrors.joinToString("\n"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (page > 0) {
                PillButton(
                    text = "Назад",
                    onClick = { goTo(page - 1) },
                    style = PillStyle.Outlined,
                    modifier = Modifier.weight(1f)
                )
            }
            if (page < previewPage) {
                PillButton(
                    text = "Дальше",
                    onClick = { goTo(page + 1) },
                    style = PillStyle.Primary,
                    modifier = Modifier.weight(1f)
                )
            } else {
                PillButton(
                    text = "Сохранить",
                    onClick = {
                        focusManager.clearFocus()
                        onSave(draft)
                    },
                    style = PillStyle.Primary,
                    enabled = pageErrors.isEmpty(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Страница «Основное»: название, описание, минимум монет с цифровой клавиатуры. */
@Composable
private fun HeaderPage(
    draft: CustomQuestDraft,
    minText: String,
    onDraftChange: (CustomQuestDraft) -> Unit,
    onMinTextChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    FormLabel("Название")
    FormTextField(
        value = draft.title,
        onValueChange = { onDraftChange(draft.copy(title = typed(it, CustomQuests.TITLE_MAX))) },
        max = CustomQuests.TITLE_MAX,
        placeholder = "Например, Уборка",
        singleLine = true,
        onDone = { focusManager.clearFocus() }
    )
    FormLabel("Описание")
    FormTextField(
        value = draft.description,
        onValueChange = {
            onDraftChange(draft.copy(description = typed(it, CustomQuests.DESCRIPTION_MAX)))
        },
        max = CustomQuests.DESCRIPTION_MAX,
        placeholder = "Что за квест и чему он учит",
        singleLine = false,
        onDone = { focusManager.clearFocus() }
    )
    FormLabel("Сколько монет нужно на счёте, чтобы взять (0–${CustomQuests.MIN_BALANCE_MAX})")
    PriceBox(minText.ifEmpty { "0" })
    Keypad(
        onDigit = { digit ->
            if (minText.length < CustomQuests.MIN_BALANCE_MAX.toString().length) {
                onMinTextChange((minText + digit).trimStart('0'))
            }
        },
        onErase = { onMinTextChange(minText.dropLast(1)) }
    )
}

/**
 * Страница «Правила»: одноразовый квест или многоразовый, кулдаун многоразового и пауза между
 * этапами. Числа меняются кнопками «−/+» по ряду удобных значений — без клавиатуры.
 */
@Composable
private fun RulesPage(draft: CustomQuestDraft, onDraftChange: (CustomQuestDraft) -> Unit) {
    FormLabel("Сколько раз можно пройти")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PillButton(
            text = "Многоразовый",
            onClick = { onDraftChange(draft.copy(repeatable = true)) },
            selected = draft.repeatable,
            compact = true,
            modifier = Modifier.weight(1f)
        )
        PillButton(
            text = "Одноразовый",
            onClick = { onDraftChange(draft.copy(repeatable = false)) },
            selected = !draft.repeatable,
            compact = true,
            modifier = Modifier.weight(1f)
        )
    }
    FormHint(
        if (draft.repeatable) {
            "Пройденный квест можно взять снова, когда закончится кулдаун."
        } else {
            "Пройденный квест закроется. Открыть его ещё раз можно здесь, во вкладке «Квесты», " +
                "кнопкой «Включить снова»."
        }
    )
    if (draft.repeatable) {
        DurationRow(
            title = "Кулдаун, мин",
            minutes = draft.cooldownMinutes,
            presets = CustomQuests.COOLDOWNS,
            max = CustomQuests.COOLDOWN_MAX,
            onChange = { onDraftChange(draft.copy(cooldownMinutes = it)) }
        )
        FormHint("Столько ждать после прохождения, прежде чем взять квест снова.")
    }
    DurationRow(
        title = "Пауза между этапами, мин",
        minutes = draft.stageDelayMinutes,
        presets = CustomQuests.STAGE_DELAYS,
        max = CustomQuests.STAGE_DELAY_MAX,
        onChange = { onDraftChange(draft.copy(stageDelayMinutes = it)) }
    )
    FormHint("Через столько после выбора откроется следующая ситуация.")
    FormLabel("Проверка взрослым")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PillButton(
            text = "Сам",
            onClick = { onDraftChange(draft.copy(requiresAdultCheck = false)) },
            selected = !draft.requiresAdultCheck,
            compact = true,
            modifier = Modifier.weight(1f)
        )
        PillButton(
            text = "Проверяю я",
            onClick = { onDraftChange(draft.copy(requiresAdultCheck = true)) },
            selected = draft.requiresAdultCheck,
            compact = true,
            modifier = Modifier.weight(1f)
        )
    }
    FormHint(
        if (draft.requiresAdultCheck) {
            "Выбор ребёнка ждёт вашей проверки во вкладке «Квесты». Награда — только после " +
                "«Засчитать», а «Не засчитано» вернёт этап в работу."
        } else {
            "Этап засчитывается сразу, как ребёнок выберет вариант."
        }
    )
    FormLabel("Тема")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PillButton(
            text = "Без темы",
            onClick = { onDraftChange(draft.copy(topic = null)) },
            selected = draft.topic == null,
            compact = true,
            autoShrink = false
        )
        QuestTopic.entries.forEach { topic ->
            PillButton(
                text = topic.title,
                onClick = { onDraftChange(draft.copy(topic = topic)) },
                selected = draft.topic == topic,
                compact = true,
                autoShrink = false
            )
        }
    }
    FormHint("Пройденный квест засчитает тему в разделе «Прогресс».")
}

/** Пояснение под полем формы — мелко и тихо. */
@Composable
private fun FormHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Строка «название, −, длительность, +»: шаги — по ряду [presets], не дальше [max]. */
@Composable
private fun DurationRow(title: String, minutes: Int, presets: List<Int>, max: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ShrinkText(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            minSize = 10.dp
        )
        SpriteButton(
            assetPath = Sprites.MINUS,
            contentDescription = "$title меньше",
            onClick = { onChange(CustomQuests.previousPreset(presets, minutes).coerceAtLeast(0)) },
            size = StepperButtonSize,
            enabled = minutes > 0,
            showIndicator = false
        )
        Text(
            text = minutesTitle(minutes),
            modifier = Modifier.widthIn(min = DurationValueWidth),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
        SpriteButton(
            assetPath = Sprites.PLUS,
            contentDescription = "$title больше",
            onClick = { onChange(CustomQuests.nextPreset(presets, minutes).coerceAtMost(max)) },
            size = StepperButtonSize,
            enabled = minutes < max && CustomQuests.nextPreset(presets, minutes) != minutes,
            showIndicator = false
        )
    }
}

/** Страница одной ситуации: текст и варианты. */
@Composable
private fun StepPage(
    index: Int,
    step: CustomQuestStepDraft,
    onChange: ((CustomQuestStepDraft) -> CustomQuestStepDraft) -> Unit
) {
    val focusManager = LocalFocusManager.current
    FormLabel("Ситуация")
    FormTextField(
        value = step.text,
        onValueChange = { text -> onChange { it.copy(text = typed(text, CustomQuests.STEP_TEXT_MAX)) } },
        max = CustomQuests.STEP_TEXT_MAX,
        placeholder = if (index == 0) "Например, Комната в беспорядке" else "Что случилось дальше",
        singleLine = false,
        onDone = { focusManager.clearFocus() }
    )
    FormLabel("Варианты (${CustomQuests.OPTIONS_MIN}–${CustomQuests.OPTIONS_MAX})")
    step.options.forEachIndexed { optionIndex, option ->
        OptionEditor(
            number = optionIndex + 1,
            option = option,
            canRemove = step.options.size > CustomQuests.OPTIONS_MIN,
            onChange = { changed ->
                onChange { current ->
                    current.copy(
                        options = current.options.mapIndexed { i, o -> if (i == optionIndex) changed else o }
                    )
                }
            },
            onRemove = {
                focusManager.clearFocus()
                onChange { current ->
                    current.copy(options = current.options.filterIndexed { i, _ -> i != optionIndex })
                }
            }
        )
    }
    if (step.options.size < CustomQuests.OPTIONS_MAX) {
        PillButton(
            text = "Добавить вариант",
            onClick = { onChange { it.copy(options = it.options + CustomQuestOptionDraft()) } },
            style = PillStyle.Tonal,
            compact = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Карточка одного варианта: кнопка, результат, монеты, сытость, здоровье, настроение, прогресс. */
@Composable
private fun OptionEditor(
    number: Int,
    option: CustomQuestOptionDraft,
    canRemove: Boolean,
    onChange: (CustomQuestOptionDraft) -> Unit,
    onRemove: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    AdultCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Вариант $number",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (canRemove) {
                PillButton(
                    text = "Удалить вариант",
                    onClick = onRemove,
                    style = PillStyle.Text,
                    compact = true,
                    autoShrink = false
                )
            }
        }
        FormTextField(
            value = option.label,
            onValueChange = { onChange(option.copy(label = typed(it, CustomQuests.LABEL_MAX))) },
            max = CustomQuests.LABEL_MAX,
            placeholder = "Текст кнопки",
            singleLine = true,
            onDone = { focusManager.clearFocus() }
        )
        FormTextField(
            value = option.resultText,
            onValueChange = { onChange(option.copy(resultText = typed(it, CustomQuests.RESULT_MAX))) },
            max = CustomQuests.RESULT_MAX,
            placeholder = "Что вышло после выбора",
            singleLine = false,
            onDone = { focusManager.clearFocus() }
        )
        StepperRow(
            iconPath = Sprites.COIN,
            title = "Монеты",
            value = option.moneyDelta,
            step = CustomQuests.MONEY_STEP,
            min = CustomQuests.MONEY_MIN,
            max = CustomQuests.MONEY_MAX,
            onChange = { onChange(option.copy(moneyDelta = it)) }
        )
        CustomQuests.STATS.forEach { stat ->
            StepperRow(
                iconPath = Sprites.stat(stat.xmlName),
                title = CustomQuests.statTitle(stat),
                value = option.deltaOf(stat),
                step = CustomQuests.STAT_STEP,
                min = CustomQuests.STAT_MIN,
                max = CustomQuests.STAT_MAX,
                onChange = { onChange(option.withDelta(stat, it)) }
            )
        }
        Text(
            text = "Прогресс квеста",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CustomQuests.PROGRESS_STEPS.forEach { percent ->
                PillButton(
                    text = if (percent == 0) "0" else "+$percent",
                    onClick = { onChange(option.copy(progressDelta = percent)) },
                    selected = option.progressDelta == percent,
                    compact = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Строка «иконка, название, −, значение, +». */
@Composable
private fun StepperRow(
    iconPath: String,
    title: String,
    value: Int,
    step: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Sprite(assetPath = iconPath, contentDescription = null, modifier = Modifier.size(StepperIconSize))
        ShrinkText(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            minSize = 10.dp
        )
        SpriteButton(
            assetPath = Sprites.MINUS,
            contentDescription = "$title меньше",
            onClick = { onChange((value - step).coerceAtLeast(min)) },
            size = StepperButtonSize,
            enabled = value > min,
            showIndicator = false
        )
        Text(
            text = signed(value),
            modifier = Modifier.widthIn(min = StepperValueWidth),
            style = MaterialTheme.typography.titleMedium,
            color = when {
                value > 0 -> GameColors.success
                value < 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
        SpriteButton(
            assetPath = Sprites.PLUS,
            contentDescription = "$title больше",
            onClick = { onChange((value + step).coerceAtMost(max)) },
            size = StepperButtonSize,
            enabled = value < max,
            showIndicator = false
        )
    }
}

/** Текстовое поле формы: системная клавиатура, счётчик букв. */
@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    max: Int,
    placeholder: String,
    singleLine: Boolean,
    onDone: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        supportingText = { Text("${value.length}/$max") },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 4,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() })
    )
}

/** Проверка: карточка, как её увидит ребёнок, и все ситуации с вариантами. */
@Composable
private fun PreviewPage(draft: CustomQuestDraft) {
    val quest = draft.toQuest("preview")
    FormLabel("Так квест увидит ребёнок")
    QuestCard(
        entry = QuestEntry(quest, null),
        expanded = false,
        balance = quest.minBalance,
        canRestart = false,
        nowMillis = 0L,
        onToggle = {},
        onStart = {},
        onChoose = {},
        onAdvance = {},
        onRestart = {}
    )
    FormLabel("Что внутри")
    AdultCard {
        if (quest.description.isNotEmpty()) {
            Text(
                text = quest.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = questRulesText(draft),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        draft.steps.forEachIndexed { index, step ->
            CardDivider()
            Text(
                text = "Шаг ${index + 1}. ${step.text.trim().ifEmpty { "…" }}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (index < draft.steps.lastIndex && draft.stageDelayMinutes > 0) {
                Text(
                    text = "Следующий шаг — через ${minutesTitle(draft.stageDelayMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            step.options.forEach { option ->
                Text(
                    text = "• ${option.label.trim().ifEmpty { "…" }} — ${optionEffectsText(option)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (option.resultText.isNotBlank()) {
                    Text(
                        text = option.resultText.trim(),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** «Многоразовый, кулдаун 1 ч» или «Одноразовый»; с паузой — «, пауза между этапами 5 мин». */
internal fun questRulesText(draft: CustomQuestDraft): String = buildString {
    append(if (draft.repeatable) "Многоразовый, кулдаун ${minutesTitle(draft.cooldownMinutes)}" else "Одноразовый")
    if (draft.steps.size > 1 && draft.stageDelayMinutes > 0) {
        append(", пауза между этапами ${minutesTitle(draft.stageDelayMinutes)}")
    }
    if (draft.requiresAdultCheck) append(", проверяет взрослый")
}

/**
 * «+20 монет, +10 сытости, −5 настроения, +50 %», а без изменений — «без изменений». Число и слово
 * соединены неразрывным пробелом: «%» не уезжает на новую строку один.
 */
internal fun optionEffectsText(option: CustomQuestOptionDraft): String = listOfNotNull(
    option.moneyDelta.takeIf { it != 0 }?.let { "${signed(it)}\u00A0монет" },
    option.hungerDelta.takeIf { it != 0 }?.let { "${signed(it)}\u00A0сытости" },
    option.healthDelta.takeIf { it != 0 }?.let { "${signed(it)}\u00A0здоровья" },
    option.moodDelta.takeIf { it != 0 }?.let { "${signed(it)}\u00A0настроения" },
    option.progressDelta.takeIf { it != 0 }?.let { "+$it\u00A0%" }
).joinToString(", ").ifEmpty { "без изменений" }

// --- Превью ---

internal val PreviewCleaningDraft = CustomQuestDraft(
    title = "Уборка",
    description = "Помоги дома и получи награду",
    minBalance = 0,
    steps = listOf(
        CustomQuestStepDraft(
            text = "Комната в беспорядке",
            options = listOf(
                CustomQuestOptionDraft("Убрать сейчас", "Стало чисто!", progressDelta = 25, healthDelta = 5),
                CustomQuestOptionDraft("Потом", "Беспорядок остался")
            )
        ),
        CustomQuestStepDraft(
            text = "Мама предлагает 20 монет за помощь",
            options = listOf(
                CustomQuestOptionDraft("Помочь", "Мама довольна", moneyDelta = 20, progressDelta = 50),
                CustomQuestOptionDraft("Отказаться", "Грустно", moodDelta = -5)
            )
        )
    ),
    stageDelayMinutes = 5
)

@Composable
private fun FormPreviewSurface(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Preview(name = "Quest form — header", showBackground = true, widthDp = 411, heightDp = 1100)
@Composable
private fun CustomQuestFormHeaderPreview() {
    FinGameTheme(darkTheme = false) {
        FormPreviewSurface { CustomQuestForm({ true }, {}, 0, PreviewCleaningDraft) }
    }
}

@Preview(name = "Quest form — step, dark", showBackground = true, widthDp = 411, heightDp = 1400)
@Composable
private fun CustomQuestFormStepPreview() {
    FinGameTheme(darkTheme = true) {
        FormPreviewSurface { CustomQuestForm({ true }, {}, 0, PreviewCleaningDraft, initialPage = 3) }
    }
}

@Preview(
    name = "Quest form — step, 360dp font 1.3",
    showBackground = true,
    widthDp = 360,
    heightDp = 1800,
    fontScale = 1.3f
)
@Composable
private fun CustomQuestFormNarrowPreview() {
    FinGameTheme(darkTheme = false) {
        FormPreviewSurface { CustomQuestForm({ true }, {}, 0, PreviewCleaningDraft, initialPage = 2) }
    }
}

@Preview(name = "Quest form — check", showBackground = true, widthDp = 411, heightDp = 1100)
@Composable
private fun CustomQuestFormCheckPreview() {
    FinGameTheme(darkTheme = false) {
        FormPreviewSurface { CustomQuestForm({ true }, {}, 0, PreviewCleaningDraft, initialPage = 4) }
    }
}

@Preview(name = "Quest form — rules, 360dp font 1.3", showBackground = true, widthDp = 360, heightDp = 900, fontScale = 1.3f)
@Composable
private fun CustomQuestFormRulesPreview() {
    FinGameTheme(darkTheme = false) {
        FormPreviewSurface { CustomQuestForm({ true }, {}, 0, PreviewCleaningDraft, initialPage = 1) }
    }
}

@Preview(name = "Quest form — edit an active quest", showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun CustomQuestFormEditPreview() {
    FinGameTheme(darkTheme = true) {
        FormPreviewSurface {
            CustomQuestForm(
                onSave = { true },
                onCancel = {},
                existingCount = 0,
                initialDraft = PreviewCleaningDraft,
                title = "Изменить квест",
                notice = "Ребёнок сейчас проходит этот квест. После сохранения прохождение начнётся " +
                    "заново: прогресс сбросится, полученные монеты останутся."
            )
        }
    }
}

@Preview(name = "Quest form — landscape", showBackground = true, widthDp = 891, heightDp = 411)
@Composable
private fun CustomQuestFormLandscapePreview() {
    FinGameTheme(darkTheme = true) {
        FormPreviewSurface { CustomQuestForm({ true }, {}, 0, PreviewCleaningDraft, initialPage = 2) }
    }
}
