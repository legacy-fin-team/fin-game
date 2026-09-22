package com.legacy.fingame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.economy.Budget
import com.legacy.fingame.game.economy.BudgetDraft
import com.legacy.fingame.game.economy.BudgetResult
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.economy.Deposit
import com.legacy.fingame.game.economy.Economy
import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.game.economy.SpendKind
import com.legacy.fingame.ui.components.BalanceChip
import com.legacy.fingame.ui.components.GameDialog
import com.legacy.fingame.ui.components.GameDialogBlock
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens

/** Размер крестика, закрывающего экран, на экране с высотой и на коротком. */
private val CloseButtonSize = 64.dp
private val ShortScreenCloseButtonSize = 44.dp

/** Сколько цифр принимает поле суммы: больше девяти игрок не наберёт и в самой долгой игре. */
private const val AmountMaxDigits = 9

/** Окно, открытое поверх экрана бюджета. Чисто экранное: игре до него дела нет. */
private enum class BudgetWindow { NONE, CONFIRM_BUDGET, CLOSE_DEPOSIT }

/**
 * Экран бюджета: всё, что игрок делает со своими деньгами между покупками.
 *
 * Сверху — итог прошлого периода, чтобы игрок увидел, чем кончился его прошлый план, и только
 * потом решал, как жить дальше. Ниже — ровно один блок про «сейчас»: пока бонус дня не получен,
 * это кнопка бонуса (бонус и открывает новый период, так что планировать до него нечего); после
 * бонуса — раскладка денег; после подтверждения — как идёт текущий период.
 * Действующий вклад показывается своей карточкой в любом из этих состояний: он живёт по своему
 * сроку и от периода не зависит.
 *
 * Экран прокручивается целиком: блоков бывает много, а высоты экрана — на телефоне, повёрнутом
 * набок, и при крупном системном шрифте — мало.
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
    var window by remember { mutableStateOf(BudgetWindow.NONE) }
    val isShortScreen = GameDimens.isShortScreen

    // Бонус дня открывает новый период, поэтому, пока он не получен, раскладывать нечего: экран
    // показывает кнопку бонуса вместо раскладки, а не оба блока сразу.
    val showBonus = state.dailyBonusAvailable
    val showPlanning = !showBonus && state.canPlanBudget

    // Клавиатура отрезает от прокручиваемой области ровно свою высоту (`imePadding`), а не
    // ложится поверх неё: иначе поле, к которому прокрутка и так не доставала, при наборе уходит
    // под клавиатуру вместе со всем, что ниже, — сроком вклада и кнопкой подтверждения.
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // На экране с высотой шапка читается сверху вниз: счета и выход, под ними заголовок. На
        // коротком все трое стоят в одну строку — высота, которую это экономит, достаётся блокам.
        if (isShortScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BalanceChip(balance = state.balance, depositAmount = state.depositAmount)
                Text(
                    text = "Бюджет",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                SpriteButton(
                    assetPath = Sprites.CLOSE,
                    contentDescription = "Закрыть бюджет",
                    onClick = onClose,
                    size = ShortScreenCloseButtonSize
                )
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
                    size = CloseButtonSize
                )
            }
            Text(text = "Бюджет", style = MaterialTheme.typography.headlineMedium)
        }

        state.previousBudgetResult?.let { result -> PreviousBudgetCard(result = result) }

        if (showBonus) {
            DailyBonusCard(onClaimDailyBonus = onClaimDailyBonus)
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

        state.budget?.let { budget ->
            RunningBudgetCard(
                budget = budget,
                balance = state.balance,
                depositAmount = state.depositAmount
            )
        }

        state.deposit?.let { deposit ->
            DepositCard(
                deposit = deposit,
                // Дни игра считает от эпохи; игроку показывается порядковый номер, отсчитанный от
                // самой старой записи журнала, — тот же счёт, по которому дни подписаны в журнале.
                oldestGameDay = state.moneyLog.entries.lastOrNull()?.gameDay ?: deposit.openedDay,
                onCloseEarly = { window = BudgetWindow.CLOSE_DEPOSIT }
            )
        }

        PillButton(text = "Журнал", onClick = onOpenLog)
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
        ResultRow(
            title = SpendKind.WANT.title,
            planned = result.plannedWant,
            actual = result.actualWant,
            diff = result.wantDiff
        )
        ResultRow(
            title = "Сохранить",
            planned = result.plannedSavings,
            actual = result.actualSavings,
            diff = result.savingsDiff
        )
        if (result.plannedDeposit > 0) {
            AmountRow(label = "На вкладе", value = result.plannedDeposit.toString())
        }
        Text(
            text = "Плюс — не потратили всё, минус — вышли за план",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Строка отчёта: заголовок категории и под ним план, факт и разница.
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
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        AmountRow(label = "План", value = planned.toString())
        AmountRow(label = "Факт", value = actual.toString())
        AmountRow(
            label = "Разница",
            value = signedAmountText(diff),
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
 * @param onClaimDailyBonus вызывается по кнопке.
 * @param modifier модификатор карточки.
 */
@Composable
private fun DailyBonusCard(onClaimDailyBonus: () -> Unit, modifier: Modifier = Modifier) {
    BudgetCard(title = "Новый день", modifier = modifier) {
        Text(
            text = "Получите бонус дня, чтобы спланировать бюджет",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PillButton(text = "Бонус дня +${Economy.DAILY_BONUS}", onClick = onClaimDailyBonus)
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
 * @param total сколько всего раскладывается: всё, что на текущем счёте.
 * @param draft раскладка, которую игрок уже набрал.
 * @param depositAllowed можно ли открыть новый вклад — вклад бывает только один одновременно.
 * @param onDraftChange вызывается с изменённой раскладкой при каждой правке.
 * @param onConfirm вызывается, когда игрок нажал «Подтвердить бюджет» (экран после этого
 *   показывает окно подтверждения).
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
    BudgetCard(title = "Распределите $total", modifier = modifier) {
        if (depositAllowed) {
            AmountField(
                label = "На вклад",
                value = draft.depositAmount,
                onValueChange = { onDraftChange(draft.copy(depositAmount = it)) }
            )
            if (draft.depositAmount > 0) {
                TermPicker(
                    selected = draft.depositTermDays,
                    onSelect = { onDraftChange(draft.copy(depositTermDays = it)) }
                )
                val rate = Deposit.rateOf(draft.depositTermDays)
                AmountRow(
                    label = "Ставка",
                    value = "$rate% · вернётся ${
                        draft.depositAmount + Deposit.interestOf(draft.depositAmount, rate)
                    }"
                )
            }
        } else {
            Text(
                text = "Вклад уже открыт",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AmountField(
            label = SpendKind.MUST.title,
            value = draft.mustSpend,
            onValueChange = { onDraftChange(draft.copy(mustSpend = it)) }
        )
        AmountField(
            label = SpendKind.WANT.title,
            value = draft.wantSpend,
            onValueChange = { onDraftChange(draft.copy(wantSpend = it)) }
        )
        AmountRow(label = "Останется", value = Budget.savingsOf(draft, total).toString())
        PillButton(text = "Подтвердить бюджет", onClick = onConfirm)
    }
}

/**
 * Поле ввода суммы: только цифры, пустое поле читается как ноль.
 *
 * @param label подпись поля.
 * @param value сумма, которая в нём сейчас.
 * @param onValueChange вызывается с новой суммой.
 * @param modifier модификатор поля.
 */
@Composable
private fun AmountField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        // Ноль показывается пустым полем: иначе его нельзя стереть, и набранное поверх него
        // читалось бы как «01». Ноль это поле всё равно и означает.
        value = if (value == 0) "" else value.toString(),
        onValueChange = { typed ->
            onValueChange(typed.filter { it.isDigit() }.take(AmountMaxDigits).toIntOrNull() ?: 0)
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

/**
 * Выбор срока вклада: по кнопке на каждый предлагаемый срок, выбранный выделен.
 *
 * Кнопки переносятся на следующую строку, а не уезжают вбок: прокручивающийся ряд обрывался краем
 * карточки посреди кнопки, и последние сроки нельзя было ни увидеть, ни заподозрить.
 *
 * Выбранный срок выделен цветом выбранной кнопки ([PillButton]`(selected = true)`), а не погашен:
 * погашенная кнопка читается — и вслух объявляется — как недоступная, хотя выбранный срок ровно
 * наоборот. Нажатие на него ничего не меняет и так.
 *
 * @param selected выбранный срок в днях.
 * @param onSelect вызывается с выбранным сроком.
 * @param modifier модификатор ряда.
 */
@Composable
private fun TermPicker(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Deposit.TERM_DAYS.forEach { term ->
            PillButton(
                text = "$term дн",
                onClick = { onSelect(term) },
                compact = true,
                selected = term == selected
            )
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
            onDismiss = onDismiss
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
            Text(
                text = "После подтверждения бюджет не изменить.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            PillButton(text = "Подтвердить", onClick = onConfirm)
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
        AmountRow(
            label = SpendKind.MUST.title,
            value = "${budget.spentMust} из ${budget.plannedMust}",
            valueColor = if (budget.mustLeft < 0) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        AmountRow(
            label = SpendKind.WANT.title,
            value = "${budget.spentWant} из ${budget.plannedWant}",
            valueColor = if (budget.wantLeft < 0) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        AmountRow(
            label = "Осталось потратить",
            value = (budget.plannedSpend - budget.spent).toString()
        )
        AmountRow(label = "Планировали сохранить", value = budget.plannedSavings.toString())
        HorizontalDivider(color = GameColors.cardStroke)
        AmountRow(label = "Сейчас на счету", value = balance.toString())
        if (depositAmount > 0) {
            AmountRow(label = "Во вкладе", value = depositAmount.toString())
        }
    }
}

/**
 * Карточка действующего вклада: на что игрок подписался и когда это кончится.
 *
 * @param deposit открытый вклад.
 * @param oldestGameDay день самой старой записи журнала, от которого игроку считаются дни.
 * @param onCloseEarly вызывается по кнопке досрочного закрытия.
 * @param modifier модификатор карточки.
 */
@Composable
private fun DepositCard(
    deposit: Deposit,
    oldestGameDay: Long,
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
        Text(
            text = "Закроется в день ${dayNumberOf(deposit.maturityDay, oldestGameDay)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PillButton(text = "Закрыть досрочно", onClick = onCloseEarly)
    }
}

/**
 * Окно перед досрочным закрытием вклада: проценты сгорают целиком, и это единственное, что стоит
 * тут сказать.
 *
 * @param amount тело вклада — ровно столько и вернётся.
 * @param onConfirm вызывается, когда игрок подтвердил закрытие.
 * @param onDismiss вызывается, когда закрытие отменено.
 */
@Composable
private fun CloseDepositDialog(amount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    GameDialog(onDismiss = onDismiss) {
        GameDialogBlock(
            title = "Проценты сгорят",
            closeDescription = "Оставить вклад",
            onDismiss = onDismiss
        ) {
            Text(
                text = "Досрочное закрытие вернёт только $amount, проценты не начислятся.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            PillButton(text = "Закрыть вклад", onClick = onConfirm)
        }
    }
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

/** Строка «подпись — значение»: подпись слева, значение справа, обе в одну строку. */
@Composable
private fun AmountRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = valueColor, maxLines = 1)
    }
}

/** Журнал, от которого превью отсчитывают номера дней. */
private val PreviewLog = MoneyLog(
    listOf(
        MoneyEntry(MoneyLog.REASON_DEPOSIT_OPENED, -200, 19_002L, 1_700_002_000_000L),
        MoneyEntry(MoneyLog.REASON_DAILY_BONUS, 50, 19_000L, 1_700_000_000_000L)
    )
)

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
    moneyLog = PreviewLog
)

/** Состояние игрока, который ещё не забрал бонус дня: планировать ему пока нечего. */
private val PreviewBonusState = GameUiState(
    balance = 90,
    dailyBonusAvailable = true,
    moneyLog = PreviewLog
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

/** Превью на узком экране с крупным системным шрифтом: строки не должны обрезаться. */
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
    moneyLog = PreviewLog
)

/** Превью раскладки в светлой теме. */
@Preview(name = "BudgetScreen — Planning", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun BudgetScreenPlanningPreview() {
    FinGameTheme(darkTheme = false) { BudgetScreenPreview(PreviewPlanningState) }
}

/** Превью раскладки на узком экране с крупным системным шрифтом: ряд сроков прокручивается. */
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

/** Превью окна досрочного закрытия вклада. */
@Preview(name = "BudgetScreen — Close deposit window", showBackground = true)
@Composable
private fun CloseDepositDialogPreview() {
    FinGameTheme(darkTheme = false) { CloseDepositDialog(amount = 200, onConfirm = {}, onDismiss = {}) }
}
