package com.legacy.fingame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.PillButtonMinLabelSize
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.components.pillButtonAutoSizeRange
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors

/** Сколько строк может занять причина, прежде чем её оборвут многоточием. */
private const val ReasonMaxLines = 2

/** Ширина столбца изменения и столбца времени: колонки должны совпадать от строки к строке. */
private val AmountColumnWidth = 76.dp
private val TimeColumnWidth = 104.dp

/**
 * Чем пишется один столбец журнала: стиль надписи и диапазон, в котором она ужимается.
 *
 * Стиль и диапазон ходят вместе намеренно: диапазон считается от размера шрифта этого самого стиля
 * ([pillButtonAutoSizeRange]), и стиль, взятый строкой отдельно от него, разошёлся бы с диапазоном,
 * которым её ужимают, — надпись ужималась бы не от своего размера.
 *
 * @property style стиль, которым написан столбец.
 * @property minFontSize мельче этого надпись не становится.
 * @property maxFontSize крупнее этого — тоже: это размер самого [style].
 */
private data class LogColumnStyle(
    val style: TextStyle,
    val minFontSize: TextUnit,
    val maxFontSize: TextUnit
)

/**
 * @param style стиль столбца.
 * @return Тот же стиль вместе с диапазоном, в котором ужимается написанное им.
 */
@Composable
private fun logColumnStyleOf(style: TextStyle): LogColumnStyle {
    val (minFontSize, maxFontSize) = pillButtonAutoSizeRange(
        minLabelSize = PillButtonMinLabelSize,
        styleFontSize = style.fontSize,
        density = LocalDensity.current
    )
    return LogColumnStyle(style = style, minFontSize = minFontSize, maxFontSize = maxFontSize)
}

/**
 * Экран журнала: всё, что происходило с деньгами игрока, новейшее сверху.
 *
 * Это таблица без шапки: на узком экране три подписи столбцов не помещаются, а строка «причина —
 * сумма — время» читается и без них. Записи уже лежат в нужном порядке
 * ([com.legacy.fingame.game.economy.MoneyLog]), так что экрану нечего сортировать.
 *
 * День показывается порядковым номером, отсчитанным от самой старой записи, которую журнал ещё
 * помнит: игра считает дни от эпохи, и показывать игроку девятнадцатитысячный день бессмысленно.
 *
 * @param log журнал, как его хранит состояние игры.
 * @param onOpenBudget вызывается, когда игрок хочет перейти к бюджету.
 * @param onClose вызывается, когда игрок закрывает экран.
 * @param modifier модификатор корня экрана.
 */
@Composable
fun LogScreen(
    log: MoneyLog,
    onOpenBudget: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = log.entries
    val oldestGameDay = entries.lastOrNull()?.gameDay ?: 0L
    val reasonStyle = logColumnStyleOf(MaterialTheme.typography.bodyMedium)
    val amountStyle = logColumnStyleOf(MaterialTheme.typography.labelLarge)
    val timeStyle = logColumnStyleOf(MaterialTheme.typography.labelMedium)

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Журнал", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.weight(1f))
            PillButton(text = "Бюджет", onClick = onOpenBudget, compact = true)
            Spacer(modifier = Modifier.width(8.dp))
            SpriteButton(
                assetPath = Sprites.CLOSE,
                contentDescription = "Закрыть журнал",
                onClick = onClose,
                size = 64.dp
            )
        }

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Пока ничего не происходило",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(entries) { index, entry ->
                    MoneyLogRow(
                        entry = entry,
                        oldestGameDay = oldestGameDay,
                        reasonStyle = reasonStyle,
                        amountStyle = amountStyle,
                        timeStyle = timeStyle
                    )
                    if (index < entries.lastIndex) {
                        HorizontalDivider(color = GameColors.cardStroke)
                    }
                }
            }
        }
    }
}

/**
 * Одна строка журнала: причина, изменение и время — тремя отдельными столбцами.
 *
 * Изменение и время стоят в столбцах фиксированной ширины ([AmountColumnWidth],
 * [TimeColumnWidth]), а не подстраиваются под свой текст: `LazyColumn` не умеет `IntrinsicSize`
 * между элементами, и только константная ширина держит колонки на одном месте от строки к строке.
 * Причина получает всё, что осталось — весь остаток ширины экрана.
 *
 * @param entry запись журнала.
 * @param oldestGameDay день самой старой хранимой записи, от которого считается номер дня.
 * @param reasonStyle чем пишется столбец причины: стиль и диапазон, в котором он ужимается
 * (см. [LogColumnStyle]). Считается один раз на весь журнал, а не на каждую строку.
 * @param amountStyle чем пишется столбец изменения.
 * @param timeStyle чем пишется столбец времени.
 * @param modifier модификатор строки.
 */
@Composable
private fun MoneyLogRow(
    entry: MoneyEntry,
    oldestGameDay: Long,
    reasonStyle: LogColumnStyle,
    amountStyle: LogColumnStyle,
    timeStyle: LogColumnStyle,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.reason,
            modifier = Modifier.weight(1f),
            style = reasonStyle.style,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = ReasonMaxLines,
            overflow = TextOverflow.Ellipsis,
            // Название товара из данных может оказаться длинным словом, которое некуда перенести:
            // надпись ужимается тем же механизмом, что и подписи кнопок.
            autoSize = TextAutoSize.StepBased(
                minFontSize = reasonStyle.minFontSize,
                maxFontSize = reasonStyle.maxFontSize
            )
        )
        Text(
            text = signedAmountText(entry.delta),
            modifier = Modifier.width(AmountColumnWidth),
            textAlign = TextAlign.End,
            style = amountStyle.style,
            color = if (entry.delta < 0) MaterialTheme.colorScheme.error else GameColors.success,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = amountStyle.minFontSize,
                maxFontSize = amountStyle.maxFontSize
            )
        )
        Text(
            text = dayTimeTextOf(entry.gameDay, oldestGameDay, entry.timestampMillis),
            modifier = Modifier.width(TimeColumnWidth),
            textAlign = TextAlign.End,
            style = timeStyle.style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = timeStyle.minFontSize,
                maxFontSize = timeStyle.maxFontSize
            )
        )
    }
}

/** Журнал, который показывают превью: день бонуса, покупки и операции по вкладу. */
private val PreviewLog = MoneyLog(
    listOf(
        MoneyEntry("Шапка x1", -125, 19_002L, 1_700_003_000_000L),
        MoneyEntry(MoneyLog.REASON_DEPOSIT_INTEREST, 30, 19_002L, 1_700_002_000_000L),
        MoneyEntry(MoneyLog.REASON_DEPOSIT_CLOSED, 200, 19_002L, 1_700_002_000_000L),
        MoneyEntry("Яблоко x4", -60, 19_001L, 1_700_001_000_000L),
        MoneyEntry(MoneyLog.REASON_DEPOSIT_OPENED, -200, 19_000L, 1_700_000_500_000L),
        MoneyEntry(MoneyLog.REASON_DAILY_BONUS, 50, 19_000L, 1_700_000_000_000L)
    )
)

/** Превью журнала в светлой теме. */
@Preview(name = "LogScreen — Light", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun LogScreenLightPreview() {
    FinGameTheme(darkTheme = false) { LogScreen(log = PreviewLog, onOpenBudget = {}, onClose = {}) }
}

/** Превью журнала в тёмной теме. */
@Preview(name = "LogScreen — Dark", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun LogScreenDarkPreview() {
    FinGameTheme(darkTheme = true) { LogScreen(log = PreviewLog, onOpenBudget = {}, onClose = {}) }
}

/** Превью на узком экране с крупным системным шрифтом: строка не должна расползаться. */
@Preview(
    name = "LogScreen — Narrow phone, large text",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.3f
)
@Composable
private fun LogScreenLargeTextPreview() {
    FinGameTheme(darkTheme = false) { LogScreen(log = PreviewLog, onOpenBudget = {}, onClose = {}) }
}

/** Превью в альбомной ориентации. */
@Preview(name = "LogScreen — Landscape", showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun LogScreenLandscapePreview() {
    FinGameTheme(darkTheme = false) { LogScreen(log = PreviewLog, onOpenBudget = {}, onClose = {}) }
}

/** Превью пустого журнала: игрок, с деньгами которого ещё ничего не случалось. */
@Preview(name = "LogScreen — Empty", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun LogScreenEmptyPreview() {
    FinGameTheme(darkTheme = false) { LogScreen(log = MoneyLog.EMPTY, onOpenBudget = {}, onClose = {}) }
}
