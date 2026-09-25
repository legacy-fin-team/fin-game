package com.legacy.fingame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.ui.components.BalanceChip
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.PillStyle
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.components.pillButtonAutoSizeRange
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens

/** Поля экрана и шаг сетки, по которой расставлено всё остальное. */
private val ScreenPadding = 16.dp
private val HeaderGap = 8.dp
private val ListGap = 12.dp

/** Размер крестика: такой же, как на остальных экранах, и всё ещё удобный для пальца. */
private val CloseButtonSize = 40.dp

/** Наименьшая высота строки журнала — чтобы по ней было легко попасть и легко её прочитать. */
private val RowMinHeight = 48.dp

/** Отступ строки сверху и снизу: плотность списка задаётся здесь и только здесь. */
private val RowVerticalPadding = 8.dp

/**
 * Зазор между названием операции и часом под ним. Строки однострочные и обрезаны по кеглю, так что
 * без зазора хвосты букв названия касались цифр времени.
 */
private val RowTextGap = 4.dp

/**
 * Мельче этого название операции не ужимается. Физический размер, от системного шрифта не зависит:
 * на 360 dp при шрифте 1.3 «Проценты по вкладу» помещается целиком около 13 dp, и до пола остаётся
 * запас.
 */
private val ReasonMinSize = 9.dp

/** Зазор между названием операции и суммой. */
private val RowColumnGap = 12.dp

/**
 * Наименьшая ширина столбца сумм: «−125» просит 72 dp даже при самом крупном системном шрифте, и
 * столбец такой ширины выстраивает числа друг под другом от строки к строке.
 */
private val AmountColumnMinWidth = 72.dp

/**
 * Экран журнала: всё, что происходило с деньгами игрока, новейшее сверху.
 *
 * Шапка идёт двумя ярусами: сверху баланс и крестик, под ними — заголовок экрана и переход к
 * бюджету. В одну строку они не помещаются: на 360 dp при системном шрифте 1.3 «Журнал» просит
 * 156 dp, «Бюджет» — 101 dp, крестик — 40 dp, и вместе с зазорами это ровно та ширина, на которой
 * заголовок начинал уезжать под кнопку. Разведённые по ярусам, они помещаются с запасом.
 *
 * Список — две зоны, а не три столбца: слева название операции, под ним подписью день и час, справа
 * сумма. Название так не переносится на вторую строку, а суммы стоят колонкой
 * ([AmountColumnMinWidth]). Записи сгруппированы по игровым дням, и каждая группа начинается
 * заголовком «ДЕНЬ 1», который держится у верхнего края, пока группа проходит мимо, — это и есть
 * шапка, которой у таблицы не было.
 *
 * День показывается порядковым номером, отсчитанным от самой старой записи, которую журнал ещё
 * помнит: игра считает дни от эпохи, и показывать игроку девятнадцатитысячный день бессмысленно.
 *
 * @param log журнал, как его хранит состояние игры.
 * @param onOpenBudget вызывается, когда игрок хочет перейти к бюджету.
 * @param onClose вызывается, когда игрок закрывает экран.
 * @param modifier модификатор корня экрана.
 * @param balance текущий баланс игрока, показываемый в шапке.
 * @param depositAmount сколько лежит на вкладе, или `0`, когда вклада нет.
 */
@Composable
fun LogScreen(
    log: MoneyLog,
    onOpenBudget: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    balance: Int = 0,
    depositAmount: Int = 0
) {
    val entries = log.entries
    val oldestGameDay = log.oldestGameDay ?: 0L
    // Записи уже лежат новейшими вперёд, так что и дни выходят из группировки в том же порядке.
    val byDay = entries.groupBy { it.gameDay }

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
                    contentDescription = "Закрыть журнал",
                    onClick = onClose,
                    size = CloseButtonSize,
                    showIndicator = false
                )
            }

            Spacer(modifier = Modifier.height(HeaderGap))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ListGap),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Журнал",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                PillButton(
                    text = "Бюджет",
                    onClick = onOpenBudget,
                    style = PillStyle.Text,
                    compact = true
                )
            }

            Spacer(modifier = Modifier.height(ListGap))
        }

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Пока пусто",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.widthIn(max = GameDimens.ContentMaxWidth).fillMaxSize(),
                // Отступ задаёт сама строка: два механизма плотности разом развели бы разделители
                // и текст на разные расстояния.
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                byDay.forEach { (gameDay, dayEntries) ->
                    stickyHeader(key = gameDay) {
                        DayHeader(dayNumber = dayNumberOf(gameDay, oldestGameDay))
                    }
                    // Без ключей: у записи нет ничего, что было бы гарантированно уникальным, а
                    // список только дописывается сверху — порядковый номер тут и есть ключ.
                    items(items = dayEntries) { entry ->
                        MoneyLogRow(entry = entry)
                        // Разделитель стоит и после последней строки: список кончается чертой,
                        // а не обрывом, и все зазоры вокруг черты одинаковые.
                        HorizontalDivider(color = GameColors.divider)
                    }
                }
            }
        }
    }
}

/**
 * Заголовок группы записей одного игрового дня.
 *
 * Держится у верхнего края списка, пока мимо проходит его день, поэтому игрок всегда видит, к
 * какому дню относится то, что он читает, — и строке больше не нужно повторять день в себе.
 *
 * @param dayNumber порядковый номер дня, как его показывают игроку.
 * @param modifier модификатор заголовка.
 */
@Composable
private fun DayHeader(
    dayNumber: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "День $dayNumber".uppercase(),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = RowVerticalPadding),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Одна строка журнала: слева название операции и час под ним подписью, справа — сумма.
 *
 * Двух зон хватает там, где не хватало трёх столбцов: раньше название получало остаток после двух
 * жёстких колонок и на узком экране переносилось на вторую строку, а время ужималось до кегля,
 * который ни с чем на экране не совпадал. Теперь название занимает всё, что остаётся от столбца
 * сумм ([AmountColumnMinWidth]). Название всегда в одну строку: не помещается — ужимается целиком
 * (`autoSize` до [ReasonMinSize]), а не обрезается многоточием посреди слова. Под ним с зазором
 * [RowTextGap] — час записи.
 *
 * День в строке не повторяется: его называет [DayHeader], который висит над группой всё время,
 * пока группа на экране.
 *
 * @param entry запись журнала.
 * @param modifier модификатор строки.
 */
@Composable
private fun MoneyLogRow(
    entry: MoneyEntry,
    modifier: Modifier = Modifier
) {
    val reasonStyle = MaterialTheme.typography.bodyMedium
    val (reasonMinFontSize, reasonMaxFontSize) = pillButtonAutoSizeRange(
        minLabelSize = ReasonMinSize,
        styleFontSize = reasonStyle.fontSize,
        density = LocalDensity.current
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = RowMinHeight)
            .padding(vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(RowTextGap)
        ) {
            // Перенос не отключается: только так `autoSize` меряет название по ширине, которая у
            // него есть, и узнаёт, что оно не помещается.
            Text(
                text = entry.reason,
                style = reasonStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = reasonMinFontSize,
                    maxFontSize = reasonMaxFontSize
                )
            )
            Text(
                text = timeTextOf(entry.timestampMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(RowColumnGap))

        Text(
            text = signedAmountText(entry.delta),
            modifier = Modifier.widthIn(min = AmountColumnMinWidth),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelLarge,
            color = if (entry.delta < 0) MaterialTheme.colorScheme.error else GameColors.success,
            maxLines = 1
        )
    }
}

/** Журнал, который показывают превью: день бонуса, покупки и операции по вкладу. */
private val PreviewLog = MoneyLog(
    listOf(
        MoneyEntry("Шапка x1", -125, 19_002L, 1_700_003_000_000L),
        MoneyEntry(MoneyLog.REASON_DEPOSIT_INTEREST, 30, 19_002L, 1_700_002_000_000L),
        MoneyEntry(MoneyLog.REASON_DEPOSIT_CLOSED, 200, 19_002L, 1_700_002_100_000L),
        MoneyEntry("Яблоко x4", -60, 19_001L, 1_700_001_000_000L),
        MoneyEntry(MoneyLog.REASON_DEPOSIT_OPENED, -200, 19_000L, 1_700_000_500_000L),
        MoneyEntry(MoneyLog.REASON_DAILY_BONUS, 50, 19_000L, 1_700_000_000_000L)
    )
)

/** Превью журнала в светлой теме. */
@Preview(name = "LogScreen — Light", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun LogScreenLightPreview() {
    FinGameTheme(darkTheme = false) {
        LogScreen(log = PreviewLog, onOpenBudget = {}, onClose = {}, balance = 250)
    }
}

/** Превью журнала в тёмной теме. */
@Preview(name = "LogScreen — Dark", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun LogScreenDarkPreview() {
    FinGameTheme(darkTheme = true) {
        LogScreen(log = PreviewLog, onOpenBudget = {}, onClose = {}, balance = 250)
    }
}

/**
 * Превью на узком экране с крупным системным шрифтом — тот самый случай, в котором заголовок
 * наезжал на кнопку «Бюджет»: 360 dp, шрифт 1.3.
 */
@Preview(
    name = "LogScreen — Narrow phone, large text",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.3f
)
@Composable
private fun LogScreenLargeTextPreview() {
    FinGameTheme(darkTheme = false) {
        LogScreen(
            log = PreviewLog,
            onOpenBudget = {},
            onClose = {},
            balance = 250,
            depositAmount = 200
        )
    }
}

/** Превью в альбомной ориентации. */
@Preview(name = "LogScreen — Landscape", showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun LogScreenLandscapePreview() {
    FinGameTheme(darkTheme = false) {
        LogScreen(log = PreviewLog, onOpenBudget = {}, onClose = {}, balance = 250)
    }
}

/** Превью пустого журнала: игрок, с деньгами которого ещё ничего не случалось. */
@Preview(name = "LogScreen — Empty", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun LogScreenEmptyPreview() {
    FinGameTheme(darkTheme = false) {
        LogScreen(log = MoneyLog.EMPTY, onOpenBudget = {}, onClose = {})
    }
}
