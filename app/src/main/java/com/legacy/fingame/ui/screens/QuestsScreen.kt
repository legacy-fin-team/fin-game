package com.legacy.fingame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestEngine
import com.legacy.fingame.game.quests.QuestEntry
import com.legacy.fingame.game.quests.QuestKind
import com.legacy.fingame.game.quests.QuestNode
import com.legacy.fingame.game.quests.QuestOption
import com.legacy.fingame.game.quests.QuestOutcome
import com.legacy.fingame.game.quests.QuestProgress
import com.legacy.fingame.game.quests.QuestStatus
import com.legacy.fingame.game.stats.StatKind
import com.legacy.fingame.ui.components.BalanceChip
import com.legacy.fingame.ui.components.EffectChip
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.PillStyle
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens
import com.legacy.fingame.utils.SpriteLoader
import kotlinx.coroutines.delay

/** Поля экрана и шаг сетки — те же, что у журнала. */
private val ScreenPadding = 16.dp
private val HeaderGap = 8.dp
private val ListGap = 12.dp

/** Размер крестика: такой же, как на остальных экранах. */
private val CloseButtonSize = 40.dp

/** Внутренний отступ карточки и зазор между её частями. */
private val CardPadding = 16.dp
private val CardGap = 12.dp

/** Иконка квеста в заголовке карточки на телефоне; на планшете растёт через [GameDimens.buttonSize]. */
private val QuestIconSize = 48.dp

/** Картинка ситуации не выше этого, чтобы кнопки вариантов оставались на экране. */
private val NodeImageMaxHeight = 160.dp

/** Зазоры между чипами эффектов и между кнопками вариантов. */
private val ChipGap = 8.dp
private val OptionGap = 8.dp

/** Размер монеты в чипе денег — как иконка стата в [EffectChip]. */
private val CoinIconSize = 24.dp

/** Толщина полоски прогресса — как у карточки цели. */
private val ProgressBarHeight = 8.dp

/** Как часто экран перечитывает игровые часы для отсчёта. */
private const val CountdownStepMillis = 1_000L

/**
 * Экран квестов: каждая карточка — один квест, тап раскрывает её на месте, раскрыта одна за раз.
 *
 * Шапка — два яруса, как у журнала: сверху счёт и крестик, под ними заголовок. Раскрытая карточка
 * показывает описание, полоску прогресса (если он у квеста есть) и ниже — либо текущую ситуацию с
 * кнопками вариантов столбиком, либо результат выбора с изменениями и отсчётом до следующего шага
 * или кнопкой «Дальше»/«Завершить». Результат и изменения видны только после выбора.
 *
 * Раскрытая карточка переживает поворот экрана ([rememberSaveable]). Отсчёт обновляется раз в
 * секунду по игровым часам ([currentMillis]), так что перемотка демо-сборки его ускоряет; когда ни
 * один квест не ждёт шага, часы не перечитываются.
 *
 * @param entries карточки по порядку, см. [com.legacy.fingame.game.quests.QuestBoard.entriesOf].
 * @param currentMillis момент по игровым часам (в приложении — `GameViewModel::nowMillis`).
 * @param onStart «Взять» квест с этим id.
 * @param onChoose выбор варианта: id квеста и номер варианта.
 * @param onAdvance «Дальше» или «Завершить» для квеста с этим id.
 * @param onRestart «Ещё раз» для пройденного квеста игрока.
 * @param onClose закрыть экран.
 * @param modifier модификатор корня экрана.
 * @param balance текущий счёт — в шапке и для проверки минимума.
 * @param depositAmount тело вклада — в шапке.
 * @param initiallyExpanded карточка, раскрытая при первом показе; для превью.
 * @param canRestart показывать ли «Ещё раз» у пройденного квеста игрока — только в демо-сборке.
 * @param ignoreDelays ожидание в квестах снято (отладочная сборка, см.
 * `GameViewModel.ignoreQuestDelays`): ни «Доступен через …», ни «Следующий шаг через …».
 */
@Composable
fun QuestsScreen(
    entries: List<QuestEntry>,
    currentMillis: () -> Long,
    onStart: (questId: String) -> Unit,
    onChoose: (questId: String, optionIndex: Int) -> Unit,
    onAdvance: (questId: String) -> Unit,
    onRestart: (questId: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    balance: Int = 0,
    depositAmount: Int = 0,
    initiallyExpanded: String? = null,
    canRestart: Boolean = false,
    ignoreDelays: Boolean = false
) {
    var expandedId by rememberSaveable { mutableStateOf(initiallyExpanded) }
    // Момент берётся прямо из часов, когда меняются карточки (выбор, «Дальше») или приходит тик, —
    // так выбор без ожидания не покажет отсчёт от давно прочитанного времени. Тик идёт раз в
    // секунду, только пока какой-то квест ждёт шага; дождались — ключ меняется и тик стихает.
    var tick by remember { mutableIntStateOf(0) }
    val nowMillis = remember(tick, entries) { currentMillis() }
    val hasWaiting = hasWaitingStep(entries, nowMillis, ignoreDelays)
    LaunchedEffect(hasWaiting) {
        while (hasWaiting) {
            delay(CountdownStepMillis)
            tick++
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = GameDimens.ContentMaxWidth).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HeaderGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BalanceChip(balance = balance, depositAmount = depositAmount)
                Spacer(modifier = Modifier.weight(1f))
                SpriteButton(
                    assetPath = Sprites.CLOSE,
                    contentDescription = "Закрыть квесты",
                    onClick = onClose,
                    size = CloseButtonSize,
                    showIndicator = false
                )
            }

            Spacer(modifier = Modifier.height(HeaderGap))

            Text(
                text = "Квесты",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(ListGap))
        }

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Пока квестов нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.widthIn(max = GameDimens.ContentMaxWidth).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(ListGap),
                // Тень карточки не срезается краем списка.
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
            ) {
                items(items = entries, key = { it.quest.id }) { entry ->
                    val questId = entry.quest.id
                    QuestCard(
                        entry = entry,
                        expanded = expandedId == questId,
                        balance = balance,
                        canRestart = canRestart,
                        nowMillis = nowMillis,
                        ignoreDelays = ignoreDelays,
                        onToggle = { expandedId = nextExpandedQuest(expandedId, questId) },
                        onStart = { onStart(questId) },
                        onChoose = { index -> onChoose(questId, index) },
                        onAdvance = { onAdvance(questId) },
                        onRestart = { onRestart(questId) }
                    )
                }
            }
        }
    }
}

/**
 * Карточка одного квеста: заголовок (иконка, название, статус) всегда, остальное — когда раскрыта.
 * Тап по заголовку раскрывает и сворачивает карточку.
 */
@Composable
private fun QuestCard(
    entry: QuestEntry,
    expanded: Boolean,
    balance: Int,
    canRestart: Boolean,
    nowMillis: Long,
    ignoreDelays: Boolean,
    onToggle: () -> Unit,
    onStart: () -> Unit,
    onChoose: (Int) -> Unit,
    onAdvance: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quest = entry.quest
    val progress = entry.progress

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        // Раскрытая карточка отличается цветом рамки, а не толщиной — как надетое в инвентаре.
        border = BorderStroke(
            width = 1.dp,
            color = if (expanded) MaterialTheme.colorScheme.primary else GameColors.cardStroke
        ),
        shadowElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = if (expanded) "Свернуть" else "Раскрыть",
                        onClick = onToggle
                    )
                    .padding(CardPadding),
                horizontalArrangement = Arrangement.spacedBy(CardGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuestImage(
                    imagePath = quest.imagePath,
                    modifier = Modifier.size(GameDimens.buttonSize(QuestIconSize))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    // У раскрытой карточки отсчёт и «завершён» уже внизу — шапка их не повторяет.
                    val status = if (expanded) {
                        expandedQuestStatusText(entry, balance, nowMillis, ignoreDelays)
                    } else {
                        questStatusText(entry, balance, nowMillis, ignoreDelays)
                    }
                    if (status != null) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (expanded) {
                HorizontalDivider(color = GameColors.divider)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(CardPadding),
                    verticalArrangement = Arrangement.spacedBy(CardGap)
                ) {
                    if (quest.description.isNotEmpty()) {
                        Text(
                            text = quest.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (quest.hasProgress && progress != null) {
                        QuestProgressBar(percent = progress.progress)
                    }
                    QuestBody(
                        quest = quest,
                        progress = progress,
                        balance = balance,
                        canRestart = canRestart,
                        nowMillis = nowMillis,
                        ignoreDelays = ignoreDelays,
                        onStart = onStart,
                        onChoose = onChoose,
                        onAdvance = onAdvance,
                        onRestart = onRestart
                    )
                }
            }
        }
    }
}

/** Нижняя часть раскрытой карточки — в зависимости от того, где игрок в квесте. */
@Composable
private fun QuestBody(
    quest: Quest,
    progress: QuestProgress?,
    balance: Int,
    canRestart: Boolean,
    nowMillis: Long,
    ignoreDelays: Boolean,
    onStart: () -> Unit,
    onChoose: (Int) -> Unit,
    onAdvance: () -> Unit,
    onRestart: () -> Unit
) {
    val choice = progress?.lastChoice
    when {
        progress == null -> StartBlock(quest = quest, balance = balance, onStart = onStart)
        progress.isFinished -> FinishedBlock(
            quest = quest,
            progress = progress,
            balance = balance,
            canRestart = canRestart,
            nowMillis = nowMillis,
            ignoreDelays = ignoreDelays,
            onRestart = onRestart
        )
        choice != null -> OutcomeBlock(
            quest = quest,
            outcome = choice,
            availableAtMillis = QuestEngine.stepAvailableAtMillis(progress, nowMillis, ignoreDelays),
            nowMillis = nowMillis,
            onAdvance = onAdvance
        )
        else -> quest.node(progress.nodeId)?.let { node ->
            NodeBlock(node = node, balance = balance, onChoose = onChoose)
        }
    }
}

/** Квест ещё не взят: кнопка «Взять» и пояснение про минимум. */
@Composable
private fun StartBlock(quest: Quest, balance: Int, onStart: () -> Unit) {
    val affordable = balance >= quest.minBalance
    Column(verticalArrangement = Arrangement.spacedBy(CardGap)) {
        PillButton(
            text = "Взять",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            style = PillStyle.Primary,
            enabled = affordable,
            // compact — иначе PillButton сжимается по надписи и fillMaxWidth не растягивает его.
            compact = true
        )
        if (quest.minBalance > 0) {
            MinBalanceNote(amount = quest.minBalance, affordable = affordable)
        }
    }
}

/**
 * Квест пройден: надпись и, для квеста игрока в демо-сборке, «Ещё раз» — неактивная, пока квест
 * ещё нельзя начать снова (кулдаун, одноразовый без включения взрослым или не хватает монет; см.
 * [QuestEngine.availabilityOf]).
 */
@Composable
private fun FinishedBlock(
    quest: Quest,
    progress: QuestProgress,
    balance: Int,
    canRestart: Boolean,
    nowMillis: Long,
    ignoreDelays: Boolean,
    onRestart: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(CardGap)) {
        Text(
            text = "Квест завершён",
            style = MaterialTheme.typography.bodyMedium,
            color = GameColors.success
        )
        if (canRestart && quest.kind == QuestKind.PLAYER) {
            val affordable = balance >= quest.minBalance
            val availability = QuestEngine.availabilityOf(
                quest,
                listOf(progress),
                balance,
                nowMillis,
                ignoreDelays
            )
            PillButton(
                text = "Ещё раз",
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth(),
                style = PillStyle.Outlined,
                enabled = availability.canStart,
                compact = true
            )
            if (quest.minBalance > 0) {
                MinBalanceNote(amount = quest.minBalance, affordable = affordable)
            }
        }
    }
}

/** Пояснение: минимум нужен в запасе и не тратится; красным, пока его нет. */
@Composable
private fun MinBalanceNote(amount: Int, affordable: Boolean) {
    Text(
        text = minBalanceNoteText(amount),
        style = MaterialTheme.typography.bodySmall,
        color = if (affordable) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        }
    )
}

/**
 * Текущая ситуация: картинка (если есть файл), текст и кнопки вариантов столбиком на всю ширину —
 * длинная надпись не сжимает кнопку в точку. Все варианты одного вида, ни один не выделен.
 * Вариант, на который не хватает монет, неактивен, а под ним написано, сколько нужно.
 */
@Composable
private fun NodeBlock(node: QuestNode, balance: Int, onChoose: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CardGap)) {
        NodeImage(imagePath = node.imagePath)
        Text(
            text = node.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(OptionGap)
        ) {
            node.options.forEachIndexed { index, option ->
                val lockText = optionLockText(option, balance)
                PillButton(
                    text = option.label,
                    onClick = { onChoose(index) },
                    modifier = Modifier.fillMaxWidth(),
                    // Все варианты одного вида: выделенный первый подсказывал бы «правильный» ответ.
                    style = PillStyle.Tonal,
                    enabled = lockText == null,
                    compact = true
                )
                if (lockText != null) {
                    Text(
                        text = lockText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Результат выбора: что выбрано, что случилось, изменения полосок и денег, изменение прогресса и
 * либо отсчёт до следующего шага, либо кнопка «Дальше»/«Завершить».
 */
@Composable
private fun OutcomeBlock(
    quest: Quest,
    outcome: QuestOutcome,
    availableAtMillis: Long,
    nowMillis: Long,
    onAdvance: () -> Unit
) {
    val effects = StatKind.entries.mapNotNull { stat ->
        outcome.statEffects[stat]?.takeIf { it != 0 }?.let { value -> stat to value }
    }

    Column(verticalArrangement = Arrangement.spacedBy(CardGap)) {
        Text(
            text = outcome.optionLabel,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = outcome.resultText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (effects.isNotEmpty() || outcome.moneyDelta != 0) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ChipGap),
                verticalArrangement = Arrangement.spacedBy(ChipGap)
            ) {
                effects.forEach { (stat, value) -> EffectChip(stat = stat, value = value) }
                if (outcome.moneyDelta != 0) {
                    CoinDeltaChip(delta = outcome.moneyDelta)
                }
            }
        }
        if (quest.hasProgress && outcome.progressDelta != 0) {
            Text(
                text = progressChangeText(outcome.progressDelta),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (nowMillis < availableAtMillis) {
            Text(
                text = nextStepInText(availableAtMillis - nowMillis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            PillButton(
                text = advanceButtonText(outcome),
                onClick = onAdvance,
                modifier = Modifier.fillMaxWidth(),
                style = PillStyle.Primary,
                compact = true
            )
        }
    }
}

/** Изменение денег в том же виде, что и [EffectChip]: монета и сумма со знаком. */
@Composable
private fun CoinDeltaChip(delta: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Sprite(
                assetPath = Sprites.COIN,
                contentDescription = "Монеты",
                modifier = Modifier.size(CoinIconSize)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = signedAmountText(delta),
                style = MaterialTheme.typography.labelLarge,
                color = if (delta < 0) MaterialTheme.colorScheme.error else GameColors.success,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/** Полоска прогресса квеста в стиле карточки цели: без круглого хвоста, с процентами справа. */
@Composable
private fun QuestProgressBar(percent: Int, modifier: Modifier = Modifier) {
    val fraction = (percent.coerceIn(Quest.MIN_PROGRESS, Quest.MAX_PROGRESS)).toFloat() /
        Quest.MAX_PROGRESS
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .weight(1f)
                .height(ProgressBarHeight),
            color = GameColors.goalProgress,
            trackColor = GameColors.goalProgressTrack,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Картинка квеста, а без файла — иконка квестов (см. [questImageOf]). */
@Composable
private fun QuestImage(imagePath: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val loader = remember(context) { SpriteLoader(context) }
    val shown = remember(imagePath, loader) { questImageOf(imagePath) { loader.hasSprite(it) } }
    Sprite(assetPath = shown, contentDescription = null, modifier = modifier)
}

/** Картинка ситуации — только если файл есть; иначе ничего, а не заглушка. */
@Composable
private fun NodeImage(imagePath: String?) {
    if (imagePath == null) return
    val context = LocalContext.current
    val loader = remember(context) { SpriteLoader(context) }
    val exists = remember(imagePath, loader) { loader.hasSprite(imagePath) }
    if (!exists) return
    Sprite(
        assetPath = imagePath,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = NodeImageMaxHeight)
    )
}

/** Момент, от которого считаются превью. */
private const val PreviewNow = 1_700_000_000_000L

private val PreviewPicnic = Quest(
    id = "picnic",
    title = "Пикник",
    description = "Позови друзей на пикник. Чем лучше подготовка, тем веселее праздник.",
    kind = QuestKind.PLAYER,
    firstNodeId = "food",
    hasProgress = true,
    minBalance = 100,
    nodes = mapOf(
        "food" to QuestNode(
            id = "food",
            text = "Что возьмём из еды?",
            delayMinutes = 1,
            options = listOf(
                QuestOption("Фрукты и сок", "Вкусно и полезно.", "place"),
                QuestOption("Чипсы", "Дёшево и хрустит.", "place"),
                QuestOption("Ничего", "Все голодные.", "place")
            )
        ),
        "place" to QuestNode(
            id = "place",
            text = "Где устроим пикник?",
            options = listOf(QuestOption("В парке", "Свежий воздух.", Quest.END_NODE))
        )
    )
)

private val PreviewPiggy = Quest(
    id = "piggy_bank",
    title = "Копилка",
    description = "Научись откладывать: терпение приносит монеты.",
    kind = QuestKind.PLAYER,
    firstNodeId = "gift",
    nodes = mapOf(
        "gift" to QuestNode(
            id = "gift",
            text = "Бабушка подарила монетки.",
            options = listOf(QuestOption("В копилку", "Звяк!", Quest.END_NODE))
        )
    )
)

private val PreviewWallet = Quest(
    id = "lost_wallet",
    title = "Потерянный кошелёк",
    description = "На дорожке лежит чужой кошелёк.",
    kind = QuestKind.RANDOM,
    firstNodeId = "found",
    nodes = mapOf(
        "found" to QuestNode(
            id = "found",
            text = "Внутри 30 монет и визитка хозяина. Как поступить?",
            options = listOf(
                QuestOption("Вернуть", "Хозяин обрадовался.", Quest.END_NODE),
                QuestOption("Оставить себе", "На душе неспокойно.", Quest.END_NODE)
            )
        )
    )
)

private val PreviewGuests = Quest(
    id = "guests",
    title = "Гости",
    description = "Скоро придут гости. Подготовься к встрече!",
    kind = QuestKind.RANDOM,
    firstNodeId = "treat",
    hasProgress = true,
    nodes = mapOf(
        "treat" to QuestNode(
            id = "treat",
            text = "Чем угостим гостей?",
            options = listOf(
                QuestOption("Испечь пирог", "Пирог удался.", Quest.END_NODE, moneyDelta = -10),
                QuestOption("Купить торт", "Торт красивый.", Quest.END_NODE, moneyDelta = -30),
                QuestOption("Позвать на чай", "Тепло и бесплатно.", Quest.END_NODE)
            )
        )
    )
)

/** Все состояния карточки разом: ждёт шага, выбор, свободный квест, пройденный. */
private val PreviewEntries = listOf(
    QuestEntry(
        PreviewPicnic,
        QuestProgress(
            questId = "picnic",
            nodeId = "food",
            availableAtMillis = PreviewNow + 42_000L,
            progress = 40,
            lastChoice = QuestOutcome(
                optionLabel = "Фрукты и сок",
                resultText = "Вкусно и полезно. Все сыты и довольны.",
                nextNodeId = "place",
                statEffects = mapOf(StatKind.HUNGER to 20, StatKind.HEALTH to 5),
                moneyDelta = -40,
                progressDelta = 40
            )
        )
    ),
    QuestEntry(PreviewWallet, QuestProgress("lost_wallet", "found", PreviewNow)),
    QuestEntry(PreviewPiggy, null),
    QuestEntry(
        PreviewGuests,
        QuestProgress("guests", Quest.END_NODE, PreviewNow, 80, QuestStatus.FINISHED)
    )
)

/** Превью в светлой теме: раскрыт выбор в случайном квесте. */
@Preview(name = "QuestsScreen — Light", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun QuestsScreenLightPreview() {
    FinGameTheme(darkTheme = false) {
        QuestsScreen(
            entries = PreviewEntries,
            currentMillis = { PreviewNow },
            onStart = {}, onChoose = { _, _ -> }, onAdvance = {}, onRestart = {}, onClose = {},
            balance = 120,
            initiallyExpanded = "lost_wallet"
        )
    }
}

/** Превью в тёмной теме: раскрыт результат с отсчётом. */
@Preview(name = "QuestsScreen — Dark", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun QuestsScreenDarkPreview() {
    FinGameTheme(darkTheme = true) {
        QuestsScreen(
            entries = PreviewEntries,
            currentMillis = { PreviewNow },
            onStart = {}, onChoose = { _, _ -> }, onAdvance = {}, onRestart = {}, onClose = {},
            balance = 120,
            initiallyExpanded = "picnic"
        )
    }
}

/** Узкий телефон с крупным шрифтом: 360 dp, шрифт 1.3 — чипы и кнопки не должны обрезаться. */
@Preview(
    name = "QuestsScreen — Narrow phone, large text",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.3f
)
@Composable
private fun QuestsScreenLargeTextPreview() {
    FinGameTheme(darkTheme = false) {
        QuestsScreen(
            entries = PreviewEntries,
            currentMillis = { PreviewNow },
            onStart = {}, onChoose = { _, _ -> }, onAdvance = {}, onRestart = {}, onClose = {},
            balance = 120,
            depositAmount = 200,
            initiallyExpanded = "picnic"
        )
    }
}

/** Альбомная ориентация телефона. */
@Preview(name = "QuestsScreen — Landscape", showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun QuestsScreenLandscapePreview() {
    FinGameTheme(darkTheme = false) {
        QuestsScreen(
            entries = PreviewEntries,
            currentMillis = { PreviewNow },
            onStart = {}, onChoose = { _, _ -> }, onAdvance = {}, onRestart = {}, onClose = {},
            balance = 120,
            initiallyExpanded = "lost_wallet"
        )
    }
}

/** Планшет: контент не шире [GameDimens.ContentMaxWidth], иконки крупнее. */
@Preview(name = "QuestsScreen — Tablet", showBackground = true, device = Devices.TABLET)
@Composable
private fun QuestsScreenTabletPreview() {
    FinGameTheme(darkTheme = false) {
        QuestsScreen(
            entries = PreviewEntries,
            currentMillis = { PreviewNow },
            onStart = {}, onChoose = { _, _ -> }, onAdvance = {}, onRestart = {}, onClose = {},
            balance = 50,
            initiallyExpanded = "piggy_bank"
        )
    }
}

/** Пустой список: у игрока пока нет квестов. */
@Preview(name = "QuestsScreen — Empty", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun QuestsScreenEmptyPreview() {
    FinGameTheme(darkTheme = false) {
        QuestsScreen(
            entries = emptyList(),
            currentMillis = { PreviewNow },
            onStart = {}, onChoose = { _, _ -> }, onAdvance = {}, onRestart = {}, onClose = {}
        )
    }
}
