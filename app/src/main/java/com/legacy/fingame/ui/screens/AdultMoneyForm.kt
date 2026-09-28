package com.legacy.fingame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.adult.AdultMoney
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.PillStyle
import com.legacy.fingame.ui.theme.FinGameTheme

/**
 * Форма взрослого «Изменить монеты»: добавить или убрать, сумма с цифровой клавиатуры и причина —
 * обязательная, её увидит ребёнок в своём журнале («Взрослый добавил 50: за уборку»). Убрать
 * больше, чем есть на счёте, нельзя. Всё прокручивается, клавиатура телефона не закрывает поле.
 *
 * @param balance что сейчас на текущем счёте ребёнка.
 * @param onSave записать: сумма, добавить ли, причина; true — записано, форма закрывается.
 * @param onCancel закрыть без изменений.
 */
@Composable
internal fun AdultMoneyForm(
    balance: Int,
    onSave: (amount: Int, add: Boolean, reason: String) -> Boolean,
    onCancel: () -> Unit
) {
    var add by rememberSaveable { mutableStateOf(true) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var tried by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val amount = amountText.toIntOrNull() ?: 0
    val errors = AdultMoney.validate(amount, add, reason, balance)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShrinkText(
                text = "Изменить монеты",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                minSize = 12.dp
            )
            PillButton(text = "Отмена", onClick = onCancel, style = PillStyle.Text, compact = true)
        }
        Text(
            text = "Сейчас на счёте: ${coinsText(balance)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(
                text = "Добавить",
                onClick = { add = true },
                selected = add,
                compact = true,
                modifier = Modifier.weight(1f)
            )
            PillButton(
                text = "Убрать",
                onClick = { add = false },
                selected = !add,
                compact = true,
                modifier = Modifier.weight(1f)
            )
        }
        FormLabel("Сколько монет (1–${AdultMoney.AMOUNT_MAX})")
        PriceBox(amountText)
        Keypad(
            onDigit = { digit ->
                if (amountText.length < AdultMoney.AMOUNT_MAX.toString().length) {
                    amountText = (amountText + digit).trimStart('0')
                }
            },
            onErase = { amountText = amountText.dropLast(1) }
        )
        FormLabel("Причина — её увидит ребёнок")
        OutlinedTextField(
            value = reason,
            onValueChange = { text ->
                reason = text.filterNot { it == '\n' || it.code < 0x20 }.take(AdultMoney.REASON_MAX)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (add) "Например, за уборку" else "Например, разбил чашку") },
            supportingText = { Text("${reason.length}/${AdultMoney.REASON_MAX}") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
        if (errors.isEmpty()) {
            val delta = if (add) amount else -amount
            Text(
                text = "Ребёнок увидит: «${MoneyLog.adultReason(delta, AdultMoney.cleanReason(reason))}»",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (tried) {
            Text(
                text = errors.joinToString("\n"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        PillButton(
            text = "Сохранить",
            onClick = {
                focusManager.clearFocus()
                tried = true
                if (errors.isEmpty()) onSave(amount, add, reason)
            },
            style = PillStyle.Primary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "Adult money form", showBackground = true, widthDp = 360, heightDp = 900, fontScale = 1.3f)
@Composable
private fun AdultMoneyFormPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(16.dp)) {
                AdultMoneyForm(balance = 120, onSave = { _, _, _ -> true }, onCancel = {})
            }
        }
    }
}

@Preview(name = "Adult money form — landscape", showBackground = true, widthDp = 891, heightDp = 411)
@Composable
private fun AdultMoneyFormLandscapePreview() {
    FinGameTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(16.dp)) {
                AdultMoneyForm(balance = 120, onSave = { _, _, _ -> true }, onCancel = {})
            }
        }
    }
}
