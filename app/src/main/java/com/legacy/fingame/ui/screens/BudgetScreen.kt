package com.legacy.fingame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.economy.Budget
import com.legacy.fingame.game.economy.BudgetDraft
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.economy.Deposit
import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.ui.components.BalanceChip
import com.legacy.fingame.ui.components.GameDialog
import com.legacy.fingame.ui.components.GameDialogBlock
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.PillStyle
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.components.pillButtonAutoSizeRange
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens
import kotlin.math.max

/** Размер крестика, закрывающего экран. */
private val CloseButtonSize = 40.dp

/**
 * Размер кнопок «плюс» и «минус», которыми набирается сумма.
 *
 * Меньше обычной кнопки экрана: кнопки стоят по краям ползунка, и весь ряд должен уместиться в
 * одну строку даже на узком экране с крупным системным шрифтом.
 */
private val AmountButtonSize = 40.dp

/** Зазор внутри ряда набора суммы: между кнопками «минус», «плюс» и ползунком. */
private val AmountRowGap = 8.dp

/**
 * Высота строки «подпись — значение».
 *
 * Одинаковая у всех строк, чтобы список читался ровным столбцом: иначе строка с коротким числом
 * оказывалась ниже соседних, и колонка значений шла ступеньками.
 */
private val AmountRowMinHeight = 32.dp

/** Наименьшая ширина значения: по ней числа выстраиваются в колонку у правого края строки. */
private val AmountValueMinWidth = 72.dp

/** Зазор между подписью строки «подпись — значение» и её значением. */
private val AmountLabelGap = 8.dp

/**
 * Мельче этого подпись строки «подпись — значение» не ужимается.
 *
 * Физический размер, от системного шрифта не зависит: при нём «Необязательные» — 14 знаков
 * моноширинного шрифта — занимают около 130 dp и ещё читаются. Строка, которой и этого мало,
 * раскладывается в два яруса (см. [AmountRow]).
 */
private val AmountLabelMinSize = 9.dp

/** Зазор между строками цифр срока вклада, если ряд перенесётся. */
private val TermChipGap = 8.dp

/** Подпись суммы, которая уходит на новый вклад, в раскладке. */
private const val DepositLabel = "На вклад"

/** Зазор между строками карточки бюджета — тот же, что держит сама карточка ([BudgetCard]). */
private val BudgetCardGap = 8.dp

/**
 * Общий кегль для подписей группы строк одного уровня — «Обязательные» и «Необязательные» в
 * раскладке.
 *
 * Шрифт игры моноширинный: подпись из n знаков занимает n кеглей в ширину, какие бы ни были буквы.
 * Поэтому кегль, при котором помещается самая длинная подпись группы, считается делением, и тот же
 * кегль отдаётся всем строкам группы: каждая, ужимаясь сама, ужала бы только длинное слово, и два
 * заголовка одного уровня вышли бы разного размера. Лишний знак в делителе — запас на межбуквенный
 * интервал и округление, как у подписи [com.legacy.fingame.ui.components.SpriteButton].
 *
 * @param labelRoom ширина, которая остаётся подписи в строке, в dp.
 * @param longestLabel сколько знаков в самой длинной подписи группы.
 * @param styleSize обычный кегль подписи, в dp — с учётом системного шрифта.
 * @param floor мельче этого подпись не ужимается, в dp.
 * @return Кегль в dp: обычный, когда подпись помещается и так; меньше — ровно настолько, чтобы
 * поместилась самая длинная; не меньше [floor]; и обычный, когда подписей нет или места нет вовсе
 * (считать не от чего).
 */
internal fun sharedLabelSizeOf(
    labelRoom: Float,
    longestLabel: Int,
    styleSize: Float,
    floor: Float
): Float {
    if (longestLabel <= 0 || labelRoom <= 0f) return styleSize
    val fitting = labelRoom / (longestLabel + 1)
    return fitting.coerceIn(minOf(floor, styleSize), styleSize)
}

/** Окно, открытое поверх экрана бюджета. Чисто экранное: игре до него дела нет. */
private enum class BudgetWindow { NONE, CONFIRM_BUDGET, CLOSE_DEPOSIT }

/**
 * Экран бюджета: всё, что игрок делает со своими деньгами между покупками.
 *
 * Сверху — итог прошлого периода, чтобы игрок увидел, чем кончился его прошлый план, и сразу под
 * ним — как идёт текущий: два отчёта подряд, прошлый и сегодняшний. Ниже — то, что игрок может
 * сделать прямо сейчас: пока бонус дня не получен, это кнопка бонуса (бонус и открывает новый
 * период, так что планировать до него нечего), после бонуса — раскладка денег.
 * Действующий вклад показывается своей карточкой в любом из этих состояний: он живёт по своему
 * сроку и от периода не зависит.
 *
 * Экран прокручивается целиком: блоков бывает много, а высоты экрана — на телефоне, повёрнутом
 * набок, и при крупном системном шрифте — мало. Вширь содержимое не растягивается дальше
 * [GameDimens.ContentMaxWidth] и стоит по центру: на планшете и в альбоме строка «подпись —
 * значение» иначе разъезжается во всю ширину экрана.
 *
 * @param state состояние игры: счета, бюджет, вклад и раскладка, которую игрок набирает.
 * @param onDraftChange вызывается с изменённой раскладкой при каждой правке полей.
 * @param onConfirmBudget вызывается, когда игрок подтвердил бюджет в окне подтверждения.
 * @param onCloseDepositEarly вызывается, когда игрок подтвердил досрочное закрытие вклада.
 * @param onClaimDailyBonus вызывается по кнопке бонуса дня.
 * @param onOpenLog вызывается, когда игрок открывает журнал.
 * @param onClose вызывается, когда игрок закрывает экран.
 * @param modifier модификатор корня экрана.
 */
@Composable
fun BudgetScreen(
    state: GameUiState,
    onDraftChange: (BudgetDraft) -> Unit,
    onConfirmBudget: () -> Unit,
    onCloseDepositEarly: () -> Unit,
    onClaimDailyBonus: () -> Unit,
    onOpenLog: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Какое окно игрок открыл поверх экрана. Чисто экранное состояние: игре до него дела нет,
    // пока игрок не подтвердил то, о чём окно спрашивает.
    // Переживает поворот и смену темы: активность при них пересоздаётся, и открытое окно иначе
    // пропадало бы из-под пальца, хотя его причина никуда не делась.
    var window by rememberSaveable { mutableStateOf(BudgetWindow.NONE) }
    val isShortScreen = GameDimens.isShortScreen

    // Бонус дня открывает новый период, поэтому, пока он не получен, раскладывать нечего: экран
    // показывает кнопку бонуса вместо раскладки, а не оба блока сразу.
    val showBonus = state.dailyBonusAvailable
    val showPlanning = !showBonus && state.canPlanBudget

    // Клавиатуры на экране больше нет: суммы набираются ползунком и кнопками, поэтому отступ
    // под неё прокручиваемой области не нужен.
    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = GameDimens.ContentMaxWidth)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Дни игра считает от эпохи; игроку показывается порядковый номер, отсчитанный от самой
            // старой записи журнала, — тот же счёт, по которому дни подписаны в журнале. Иначе у
            // игрока вышло бы два разных «дня 1»: один в журнале, другой здесь.
            val firstDay = firstDayOf(state.moneyLog.oldestGameDay, state.todayDay)

            // На экране с высотой шапка читается сверху вниз: счета и выход, под ними заголовок. На
            // коротком все трое стоят в одну строку — высота, которую это экономит, достаётся блокам.
            // Сегодняшний день подписан под шапкой в обоих случаях: без него «закроется в день 23»
            // не с чем сравнить.
            if (isShortScreen) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BalanceChip(balance = state.balance, depositAmount = state.depositAmount)
                        Text(
                            text = "Бюджет",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        SpriteButton(
                            assetPath = Sprites.CLOSE,
                            contentDescription = "Закрыть бюджет",
                            onClick = onClose,
                            size = CloseButtonSize,
                            showIndicator = false
                        )
                    }
                    HintText(text = todayTextOf(state.todayDay, firstDay))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    BalanceChip(balance = state.balance, depositAmount = state.depositAmount)
                    Spacer(modifier = Modifier.weight(1f))
                    SpriteButton(
                        assetPath = Sprites.CLOSE,
                        contentDescription = "Закрыть бюджет",
                        onClick = onClose,
                        size = CloseButtonSize,
                        showIndicator = false
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Бюджет", style = MaterialTheme.typography.headlineSmall)
                    HintText(text = todayTextOf(state.todayDay, firstDay))
                }
            }

            state.previousBudgetResult?.let { result -> PreviousBudgetCard(result = result) }

            state.budget?.let { budget ->
                RunningBudgetCard(
                    budget = budget,
                    balance = state.balance,
                    depositAmount = state.depositAmount
                )
            }

            if (showBonus) {
                DailyBonusCard(
                    income = state.dailyIncome,
                    careHint = state.careHint,
                    onClaimDailyBonus = onClaimDailyBonus
                )
            } else if (showPlanning) {
                PlanningCard(
                    total = state.totalToPlan,
                    draft = state.planningDraft,
                    // Вклад бывает только один одновременно: пока открыт этот, нового поля нет вовсе,
                    // чтобы игрок не набирал сумму, которой всё равно некуда лечь.
                    depositAllowed = state.deposit == null,
                    onDraftChange = onDraftChange,
                    onConfirm = { window = BudgetWindow.CONFIRM_BUDGET }
                )
            }

            state.deposit?.let { deposit ->
                DepositCard(
                    deposit = deposit,
                    todayDay = state.todayDay,
                    oldestGameDay = firstDay,
                    planningOpen = showPlanning,
                    onCloseEarly = { window = BudgetWindow.CLOSE_DEPOSIT }
                )
            }

            // Журнал — не то, ради чего игрок сюда пришёл: одна залитая кнопка на экране достаётся
            // «Подтвердить», а эта остаётся текстом.
            PillButton(text = "Журнал", onClick = onOpenLog, style = PillStyle.Text)
        }
    }

    // Окно, у которого пропала причина, закрывается само, а не стоит и молчит: закрывать нечего,
    // когда вклада уже нет, и подтверждать нечего, когда раскладка уже подтверждена.
    val shownWindow = when (window) {
        BudgetWindow.NONE -> BudgetWindow.NONE
        BudgetWindow.CONFIRM_BUDGET -> window.takeIf { showPlanning } ?: BudgetWindow.NONE
        BudgetWindow.CLOSE_DEPOSIT -> window.takeIf { state.deposit != null } ?: BudgetWindow.NONE
    }

    // Схлопнувшееся окно и забывается: иначе оно вернулось бы само, стоило причине появиться
    // снова.
    LaunchedEffect(shownWindow) {
        if (window != shownWindow) window = shownWindow
    }

    when (shownWindow) {
        BudgetWindow.NONE -> Unit

        BudgetWindow.CONFIRM_BUDGET -> ConfirmBudgetDialog(
            draft = state.planningDraft,
            total = state.totalToPlan,
            onConfirm = {
                onConfirmBudget()
                window = BudgetWindow.NONE
            },
            onDismiss = { window = BudgetWindow.NONE }
        )

        BudgetWindow.CLOSE_DEPOSIT -> CloseDepositDialog(
            amount = state.depositAmount,
            planningOpen = showPlanning,
            onConfirm = {
                onCloseDepositEarly()
                window = BudgetWindow.NONE
            },
            onDismiss = { window = BudgetWindow.NONE }
        )
    }
}

/**
 * Карточка прошлого периода: что игрок планировал, что вышло и на сколько он с планом разошёлся.
 *
 * @param result итог закрытого периода.
 * @param modifier модификатор карточки.
 */
@Composable
private fun PreviousBudgetCard(result: BudgetResult, modifier: Modifier = Modifier) {
    BudgetCard(title = "Прошлый период", modifier = modifier) {
        ResultRow(
            title = SpendKind.MUST.title,
            planned = result.plannedMust,
            actual = result.actualMust,
            diff = result.mustDiff
        )
        ResultDivider()
        ResultRow(
            title = SpendKind.WANT.title,
            planned = result.plannedWant,
            actual = result.actualWant,
            diff = result.wantDiff
        )
        ResultDivider()
        ResultRow(
            title = "Сохранить",
            planned = result.plannedSavings,
            actual = result.actualSavings,
            diff = result.savingsDiff
        )
        if (result.plannedDeposit > 0) {
            ResultDivider()
            AmountRow(label = "На вкладе", value = result.plannedDeposit.toString())
        }
        HintText(text = "Плюс${DashSeparator}не потратили всё, минус${DashSeparator}вышли за план")
    }
}

/**
 * Черта между группами отчёта.
 *
 * Девять почти одинаковых строк подряд читаются стеной текста, в которой не видно, где кончается
 * одна категория и начинается другая. Поля по 4 dp складываются с шагом карточки в 8 dp и дают те
 * самые 12 dp до черты и после неё.
 */
@Composable
private fun ResultDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = GameColors.divider)
}

/**
 * Строка отчёта: подзаголовок категории и под ним план, факт и разница.
 *
 * Подзаголовок написан мельче и бледнее строк под ним, а не крупнее: он называет группу, а не
 * спорит с её содержимым за внимание. Разница выделяется цветом значения, а не размером: плюс —
 * зелёный, минус — цвет ошибки; от размера строки поехали бы все остальные.
 *
 * @param title как называется категория.
 * @param planned сколько игрок собирался на неё потратить — или сохранить.
 * @param actual сколько вышло на самом деле.
 * @param diff разница со знаком: плюс — в пользу игрока.
 * @param modifier модификатор строки.
 */
@Composable
private fun ResultRow(
    title: String,
    planned: Int,
    actual: Int,
    diff: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AmountRow(label = "План", value = planned.toString())
        AmountRow(label = "Факт", value = actual.toString())
        Spacer(modifier = Modifier.height(4.dp))
        AmountRow(
            label = "Разница",
            value = signedAmountText(diff),
            labelColor = MaterialTheme.colorScheme.onSurface,
            valueColor = when {
                diff > 0 -> GameColors.success
                diff < 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/**
 * Карточка бонуса дня: новый период начинается с него, поэтому, пока бонус не получен, других
 * решений на экране нет.
 *
 * @param income сколько монет даст бонус (см. [GameUiState.dailyIncome]).
 * @param careHint почему бонус меньше обычного, или null, когда он полный.
 * @param onClaimDailyBonus вызывается по кнопке.
 * @param modifier модификатор карточки.
 */
@Composable
private fun DailyBonusCard(
    income: Int,
    careHint: String?,
    onClaimDailyBonus: () -> Unit,
    modifier: Modifier = Modifier
) {
    BudgetCard(title = "Новый день", modifier = modifier) {
        HintText(text = "Получите бонус дня, чтобы спланировать бюджет")
        if (careHint != null) {
            HintText(text = careHint)
        }
        // Пока бонус не получен, это единственное действие экрана — значит, ему и достаётся
        // единственная залитая кнопка.
        PillButton(
            text = "Бонус дня +$income",
            onClick = onClaimDailyBonus,
            modifier = Modifier.fillMaxWidth(),
            style = PillStyle.Primary
        )
    }
}

/**
 * Блок раскладки: игрок делит всё, что у него есть, между обязательными тратами, необязательными
 * и вкладом.
 *
 * План сбережений — не поле, а остаток: что не забрали планы трат и вклад, то игрок и собирается
 * сохранить. Поэтому раскладка, в которой суммы не сходятся, не набирается в принципе, и экрану
 * не нужно ругаться на игрока.
 *
 * Читается сверху вниз в том порядке, в каком игрок и решает: сначала обязательные траты, потом
 * необязательные, и только после них — вклад, то есть то, что остаётся, когда о жизни уже
 * подумали. Заканчивается карточка остатком и единственной залитой кнопкой экрана.
 *
 * @param total сколько всего раскладывается: всё, что на текущем счёте.
 * @param draft раскладка, которую игрок уже набрал.
 * @param depositAllowed можно ли открыть новый вклад — вклад бывает только один одновременно.
 * @param onDraftChange вызывается с изменённой раскладкой при каждой правке.
 * @param onConfirm вызывается, когда игрок нажал «Подтвердить» (экран после этого показывает окно
 *   подтверждения).
 * @param modifier модификатор карточки.
 */
@Composable
private fun PlanningCard(
    total: Int,
    draft: BudgetDraft,
    depositAllowed: Boolean,
    onDraftChange: (BudgetDraft) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Сумма ушла из заголовка в первую строку карточки: «Распределите 250» при крупном системном
    // шрифте ломалось надвое, и число оказывалось на отдельной строке без подписи.
    // Шаг один на всю раскладку и считается от всей суммы, а не от того, что осталось строке: остаток
    // меняется с каждой правкой соседних строк, и шаг «плюса» менялся бы на ходу — 25, потом 10.
    val step = amountStepOf(total)

    BudgetCard(title = "Распределите", modifier = modifier) {
        AmountRow(label = "Свободно", value = total.toString())

        // «Обязательные», «Необязательные» и «На вклад» — заголовки одного уровня, и кегль у них
        // один: он считается один раз по самой длинной подписи, а не ужимается у каждой строки
        // по-своему.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val labelFontSize = with(LocalDensity.current) {
                sharedLabelSizeOf(
                    labelRoom = (maxWidth - AmountLabelGap - AmountValueMinWidth).value,
                    longestLabel = maxOf(
                        SpendKind.MUST.title.length,
                        SpendKind.WANT.title.length,
                        DepositLabel.length
                    ),
                    styleSize = MaterialTheme.typography.bodyMedium.fontSize.toDp().value,
                    floor = AmountLabelMinSize.value
                ).dp.toSp()
            }
            Column(verticalArrangement = Arrangement.spacedBy(BudgetCardGap)) {
                AmountPicker(
                    label = SpendKind.MUST.title,
                    value = draft.mustSpend,
                    max = total - draft.depositAmount - draft.wantSpend,
                    step = step,
                    onValueChange = { onDraftChange(draft.copy(mustSpend = it)) },
                    hint = "Еда и игрушки",
                    labelFontSize = labelFontSize
                )

                AmountPicker(
                    label = SpendKind.WANT.title,
                    value = draft.wantSpend,
                    max = total - draft.depositAmount - draft.mustSpend,
                    step = step,
                    onValueChange = { onDraftChange(draft.copy(wantSpend = it)) },
                    hint = "Декор и одежда",
                    labelFontSize = labelFontSize
                )

                if (depositAllowed) {
                    AmountPicker(
                        label = DepositLabel,
                        value = draft.depositAmount,
                        max = total - draft.mustSpend - draft.wantSpend,
                        step = step,
                        onValueChange = { onDraftChange(draft.copy(depositAmount = it)) },
                        labelFontSize = labelFontSize
                    )
                }
            }
        }

        if (depositAllowed) {
            TermPicker(
                selected = draft.depositTermDays,
                onSelect = { onDraftChange(draft.copy(depositTermDays = it)) }
            )
            // Ставка и то, что вернётся, — двумя строками, как в карточке открытого вклада: одной
            // строкой «10% · вернётся 110» на узком экране с крупным шрифтом не помещалась.
            if (draft.depositAmount > 0) {
                val rate = Deposit.rateOf(draft.depositTermDays)
                AmountRow(label = "Ставка", value = "$rate%")
                AmountRow(
                    label = "Вернётся",
                    value = (draft.depositAmount + Deposit.interestOf(draft.depositAmount, rate))
                        .toString(),
                    valueColor = GameColors.success
                )
            }
        } else {
            // Единственная подсказка про вклад на время раскладки: карточка вклада ниже в это время
            // молчит, чтобы две подсказки не спорили друг с другом.
            HintText(
                text = "Вклад уже открыт. Закроете его${DashSeparator}сможете открыть новый прямо здесь"
            )
        }

        AmountRow(label = "Останется", value = Budget.savingsOf(draft, total).toString())
        HintText(text = "Останется${DashSeparator}то, что вы планируете сохранить")
        PillButton(
            text = "Подтвердить",
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            style = PillStyle.Primary
        )
    }
}

/**
 * Набор суммы без цифр: ползунок и кнопки «плюс» и «минус» круглым шагом.
 *
 * Цифры не набираются руками нигде на этом экране: клавиатура на телефоне закрывает половину
 * раскладки, а игра про деньги, а не про набор чисел. Шаг приходит от раскладки и считается от
 * всей суммы ([amountStepOf]), поэтому из сотни набирается «по десять», а из десяти тысяч — «по
 * тысяче», и за время раскладки он не меняется.
 *
 * Кнопок-ярлыков «Ничего», «Половина» и «Всё» здесь больше нет: на трёх суммах подряд они давали
 * девять одинаковых кнопок, от которых экран рябил, а отвечали они на вопрос, который ползунок
 * решает сам. Ноль и весь остаток он набирает у своих краёв, куда и притягивается
 * ([amountSnappedTo]).
 *
 * Когда раскладывать уже нечего ([max] равен нулю), пикер не исчезает — он и есть ответ на вопрос
 * «сколько сюда досталось» — но гасится весь целиком: ползунок и кнопки не отвечают на нажатия и
 * объявляются недоступными, а под ползунком говорится, почему.
 *
 * @param label подпись суммы.
 * @param value сумма, которая набрана сейчас.
 * @param max сколько всего можно разложить в эту сумму.
 * @param step шаг «плюса», «минуса» и ползунка — один на всю раскладку, см. [PlanningCard].
 * @param onValueChange вызывается с новой суммой.
 * @param modifier модификатор блока.
 * @param hint подпись под заголовком: что попадает в эту категорию. Стоит внутри самого пикера,
 *   сразу под своей строкой, а не после ползунка: снизу она читалась как заголовок следующего
 *   пикера, к которому не имеет отношения.
 * @param labelFontSize общий кегль подписи, посчитанный для группы строк, или `null` — строка
 *   ужимает подпись сама (см. [AmountRow]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountPicker(
    label: String,
    value: Int,
    max: Int,
    step: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    labelFontSize: TextUnit? = null
) {
    // Раскладывать нечего: всё уже разошлось по другим строкам, и любое нажатие здесь вернуло бы
    // ту же сумму. Кнопка, которая отвечает нажатием и не меняет ничего, читается как сломанная.
    val enabled = max > 0
    // Ползунок из коробки красится своим собственным зелёным и ставит точку на конце дорожки —
    // рядом с пиксельными кнопками это выглядит деталью из чужой игры.
    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
    )

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AmountRow(label = label, value = value.toString(), labelFontSize = labelFontSize)
        if (hint != null) {
            HintText(text = hint)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AmountRowGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpriteButton(
                assetPath = Sprites.MINUS,
                contentDescription = "$label меньше на $step",
                onClick = { onValueChange(amountSteppedBy(value, -1, max, step)) },
                size = AmountButtonSize,
                enabled = enabled,
                showIndicator = false
            )
            Slider(
                value = value.coerceIn(0, max.coerceAtLeast(0)).toFloat(),
                onValueChange = { onValueChange(amountSnappedTo(it, max, step)) },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                colors = sliderColors,
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        enabled = enabled,
                        colors = sliderColors,
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp
                    )
                },
                valueRange = 0f..max.coerceAtLeast(1).toFloat()
            )
            SpriteButton(
                assetPath = Sprites.PLUS,
                contentDescription = "$label больше на $step",
                onClick = { onValueChange(amountSteppedBy(value, 1, max, step)) },
                size = AmountButtonSize,
                enabled = enabled,
                showIndicator = false
            )
        }
        if (!enabled) {
            HintText(text = "Всё уже распределено")
        }
    }
}

/**
 * Выбор срока вклада: ряд цифр под подписью «Срок, дней».
 *
 * Слово «дн» стоит в подписи один раз, а не на каждой кнопке: шесть подписей «2 дн»…«7 дн» в
 * строку не помещались и переносились сеткой 2 × 3, а шесть голых цифр встают в одну строку даже
 * на узком экране с крупным системным шрифтом. Считается это так: на 360 dp внутри карточки
 * остаётся 296 dp; цифре нужно около 41 dp — один глиф 13 sp при шрифте 1.3 плюс поля компактной
 * кнопки по 12, — и шесть таких кнопок с пятью зазорами по 8 занимают 6 × 41 + 40 = 286 dp.
 * Каждая кнопка — круг: сторона — большее из ширины и высоты самой кнопки ([squareChip]), так что
 * на планшете, где кнопка выше, она не вытягивается в овал. Лишняя ширина ряда расходится поровну
 * между кнопками: на 360 dp при шрифте 1.3 её остаётся 20 dp на пять зазоров, и с постоянным
 * зазором [TermChipGap] шестой круг переносился бы на вторую строку.
 *
 * Ряд — [FlowRow], а не простая строка, только как страховка: при шрифте крупнее 1.3 цифры
 * перенесутся на вторую строку, а не уедут за край карточки. При 1.3 и мельче перенос не
 * срабатывает.
 *
 * Ряду отключён общий для Material наименьший размер кнопки (48 × 48 dp): [PillButton] стоит на
 * `Surface(onClick = …)`, а тот его навязывает, и шесть кнопок по 48 с зазорами заняли бы 328 dp —
 * больше, чем есть в карточке. Высоту это не трогает: её держит собственный минимум кнопки (48 dp,
 * на планшете больше), и палец по-прежнему попадает. На 360 dp при шрифте 1.3 круг выходит 48 dp,
 * шесть кругов — 288 dp из 296.
 *
 * Выбранный срок залит ([PillButton] с `selected = true`), остальные — с `selected = false` —
 * обведены контуром: выбранное заметнее невыбранного. Раньше выходило наоборот — невыбранные сроки
 * кричали зелёным, а выбранный был бледным. Каждая кнопка к тому же объявляет, выбрана ли она,
 * поэтому экранный чтец читает ряд как группу вариантов, а не как шесть отдельных кнопок.
 *
 * @param selected выбранный срок в днях.
 * @param onSelect вызывается с выбранным сроком.
 * @param modifier модификатор блока.
 */
@Composable
private fun TermPicker(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Срок, дней",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(TermChipGap)
            ) {
                Deposit.TERM_DAYS.forEach { term ->
                    PillButton(
                        text = term.toString(),
                        onClick = { onSelect(term) },
                        modifier = Modifier.squareChip(),
                        compact = true,
                        selected = term == selected,
                        autoShrink = false
                    )
                }
            }
        }
    }
}

/**
 * Окно подтверждения бюджета: что куда ляжет, одним списком, и предупреждение о том, что после
 * подтверждения план уже не переписать.
 *
 * @param draft раскладка, которую игрок набрал.
 * @param total сколько всего раскладывается.
 * @param onConfirm вызывается, когда игрок подтвердил бюджет.
 * @param onDismiss вызывается, когда подтверждение отменено.
 */
@Composable
private fun ConfirmBudgetDialog(
    draft: BudgetDraft,
    total: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    GameDialog(onDismiss = onDismiss) {
        GameDialogBlock(
            title = "Бюджет",
            closeDescription = "Отменить подтверждение",
            onDismiss = onDismiss,
            actions = {
                PillButton(
                    text = "Подтвердить",
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    style = PillStyle.Primary,
                    compact = true
                )
                // Рядом с крестиком, который свисает с угла: крестик ребёнок не находит.
                PillButton(
                    text = "Отмена",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    style = PillStyle.Text,
                    compact = true
                )
            }
        ) {
            AmountRow(label = SpendKind.MUST.title, value = draft.mustSpend.toString())
            AmountRow(label = SpendKind.WANT.title, value = draft.wantSpend.toString())
            if (draft.depositAmount > 0) {
                AmountRow(
                    label = "На вклад ${draft.depositTermDays} дн",
                    value = draft.depositAmount.toString()
                )
            }
            AmountRow(label = "Останется", value = Budget.savingsOf(draft, total).toString())
            HintText(
                text = "После подтверждения бюджет не изменить.",
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Карточка текущего периода: сколько из плана по каждой категории уже потрачено и что при этом
 * лежит на счетах.
 *
 * @param budget подтверждённый бюджет периода.
 * @param balance текущие деньги игрока.
 * @param depositAmount тело открытого вклада; ноль, когда вклада нет.
 * @param modifier модификатор карточки.
 */
@Composable
private fun RunningBudgetCard(
    budget: BudgetState,
    balance: Int,
    depositAmount: Int,
    modifier: Modifier = Modifier
) {
    BudgetCard(title = "Текущий период", modifier = modifier) {
        // «Потрачено / план» — самое длинное значение экрана, и рядом с ним термину не остаётся
        // места даже на наименьшем кегле: эти две строки стоят в два яруса.
        AmountRow(
            label = SpendKind.MUST.title,
            stacked = true,
            value = spentOfPlannedText(
                spent = budget.spentMust,
                planned = budget.plannedMust,
                over = budget.mustLeft < 0
            )
        )
        AmountRow(
            label = SpendKind.WANT.title,
            stacked = true,
            value = spentOfPlannedText(
                spent = budget.spentWant,
                planned = budget.plannedWant,
                over = budget.wantLeft < 0
            )
        )
        val leftToSpend = budget.plannedSpend - budget.spent
        AmountRow(
            label = "Осталось",
            value = leftToSpend.toString(),
            valueColor = if (leftToSpend < 0) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        AmountRow(label = "Копим", value = budget.plannedSavings.toString())
        HorizontalDivider(color = GameColors.divider)
        AmountRow(label = "На счету", value = balance.toString())
        if (depositAmount > 0) {
            AmountRow(label = "Во вкладе", value = depositAmount.toString())
        }
    }
}

/**
 * «Потрачено / план» одной строкой.
 *
 * Красным пишется только потраченное и только когда оно вышло за план: раньше краснела вся строка
 * целиком, и «15 из 0» читалось как поломка игры, а не как перерасход. Слово «из» заменено чертой:
 * она короче на два знака и не спорит с числами за внимание.
 *
 * @param spent сколько уже потрачено.
 * @param planned сколько на это планировалось.
 * @param over вышел ли игрок за план.
 * @return Строка вида `15 / 0`, покрашенная по частям.
 */
@Composable
private fun spentOfPlannedText(spent: Int, planned: Int, over: Boolean): AnnotatedString {
    val spentColor = if (over) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val plannedColor = MaterialTheme.colorScheme.onSurface
    val separatorColor = MaterialTheme.colorScheme.onSurfaceVariant

    return buildAnnotatedString {
        withStyle(SpanStyle(color = spentColor)) { append(spent.toString()) }
        withStyle(SpanStyle(color = separatorColor)) { append(" / ") }
        withStyle(SpanStyle(color = plannedColor)) { append(planned.toString()) }
    }
}

/**
 * Карточка действующего вклада: на что игрок подписался и когда это кончится.
 *
 * @param deposit открытый вклад.
 * @param todayDay сегодняшний игровой день: от него считается, сколько вкладу осталось лежать.
 * @param oldestGameDay день, от которого игроку считаются номера дней (см. [firstDayOf]).
 * @param planningOpen идёт ли сейчас раскладка: тогда о новом вкладе говорит сама раскладка, а
 *   карточка молчит; вне раскладки карточка говорит, когда новый вклад можно будет открыть.
 * @param onCloseEarly вызывается по кнопке досрочного закрытия.
 * @param modifier модификатор карточки.
 */
@Composable
private fun DepositCard(
    deposit: Deposit,
    todayDay: Long,
    oldestGameDay: Long,
    planningOpen: Boolean,
    onCloseEarly: () -> Unit,
    modifier: Modifier = Modifier
) {
    BudgetCard(title = "Вклад", modifier = modifier) {
        AmountRow(label = "Сумма", value = deposit.amount.toString())
        AmountRow(label = "Срок", value = "${deposit.termDays} дн")
        AmountRow(label = "Ставка", value = "${deposit.ratePercent}%")
        AmountRow(
            label = "Вернётся",
            value = deposit.payout.toString(),
            valueColor = GameColors.success
        )
        HintText(
            text = depositMaturityTextOf(
                maturityDay = deposit.maturityDay,
                todayDay = todayDay,
                oldestGameDay = oldestGameDay
            )
        )
        // Во время раскладки про новый вклад говорит сама раскладка (см. [PlanningCard]); вторая
        // подсказка здесь повторяла бы её другими словами, а раньше и противоречила ей.
        if (!planningOpen) {
            HintText(text = "Новый вклад можно открыть при следующем планировании")
        }
        PillButton(
            text = "Закрыть досрочно",
            onClick = onCloseEarly,
            modifier = Modifier.fillMaxWidth(),
            style = PillStyle.Outlined
        )
    }
}

/**
 * Окно перед досрочным закрытием вклада: проценты сгорают целиком, и это единственное, что стоит
 * тут сказать.
 *
 * @param amount тело вклада — ровно столько и вернётся.
 * @param planningOpen идёт ли сейчас раскладка: от этого зависит, когда игрок сможет открыть
 *   новый вклад.
 * @param onConfirm вызывается, когда игрок подтвердил закрытие.
 * @param onDismiss вызывается, когда закрытие отменено.
 */
@Composable
private fun CloseDepositDialog(
    amount: Int,
    planningOpen: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    GameDialog(onDismiss = onDismiss) {
        GameDialogBlock(
            title = "Проценты сгорят",
            closeDescription = "Оставить вклад",
            onDismiss = onDismiss,
            actions = {
                PillButton(
                    text = "Закрыть вклад",
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    style = PillStyle.Primary,
                    compact = true
                )
                PillButton(
                    text = "Оставить",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    style = PillStyle.Text,
                    compact = true
                )
            }
        ) {
            Text(
                text = "Досрочное закрытие вернёт только $amount, проценты не начислятся.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            HintText(
                text = if (planningOpen) {
                    "Новый вклад можно открыть прямо в этой раскладке."
                } else {
                    "Новый вклад можно открыть при следующем планировании."
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Делает кнопку квадратной — а круглую пилюлю кругом: сторона — большее из того, что кнопке нужно
 * в ширину и в высоту. Высоту кнопки держит её собственный минимум, который на планшете больше, а
 * ширину — одна цифра, поэтому без этого кнопка срока на планшете выходила вытянутым овалом.
 *
 * @return Модификатор, который меряет кнопку квадратом.
 */
private fun Modifier.squareChip(): Modifier = layout { measurable, constraints ->
    val width = measurable.maxIntrinsicWidth(Constraints.Infinity)
    val height = measurable.minIntrinsicHeight(width)
    val side = max(width, height)
        .coerceIn(constraints.minWidth, constraints.maxWidth)
        .coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(Constraints.fixed(side, side))
    layout(side, side) { placeable.place(0, 0) }
}

/** Карточка блока экрана бюджета: заголовок и содержимое на общей подложке. */
@Composable
private fun BudgetCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GameColors.cardStroke),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

/**
 * Строка «подпись — значение»: подпись слева, значение справа, в одну строку каждая.
 *
 * Подпись всегда в одну строку и никогда не рвётся посреди слова: раньше «Необязательные» Android
 * рвал на «Необязательны / е». Если слово не помещается в оставшуюся от значения ширину, оно
 * ужимается целиком — `autoSize` до [AmountLabelMinSize], — а не обрезается. Термины сокращать
 * нельзя, поэтому ужимается кегль, а не слово.
 *
 * Значение стоит у правого края и не уже [AmountValueMinWidth]: так числа соседних строк
 * выстраиваются в колонку, и глазу есть за что зацепиться, сравнивая их.
 *
 * Когда значение длинное — «90 / 140» занимает половину карточки, — подписи не хватило бы и
 * наименьшего кегля. Такая строка просит [stacked]: подпись идёт сверху на всю ширину, значение —
 * под ней у правого края.
 *
 * @param label подпись строки.
 * @param value значение так, как его читает игрок; может быть покрашено по частям (см.
 *   [spentOfPlannedText]).
 * @param modifier модификатор строки.
 * @param stacked разложить ли строку в два яруса: подпись сверху, значение под ней.
 * @param labelColor цвет подписи: обычно тише значения, но у итоговой строки — вровень с ним.
 * @param valueColor цвет значения: им отмечается перерасход и выгода. Части значения, у которых
 *   свой цвет, его не слушают.
 * @param labelFontSize кегль подписи, общий для группы строк одного уровня (см. [sharedLabelSizeOf]),
 *   или `null` — подпись ужимается сама, по своей ширине. Общий кегль уже посчитан так, чтобы
 *   самая длинная подпись группы поместилась, поэтому `autoSize` с ним не нужен: он ужал бы одну
 *   строку группы, а соседнюю оставил бы как есть.
 */
@Composable
private fun AmountRow(
    label: String,
    value: AnnotatedString,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    labelFontSize: TextUnit? = null
) {
    val labelStyle = MaterialTheme.typography.bodyMedium.let { style ->
        if (labelFontSize == null) style else style.copy(fontSize = labelFontSize)
    }
    val (labelMinFontSize, labelMaxFontSize) = pillButtonAutoSizeRange(
        minLabelSize = AmountLabelMinSize,
        styleFontSize = labelStyle.fontSize,
        density = LocalDensity.current
    )
    // Перенос не отключается (`softWrap` остаётся включённым): только так `autoSize` меряет подпись
    // по ширине, которая у неё есть, и узнаёт, что слово не помещается.
    val labelText: @Composable (Modifier) -> Unit = { labelModifier ->
        Text(
            text = label,
            modifier = labelModifier,
            style = labelStyle,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = if (labelFontSize == null) {
                TextAutoSize.StepBased(
                    minFontSize = labelMinFontSize,
                    maxFontSize = labelMaxFontSize
                )
            } else {
                null
            }
        )
    }
    val valueText: @Composable (Modifier) -> Unit = { valueModifier ->
        Text(
            text = value,
            modifier = valueModifier,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor,
            maxLines = 1,
            textAlign = TextAlign.End
        )
    }

    if (stacked) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AmountRowMinHeight),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            labelText(Modifier.fillMaxWidth())
            valueText(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AmountRowMinHeight),
            horizontalArrangement = Arrangement.spacedBy(AmountLabelGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            labelText(Modifier.weight(1f))
            valueText(Modifier.widthIn(min = AmountValueMinWidth))
        }
    }
}

/**
 * Та же строка, когда значение целиком одного цвета.
 *
 * @param label подпись строки.
 * @param value значение так, как его читает игрок.
 * @param modifier модификатор строки.
 * @param stacked разложить ли строку в два яруса.
 * @param labelColor цвет подписи.
 * @param valueColor цвет значения.
 * @param labelFontSize общий кегль подписи группы или `null`.
 */
@Composable
private fun AmountRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    labelFontSize: TextUnit? = null
) {
    AmountRow(
        label = label,
        value = AnnotatedString(value),
        modifier = modifier,
        stacked = stacked,
        labelColor = labelColor,
        valueColor = valueColor,
        labelFontSize = labelFontSize
    )
}

/**
 * Пояснение под строкой или карточкой: то, что игрок читает, только если не понял остального.
 *
 * Заведено отдельной функцией, чтобы все пояснения экрана были одного кегля и одного цвета: по
 * отдельности они разъезжались, и пятистрочное объяснение про вклад весило столько же, сколько
 * суммы над ним.
 *
 * @param text сам текст.
 * @param modifier модификатор текста.
 * @param textAlign выравнивание; в окнах пояснение стоит по центру, на экране — по левому краю.
 */
@Composable
private fun HintText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign
    )
}

/** Журнал, от которого превью отсчитывают номера дней: самая старая запись — из дня 19 000. */
private val PreviewLog = MoneyLog(
    listOf(
        MoneyEntry(MoneyLog.REASON_DEPOSIT_OPENED, -200, 19_002L, 1_700_002_000_000L),
        MoneyEntry(MoneyLog.REASON_DAILY_BONUS, 50, 19_000L, 1_700_000_000_000L)
    )
)

/**
 * Сегодняшний день превью.
 *
 * Пятый по счёту от начала журнала, и вклад, открытый в третий день на пять дней, закрывается
 * через три: так на превью видно и «Сегодня: день 5», и «Закроется через 3 дн · день 8».
 */
private const val PreviewToday = 19_004L

/** Отчёт, в котором обязательного потрачено сверх плана, а необязательного — меньше плана. */
private val PreviewResult = BudgetResult(
    plannedMust = 160,
    actualMust = 195,
    plannedWant = 60,
    actualWant = 20,
    plannedSavings = 40,
    actualSavings = 45,
    plannedDeposit = 0
)

/** Состояние с итогом прошлого периода, подтверждённым бюджетом и действующим вкладом. */
private val PreviewRunningState = GameUiState(
    balance = 180,
    deposit = Deposit(amount = 200, termDays = 5, ratePercent = 10, openedDay = 19_002L),
    budget = BudgetState(
        plannedMust = 140,
        plannedWant = 80,
        plannedSavings = 60,
        plannedDeposit = 200,
        spentMust = 90,
        spentWant = 30,
        startDay = 19_002L
    ),
    previousBudgetResult = PreviewResult,
    moneyLog = PreviewLog,
    todayDay = PreviewToday
)

/** Состояние игрока, который ещё не забрал бонус дня: планировать ему пока нечего. */
private val PreviewBonusState = GameUiState(
    balance = 90,
    dailyBonusAvailable = true,
    moneyLog = PreviewLog,
    todayDay = PreviewToday
)

/**
 * Экран бюджета в превью: все колбэки пустые, состояние подставляется.
 *
 * @param state состояние, которое показывает превью.
 */
@Composable
private fun BudgetScreenPreview(state: GameUiState) {
    BudgetScreen(
        state = state,
        onDraftChange = {},
        onConfirmBudget = {},
        onCloseDepositEarly = {},
        onClaimDailyBonus = {},
        onOpenLog = {},
        onClose = {}
    )
}

/** Превью с прошлым периодом, текущим бюджетом и вкладом в светлой теме. */
@Preview(name = "BudgetScreen — Light", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun BudgetScreenLightPreview() {
    FinGameTheme(darkTheme = false) { BudgetScreenPreview(PreviewRunningState) }
}

/** То же самое в тёмной теме. */
@Preview(name = "BudgetScreen — Dark", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun BudgetScreenDarkPreview() {
    FinGameTheme(darkTheme = true) { BudgetScreenPreview(PreviewRunningState) }
}

/**
 * Превью на узком экране с крупным системным шрифтом: отчёт прошлого периода и текущий период
 * целиком. Подписи не переносятся и не режутся, числа стоят колонкой, строки «потрачено / план» —
 * в два яруса.
 */
@Preview(
    name = "BudgetScreen — Narrow phone, large text",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.3f
)
@Composable
private fun BudgetScreenLargeTextPreview() {
    FinGameTheme(darkTheme = false) { BudgetScreenPreview(PreviewRunningState) }
}

/** То же самое в тёмной теме: подписи отчёта не должны потеряться на тёмной подложке. */
@Preview(
    name = "BudgetScreen — Narrow phone, large text, dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.3f
)
@Composable
private fun BudgetScreenLargeTextDarkPreview() {
    FinGameTheme(darkTheme = true) { BudgetScreenPreview(PreviewRunningState) }
}

/**
 * Текущий период с перерасходом на узком экране с крупным шрифтом: самое длинное значение экрана,
 * «195 / 160», стоит под своим термином, а не рядом с ним, и красным в нём только первое число.
 */
@Preview(
    name = "BudgetScreen — Overspent, narrow phone, large text",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.3f
)
@Composable
private fun BudgetScreenOverspentLargeTextPreview() {
    FinGameTheme(darkTheme = false) {
        BudgetScreenPreview(
            PreviewRunningState.copy(
                previousBudgetResult = null,
                budget = PreviewRunningState.budget?.copy(
                    plannedMust = 160,
                    spentMust = 195,
                    plannedWant = 1_200,
                    spentWant = 1_150
                )
            )
        )
    }
}

/** Превью в альбомной ориентации: шапка складывается в одну строку. */
@Preview(name = "BudgetScreen — Landscape", showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun BudgetScreenLandscapePreview() {
    FinGameTheme(darkTheme = false) { BudgetScreenPreview(PreviewRunningState) }
}

/** Превью на планшете. */
@Preview(name = "BudgetScreen — Tablet", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun BudgetScreenTabletPreview() {
    FinGameTheme(darkTheme = false) { BudgetScreenPreview(PreviewRunningState) }
}

/** Превью игрока без бюджета, которому доступен бонус дня. */
@Preview(name = "BudgetScreen — Daily bonus", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun BudgetScreenBonusPreview() {
    FinGameTheme(darkTheme = false) { BudgetScreenPreview(PreviewBonusState) }
}

/** Раскладка, которую показывают превью планирования: часть денег отложена, часть — на вклад. */
private val PreviewPlanningDraft =
    BudgetDraft(mustSpend = 120, wantSpend = 40, depositAmount = 100, depositTermDays = 5)

/** Состояние игрока, который забрал бонус и раскладывает деньги на новый период. */
private val PreviewPlanningState = GameUiState(
    balance = 360,
    previousBudgetResult = PreviewResult,
    budgetDraft = PreviewPlanningDraft,
    planningOpen = true,
    moneyLog = PreviewLog,
    todayDay = PreviewToday
)

/** Превью раскладки в светлой теме. */
@Preview(name = "BudgetScreen — Planning", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun BudgetScreenPlanningPreview() {
    FinGameTheme(darkTheme = false) { BudgetScreenPreview(PreviewPlanningState) }
}

/** Превью раскладки в тёмной теме. */
@Preview(name = "BudgetScreen — Planning, dark", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun BudgetScreenPlanningDarkPreview() {
    FinGameTheme(darkTheme = true) { BudgetScreenPreview(PreviewPlanningState) }
}

/**
 * Превью раскладки на узком экране с крупным системным шрифтом — то, ради чего считались ширины:
 * шесть цифр срока стоят в одну строку, «Необязательные» не рвётся и не режется, а целиком
 * ужимается до ≈ 11,5 sp рядом со своей суммой, «Подтвердить» помещается на кнопку во всю ширину.
 */
@Preview(
    name = "BudgetScreen — Planning, narrow phone, large text",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.3f
)
@Composable
private fun BudgetScreenPlanningLargeTextPreview() {
    FinGameTheme(darkTheme = false) { BudgetScreenPreview(PreviewPlanningState) }
}

/** Превью раскладки на планшете: кнопки и поля растут, ширина содержимого — нет. */
@Preview(
    name = "BudgetScreen — Planning, tablet",
    showBackground = true,
    widthDp = 1280,
    heightDp = 800
)
@Composable
private fun BudgetScreenPlanningTabletPreview() {
    FinGameTheme(darkTheme = false) { BudgetScreenPreview(PreviewPlanningState) }
}

/** Превью раскладки в альбомной ориентации. */
@Preview(
    name = "BudgetScreen — Planning, landscape",
    showBackground = true,
    widthDp = 800,
    heightDp = 360
)
@Composable
private fun BudgetScreenPlanningLandscapePreview() {
    FinGameTheme(darkTheme = false) { BudgetScreenPreview(PreviewPlanningState) }
}

/** Превью раскладки игрока, у которого вклад уже открыт: поля нового вклада нет. */
@Preview(
    name = "BudgetScreen — Planning with deposit",
    showBackground = true,
    widthDp = 411,
    heightDp = 891
)
@Composable
private fun BudgetScreenPlanningWithDepositPreview() {
    FinGameTheme(darkTheme = false) {
        BudgetScreenPreview(
            PreviewPlanningState.copy(
                deposit = Deposit(amount = 200, termDays = 5, ratePercent = 10, openedDay = 19_002L),
                budgetDraft = PreviewPlanningDraft.copy(depositAmount = 0)
            )
        )
    }
}

/** Превью окна подтверждения бюджета. */
@Preview(name = "BudgetScreen — Confirm window", showBackground = true)
@Composable
private fun ConfirmBudgetDialogPreview() {
    FinGameTheme(darkTheme = false) {
        ConfirmBudgetDialog(
            draft = PreviewPlanningDraft,
            total = PreviewPlanningState.totalToPlan,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

/**
 * То же окно на узком экране с крупным шрифтом: кнопки стоят колонкой и помещаются целиком, а
 * «Необязательные» в 280 dp окна ужимается целиком, а не режется.
 */
@Preview(
    name = "BudgetScreen — Confirm window, narrow phone, large text",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    fontScale = 1.3f
)
@Composable
private fun ConfirmBudgetDialogLargeTextPreview() {
    FinGameTheme(darkTheme = false) {
        ConfirmBudgetDialog(
            draft = PreviewPlanningDraft,
            total = PreviewPlanningState.totalToPlan,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

/** То же окно в тёмной теме. */
@Preview(name = "BudgetScreen — Confirm window, dark", showBackground = true)
@Composable
private fun ConfirmBudgetDialogDarkPreview() {
    FinGameTheme(darkTheme = true) {
        ConfirmBudgetDialog(
            draft = PreviewPlanningDraft,
            total = PreviewPlanningState.totalToPlan,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

/** Превью окна досрочного закрытия вклада. */
@Preview(name = "BudgetScreen — Close deposit window", showBackground = true)
@Composable
private fun CloseDepositDialogPreview() {
    FinGameTheme(darkTheme = false) {
        CloseDepositDialog(
            amount = 200,
            planningOpen = true,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

/** То же окно на узком экране с крупным шрифтом. */
@Preview(
    name = "BudgetScreen — Close deposit window, narrow phone, large text",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    fontScale = 1.3f
)
@Composable
private fun CloseDepositDialogLargeTextPreview() {
    FinGameTheme(darkTheme = false) {
        CloseDepositDialog(
            amount = 200,
            planningOpen = false,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
