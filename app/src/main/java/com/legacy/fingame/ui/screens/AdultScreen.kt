package com.legacy.fingame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import com.legacy.fingame.ui.components.pillButtonAutoSizeRange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.adult.DayReport
import com.legacy.fingame.game.adult.QuestHistoryEntry
import com.legacy.fingame.game.adult.dayReportsOf
import com.legacy.fingame.game.adult.purchasesOf
import com.legacy.fingame.game.adult.questHistoryOf
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.game.economy.goalProgress
import com.legacy.fingame.game.items.Goals
import com.legacy.fingame.game.items.Inventory
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCatalog
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.quests.Quest
import com.legacy.fingame.game.quests.QuestCatalog
import com.legacy.fingame.game.quests.QuestChoice
import com.legacy.fingame.game.quests.QuestKind
import com.legacy.fingame.game.quests.QuestLog
import com.legacy.fingame.game.quests.QuestNode
import com.legacy.fingame.game.quests.QuestOption
import com.legacy.fingame.game.quests.QuestProgress
import com.legacy.fingame.ui.components.GoalCard
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens
import com.legacy.fingame.utils.SpriteLoader

/** Размер крестика — как на остальных экранах. */
private val AdultCloseSize = 40.dp

/** Наименьшая ширина колонки карточек: в альбоме помещаются две, на телефоне стоя — одна. */
private val AdultCardMinWidth = 320.dp

/** Иконка товара в строке покупки. */
private val PurchaseIconSize = 32.dp

/** Мельче этого заголовок хаба не ужимается. */
private val TitleMinSize = 16.dp

/** Мельче этого подписи в карточках не ужимаются. */
private val LabelMinSize = 9.dp

/** Сколько вкладок в строке стоя: шесть вкладок — две строки. */
private const val TabsPerRow = 3

/** Поля кнопки вкладки в «буквах» — добавка к длине подписи, когда делится ширина строки. */
private const val TabPaddingChars = 3

/** Шаг между карточками. */
private val CardGap = 12.dp

/**
 * Вкладки хаба взрослого.
 *
 * @property title подпись на кнопке вкладки.
 */
enum class AdultTab(val title: String) {
    DAYS("Дни"),
    PURCHASES("Покупки"),
    QUESTS("Квесты"),
    INVENTORY("Инвентарь"),
    GOALS("Цели"),
    LOG("Журнал")
}

/**
 * Хаб взрослого: что ребёнок планировал и как прошли его дни, что он купил, как проходил квесты,
 * что у него в инвентаре, на что он копит и весь его журнал денег. Только смотреть — всё, что меняет
 * игру, модель в режиме взрослого не пускает.
 *
 * Шапка — заголовок, крестик и ряд вкладок, который прокручивается вбок, если не помещается. В
 * альбоме шапка укладывается в одну строку, а карточки встают в две колонки, если ширины хватает.
 *
 * @param state состояние игры ребёнка.
 * @param itemCatalog товары — для имён и иконок покупок, инвентаря и целей.
 * @param questCatalog квесты — для названий и шагов в истории квестов.
 * @param onClose крестик — назад в настройки, режим взрослого выключается.
 * @param modifier модификатор корня экрана.
 * @param initialTab вкладка, с которой хаб открывается (для превью).
 */
@Composable
fun AdultScreen(
    state: GameUiState,
    itemCatalog: ItemCatalog,
    questCatalog: QuestCatalog,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: AdultTab = AdultTab.DAYS
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    val short = GameDimens.isShortScreen
    val firstDay = adultFirstDayOf(state.moneyLog, state.budgetHistory, state.todayDay, state.questLog)
    // Две колонки карточек нужны только лёжа: стоя — и на планшете — одна колонка читается легче.
    val contentMaxWidth = if (short) GameDimens.ContentMaxWidth * 2 else GameDimens.ContentMaxWidth

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val headerModifier = Modifier
            .widthIn(max = contentMaxWidth)
            .fillMaxWidth()
        if (short) {
            Row(
                modifier = headerModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdultTitle(style = MaterialTheme.typography.titleLarge)
                AdultTabs(tab = tab, onSelect = { tab = it }, modifier = Modifier.weight(1f))
                AdultClose(onClose)
            }
        } else {
            Column(modifier = headerModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AdultTitle(
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    AdultClose(onClose)
                }
                AdultTabs(
                    tab = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.fillMaxWidth(),
                    wrap = true
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = contentMaxWidth)
                .fillMaxWidth()
        ) {
            when (tab) {
                AdultTab.DAYS -> DaysTab(state, itemCatalog, firstDay)
                AdultTab.PURCHASES -> PurchasesTab(state, itemCatalog, firstDay)
                AdultTab.QUESTS -> QuestsTab(state, questCatalog, firstDay)
                AdultTab.INVENTORY -> InventoryScreen(
                    entries = Inventory.entriesOf(
                        owned = state.owned,
                        worn = state.worn,
                        catalog = itemCatalog
                    ),
                    stats = state.stats,
                    onUseItem = {},
                    onToggleWorn = {},
                    onClose = {},
                    readOnly = true
                )
                AdultTab.GOALS -> GoalsTab(state, itemCatalog)
                AdultTab.LOG -> LogScreen(
                    log = state.moneyLog,
                    onOpenBudget = null,
                    onClose = null,
                    balance = state.balance,
                    depositAmount = state.depositAmount,
                    firstDay = firstDay
                )
            }
        }
    }
}

@Composable
private fun AdultTitle(style: TextStyle, modifier: Modifier = Modifier) {
    ShrinkText(
        text = "Режим взрослого",
        style = style,
        color = MaterialTheme.colorScheme.onBackground,
        minSize = TitleMinSize,
        modifier = modifier
    )
}

/**
 * Текст в одну строку, который при нехватке места ужимается целиком, а не режется многоточием —
 * как названия в журнале. Мельче [minSize] (физический размер) не становится.
 */
@Composable
private fun ShrinkText(
    text: String,
    style: TextStyle,
    color: Color,
    minSize: Dp,
    modifier: Modifier = Modifier
) {
    val (minFontSize, maxFontSize) = pillButtonAutoSizeRange(
        minLabelSize = minSize,
        styleFontSize = style.fontSize,
        density = LocalDensity.current
    )
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = minFontSize, maxFontSize = maxFontSize)
    )
}

@Composable
private fun AdultClose(onClose: () -> Unit) {
    SpriteButton(
        assetPath = Sprites.CLOSE,
        contentDescription = "Выйти из режима взрослого",
        onClick = onClose,
        size = AdultCloseSize,
        showIndicator = false
    )
}

/**
 * Вкладки. Стоя — две строки по три кнопки, ширина каждой по длине подписи: все шесть видны сразу,
 * а при крупном шрифте подписи ужимаются целиком (как во всех кнопках игры), а не режутся. Лёжа высоты
 * нет, и вкладки стоят одним рядом, который прокручивается вбок.
 */
@Composable
private fun AdultTabs(
    tab: AdultTab,
    onSelect: (AdultTab) -> Unit,
    modifier: Modifier = Modifier,
    wrap: Boolean = false
) {
    @Composable
    fun TabButton(entry: AdultTab, buttonModifier: Modifier = Modifier) {
        PillButton(
            text = entry.title,
            onClick = { onSelect(entry) },
            selected = entry == tab,
            compact = true,
            autoShrink = wrap,
            modifier = buttonModifier
        )
    }
    if (wrap) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AdultTab.entries.chunked(TabsPerRow).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Ширина — по длине подписи: так при крупном шрифте все подписи ужимаются
                    // одинаково, а не одна «Инвентарь» до многоточия.
                    row.forEach { entry ->
                        TabButton(entry, Modifier.weight((entry.title.length + TabPaddingChars).toFloat()))
                    }
                }
            }
        }
    } else {
        Row(
            modifier = modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdultTab.entries.forEach { entry -> TabButton(entry) }
        }
    }
}

/** Пустая вкладка: одна строка посередине. */
@Composable
private fun EmptyText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Колонки карточек: одна на телефоне стоя, две в альбоме, если ширины хватает. */
@Composable
private fun <T> CardGrid(
    items: List<T>,
    key: (T) -> Any,
    content: @Composable (T) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(AdultCardMinWidth),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(CardGap),
        verticalItemSpacing = CardGap
    ) {
        items(items = items, key = key) { item -> content(item) }
    }
}

/** Карточка хаба — та же, что на экране бюджета: 20 dp, рамка, лёгкая тень. */
@Composable
private fun AdultCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GameColors.cardStroke),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/** Заголовок карточки дня: «День 3» и дата справа. */
@Composable
private fun DayTitle(gameDay: Long, firstDay: Long) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "День$NoBreakSpace${dayNumberOf(gameDay, firstDay)}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = dateTextOf(gameDay),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = GameColors.divider)
}

@Composable
private fun diffColor(diff: Int): Color = when {
    diff > 0 -> GameColors.success
    diff < 0 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurface
}

/**
 * Строка плана: название категории, под ним три ячейки — план, факт и разница со знаком (плюс —
 * в пользу ребёнка, как в отчёте бюджета). Числа в ячейках не переносятся и не режутся: у каждой
 * треть карточки.
 */
@Composable
private fun PlanRow(title: String, planned: Int, actual: Int, diff: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ShrinkText(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            minSize = LabelMinSize
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlanCell("план", planned.toString(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            PlanCell("факт", actual.toString(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            PlanCell("разница", signedAmountText(diff), diffColor(diff), Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlanCell(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ShrinkText(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            minSize = LabelMinSize
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Строка «подпись — значение» в одну строку: подпись слева, число справа. */
@Composable
private fun ValueRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ShrinkText(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            minSize = LabelMinSize
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Итог дня одной строкой: зелёный — план выполнен, красный — перерасход, тихий — без плана. */
@Composable
private fun VerdictText(report: DayReport) {
    Text(
        text = dayVerdictText(report),
        style = MaterialTheme.typography.titleSmall,
        color = when (dayVerdictOf(report)) {
            DayVerdict.DONE -> GameColors.success
            DayVerdict.IN_PROGRESS -> MaterialTheme.colorScheme.onSurface
            DayVerdict.OVERSPENT -> MaterialTheme.colorScheme.error
            DayVerdict.NO_PLAN -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
        softWrap = false
    )
}

/** Покупка одной строкой: иконка, «Имя × N» и сумма. */
@Composable
private fun PurchaseRow(entry: MoneyEntry, itemCatalog: ItemCatalog) {
    val item = entry.itemId?.let { itemCatalog.findItemById(it) }
    val variantId = entry.variantId ?: item?.defaultVariantId ?: ""
    val name = item?.name ?: entry.reason
    val context = LocalContext.current
    val loader = remember(context) { SpriteLoader(context) }
    val icon = remember(item, variantId, loader) {
        purchaseIconOf(
            iconPath = item?.getIconPath(variantId),
            categoryIcon = item?.let { Sprites.shopCategory(it.category.xmlName) }
        ) { loader.hasSprite(it) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Sprite(
            assetPath = icon,
            contentDescription = null,
            modifier = Modifier.size(PurchaseIconSize)
        )
        Text(
            text = if (item != null) purchaseTitleText(name, entry.quantity) else name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = signedAmountText(entry.delta),
            style = MaterialTheme.typography.labelLarge,
            color = if (entry.delta < 0) MaterialTheme.colorScheme.error else GameColors.success,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun DaysTab(state: GameUiState, itemCatalog: ItemCatalog, firstDay: Long) {
    val reports = remember(state.budgetHistory, state.moneyLog, state.budget, state.balance) {
        dayReportsOf(state.budgetHistory, state.moneyLog, current = state.budget, balance = state.balance)
    }
    if (reports.isEmpty()) {
        EmptyText("Пока пусто")
        return
    }
    CardGrid(items = reports, key = { it.gameDay }) { report ->
        DayCard(report, itemCatalog, firstDay)
    }
}

@Composable
private fun DayCard(report: DayReport, itemCatalog: ItemCatalog, firstDay: Long) {
    AdultCard {
        DayTitle(report.gameDay, firstDay)
        val plan = report.plan
        if (plan != null) {
            PlanBlock(plan)
        } else {
            ValueRow(SpendKind.MUST.title, spentText(report.spentMust))
            ValueRow(SpendKind.WANT.title, spentText(report.spentWant))
        }
        if (report.earned > 0) {
            ValueRow("Получено", signedAmountText(report.earned), GameColors.success)
        }
        VerdictText(report)
        if (report.purchases.isNotEmpty()) {
            CardDivider()
            report.purchases.forEach { PurchaseRow(it, itemCatalog) }
        }
    }
}

/** Трата дня: «−60», а ничего — «0». */
private fun spentText(spent: Int): String = signedAmountText(-spent)

@Composable
private fun PlanBlock(plan: BudgetResult) {
    // Черта между категориями — как в отчёте бюджета: иначе три одинаковых блока сливаются.
    PlanRow(SpendKind.MUST.title, plan.plannedMust, plan.actualMust, plan.mustDiff)
    CardDivider()
    PlanRow(SpendKind.WANT.title, plan.plannedWant, plan.actualWant, plan.wantDiff)
    CardDivider()
    PlanRow("Сбережения", plan.plannedSavings, plan.actualSavings, plan.savingsDiff)
    CardDivider()
    if (plan.plannedDeposit > 0) ValueRow("На вклад", plan.plannedDeposit.toString())
}

@Composable
private fun PurchasesTab(state: GameUiState, itemCatalog: ItemCatalog, firstDay: Long) {
    val byDay = remember(state.moneyLog) { purchasesOf(state.moneyLog).groupBy { it.gameDay }.toList() }
    if (byDay.isEmpty()) {
        EmptyText("Покупок пока нет")
        return
    }
    CardGrid(items = byDay, key = { it.first }) { (gameDay, purchases) ->
        AdultCard {
            DayTitle(gameDay, firstDay)
            purchases.forEach { PurchaseRow(it, itemCatalog) }
            CardDivider()
            ValueRow("Всего", signedAmountText(purchases.sumOf { it.delta }), MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun QuestsTab(state: GameUiState, questCatalog: QuestCatalog, firstDay: Long) {
    val history = remember(state.questLog) { questHistoryOf(state.questLog, questCatalog) }
    if (history.isEmpty()) {
        EmptyText("Квестов пока не было")
        return
    }
    CardGrid(items = history, key = { it.questId }) { entry ->
        QuestCard(entry, state.questProgressOf(entry.questId), firstDay)
    }
}

@Composable
private fun QuestCard(entry: QuestHistoryEntry, progress: QuestProgress?, firstDay: Long) {
    AdultCard {
        Text(
            text = entry.quest?.title ?: "Квест удалён",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = adultQuestStatusText(entry.quest, progress),
            style = MaterialTheme.typography.bodySmall,
            color = if (progress?.isFinished == true) {
                GameColors.success
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        CardDivider()
        entry.choices.forEach { choice -> QuestChoiceRow(entry.quest, choice, firstDay) }
    }
}

@Composable
private fun QuestChoiceRow(quest: Quest?, choice: QuestChoice, firstDay: Long) {
    Column(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = questChoiceTitleText(quest, choice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = questChoiceDetailText(choice, firstDay),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoalsTab(state: GameUiState, itemCatalog: ItemCatalog) {
    val goals = remember(state.goals) { Goals.linesOf(state.goals, itemCatalog) }
    if (goals.isEmpty()) {
        EmptyText("Целей пока нет")
        return
    }
    val balance = state.balance
    CardGrid(items = goals, key = { "${it.item.id}:${it.variantId}" }) { goal ->
        val price = goal.item.price
        GoalCard(
            title = goal.item.name,
            progress = goalProgress(balance = balance, price = price),
            percentText = goalPercentText(balance = balance, price = price),
            footerText = goalFooterText(balance = balance, price = price),
            footerIsReady = goalIsReady(balance = balance, price = price),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- Превью ---

private val PreviewApple = Item("apple", "Яблоко", 15, ItemCategory.FOOD, listOf("red"))
private val PreviewHat = Item("hat", "Шляпа", 120, ItemCategory.CLOTHES, listOf("white"))
private val PreviewBall = Item("ball", "Мячик", 60, ItemCategory.entries.last(), listOf("blue"))

private val PreviewItems = object : ItemCatalog {
    private val all = listOf(PreviewApple, PreviewHat, PreviewBall)
    override fun getItemsByCategory(category: ItemCategory): List<Item> =
        all.filter { it.category == category }

    override fun findItemById(itemId: String): Item? = all.find { it.id == itemId }
}

private val PreviewQuest = Quest(
    id = "piggy",
    title = "Копилка",
    description = "",
    kind = QuestKind.PLAYER,
    firstNodeId = "start",
    nodes = mapOf(
        "start" to QuestNode("start", "", listOf(QuestOption("Отложить 20", "", "next"))),
        "next" to QuestNode("next", "", listOf(QuestOption("Не трогать", "", Quest.END_NODE)))
    ),
    hasProgress = true
)

private val PreviewQuests = QuestCatalog.of(listOf(PreviewQuest))

private const val PreviewToday = 20_720L

private val PreviewState = GameUiState(
    balance = 245,
    todayDay = PreviewToday,
    moneyLog = MoneyLog(
        listOf(
            MoneyEntry("Шляпа x1", -120, PreviewToday, 1_790_000_300_000L, "hat", "white", 1, SpendKind.WANT),
            MoneyEntry("Бонус дня", 50, PreviewToday, 1_790_000_200_000L),
            MoneyEntry("Яблоко x3", -45, PreviewToday - 1, 1_789_900_000_000L, "apple", "red", 3, SpendKind.MUST),
            MoneyEntry("Мячик x1", -60, PreviewToday - 2, 1_789_800_000_000L, "ball", "blue", 1, SpendKind.WANT),
            MoneyEntry("Бонус дня", 50, PreviewToday - 2, 1_789_700_000_000L)
        )
    ),
    budget = BudgetState(100, 60, 85, 0, 0, 120, startDay = PreviewToday),
    budgetHistory = listOf(
        BudgetResult(100, 45, 50, 120, 100, 80, 0, startDay = PreviewToday - 1),
        BudgetResult(100, 60, 100, 60, 50, 90, 0, startDay = PreviewToday - 2)
    ),
    questLog = QuestLog(
        listOf(
            QuestChoice("piggy", "next", "Не трогать", 0, 40, PreviewToday, 1_790_000_100_000L),
            QuestChoice("piggy", "start", "Отложить 20", -20, 30, PreviewToday - 1, 1_789_950_000_000L),
            QuestChoice("gone", "a", "Помочь соседу", 15, 0, PreviewToday - 2, 1_789_850_000_000L)
        )
    ),
    quests = listOf(QuestProgress("piggy", "next", 0L, progress = 70)),
    owned = mapOf(ItemSelection("apple", "red") to 2, ItemSelection("hat", "white") to 1),
    worn = setOf(ItemSelection("hat", "white")),
    goals = listOf(ItemSelection("ball", "blue"), ItemSelection("hat", "white"))
)

@Composable
private fun AdultPreview(dark: Boolean, state: GameUiState = PreviewState, tab: AdultTab = AdultTab.DAYS) {
    FinGameTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AdultScreen(
                state = state,
                itemCatalog = PreviewItems,
                questCatalog = PreviewQuests,
                onClose = {},
                initialTab = tab
            )
        }
    }
}

@Preview(name = "Adult — Days, light", widthDp = 411, heightDp = 891)
@Composable
private fun AdultDaysLightPreview() = AdultPreview(dark = false)

@Preview(name = "Adult — Days, dark", widthDp = 411, heightDp = 891)
@Composable
private fun AdultDaysDarkPreview() = AdultPreview(dark = true)

@Preview(name = "Adult — Days, 360dp font 1.3", widthDp = 360, heightDp = 800, fontScale = 1.3f)
@Composable
private fun AdultDaysLargeTextPreview() = AdultPreview(dark = false)

@Preview(name = "Adult — Days, landscape", widthDp = 800, heightDp = 360)
@Composable
private fun AdultDaysLandscapePreview() = AdultPreview(dark = false)

@Preview(name = "Adult — Days, tablet", widthDp = 800, heightDp = 1280)
@Composable
private fun AdultDaysTabletPreview() = AdultPreview(dark = false)

@Preview(name = "Adult — Purchases", widthDp = 411, heightDp = 891)
@Composable
private fun AdultPurchasesPreview() = AdultPreview(dark = false, tab = AdultTab.PURCHASES)

@Preview(name = "Adult — Quests, 360dp font 1.3", widthDp = 360, heightDp = 800, fontScale = 1.3f)
@Composable
private fun AdultQuestsPreview() = AdultPreview(dark = false, tab = AdultTab.QUESTS)

@Preview(name = "Adult — Goals, dark", widthDp = 411, heightDp = 891)
@Composable
private fun AdultGoalsPreview() = AdultPreview(dark = true, tab = AdultTab.GOALS)

@Preview(name = "Adult — Inventory", widthDp = 411, heightDp = 891)
@Composable
private fun AdultInventoryPreview() = AdultPreview(dark = false, tab = AdultTab.INVENTORY)

@Preview(name = "Adult — Log, landscape", widthDp = 800, heightDp = 360)
@Composable
private fun AdultLogPreview() = AdultPreview(dark = false, tab = AdultTab.LOG)

@Preview(name = "Adult — Empty days", widthDp = 411, heightDp = 891)
@Composable
private fun AdultEmptyPreview() = AdultPreview(dark = false, state = GameUiState(todayDay = PreviewToday))

@Preview(name = "Adult — Empty quests, dark", widthDp = 411, heightDp = 891)
@Composable
private fun AdultEmptyQuestsPreview() =
    AdultPreview(dark = true, state = GameUiState(todayDay = PreviewToday), tab = AdultTab.QUESTS)
