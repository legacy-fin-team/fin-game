package com.legacy.fingame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.economy.MoneyEntry
import com.legacy.fingame.game.economy.MoneyLog
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors

/** Сколько строк может занять причина, прежде чем её оборвут многоточием. */
private const val ReasonMaxLines = 2

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
 * @param onClose вызывается, когда игрок закрывает экран.
 * @param modifier модификатор корня экрана.
 */
@Composable
fun LogScreen(
    log: MoneyLog,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = log.entries
    val oldestGameDay = entries.lastOrNull()?.gameDay ?: 0L

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Журнал", style = MaterialTheme.typography.headlineMedium)
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
                items(entries) { entry ->
                    MoneyLogRow(entry = entry, oldestGameDay = oldestGameDay)
                    HorizontalDivider(color = GameColors.cardStroke)
                }
            }
        }
    }
}

/**
 * Одна строка журнала: причина слева, изменение и время справа.
 *
 * Изменение и время стоят друг над другом, а не в ряд с причиной: вместе они занимают столько же
 * ширины, сколько самое длинное из них, а не сумму обоих, и причина получает всё остальное —
 * достаточно, чтобы уместиться в две строки вместо того, чтобы обрываться многоточием («Вклад
 * закрыт дос…») тем раньше, чем длиннее рядом сумма.
 *
 * @param entry запись журнала.
 * @param oldestGameDay день самой старой хранимой записи, от которого считается номер дня.
 * @param modifier модификатор строки.
 */
@Composable
private fun MoneyLogRow(
    entry: MoneyEntry,
    oldestGameDay: Long,
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = ReasonMaxLines,
            overflow = TextOverflow.Ellipsis
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = signedAmountText(entry.delta),
                style = MaterialTheme.typography.labelLarge,
                color = if (entry.delta < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    GameColors.success
                },
                maxLines = 1
            )
            Text(
                text = "Д${dayNumberOf(entry.gameDay, oldestGameDay)} · ${timeTextOf(entry.timestampMillis)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** Журнал, который показывают превью: день бонуса, покупки и операции по вкладу. */
private val PreviewLog = MoneyLog(
    listOf(
        MoneyEntry("Шапка x1", -125, 19_002L, 1_700_003_000_000L),
        MoneyEntry(MoneyLog.REASON_DEPOSIT_INTEREST, 30, 19_002L, 1_700_002_000_000L),
        MoneyEntry(MoneyLog.REASON_DEPOSIT_CLOSED, 200, 19_002L, 1_700_002_000_000L),
        MoneyEntry("Яблоко x4", -60, 19_001L, 1_700_001_000_000L),
        MoneyEntry(MoneyLog.REASON_TO_SAVINGS, -100, 19_000L, 1_700_000_500_000L),
        MoneyEntry(MoneyLog.REASON_DAILY_BONUS, 50, 19_000L, 1_700_000_000_000L)
    )
)

/** Превью журнала в светлой теме. */
@Preview(name = "LogScreen — Light", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun LogScreenLightPreview() {
    FinGameTheme(darkTheme = false) { LogScreen(log = PreviewLog, onClose = {}) }
}

/** Превью журнала в тёмной теме. */
@Preview(name = "LogScreen — Dark", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun LogScreenDarkPreview() {
    FinGameTheme(darkTheme = true) { LogScreen(log = PreviewLog, onClose = {}) }
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
    FinGameTheme(darkTheme = false) { LogScreen(log = PreviewLog, onClose = {}) }
}

/** Превью в альбомной ориентации. */
@Preview(name = "LogScreen — Landscape", showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun LogScreenLandscapePreview() {
    FinGameTheme(darkTheme = false) { LogScreen(log = PreviewLog, onClose = {}) }
}

/** Превью пустого журнала: игрок, с деньгами которого ещё ничего не случалось. */
@Preview(name = "LogScreen — Empty", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun LogScreenEmptyPreview() {
    FinGameTheme(darkTheme = false) { LogScreen(log = MoneyLog.EMPTY, onClose = {}) }
}
