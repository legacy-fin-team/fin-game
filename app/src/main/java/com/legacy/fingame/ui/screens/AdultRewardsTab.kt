package com.legacy.fingame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.adult.RewardUsage
import com.legacy.fingame.game.adult.RewardUsageLog
import com.legacy.fingame.ui.theme.GameColors
import java.util.Locale

/**
 * @param usage использованная награда.
 * @param firstDay день, от которого считаются номера (см. [adultFirstDayOf]).
 * @return «Питомец Мурка · день 3, 14:05»; без имени питомца — «день 3, 14:05».
 */
fun rewardUsageDetailText(usage: RewardUsage, firstDay: Long): String {
    val time = String.format(Locale.ROOT, "%tR", usage.timestampMillis)
    val whenText = "день$NoBreakSpace${dayNumberOf(usage.gameDay, firstDay)}, $time"
    return if (usage.petName.isBlank()) whenText else "Питомец ${usage.petName}$DotSeparator$whenText"
}

/**
 * Вкладка «Награды» — журнал «Использованные награды»: что из раздела «Другое» ребёнок использовал,
 * чей питомец и когда. Записи новее прошлого просмотра помечены «новое»; открытая вкладка считается
 * просмотром — счётчик на ней гаснет.
 *
 * @param log журнал наград.
 * @param seenAtMillis когда взрослый смотрел журнал в прошлый раз.
 * @param firstDay день, который называется «день 1».
 * @param onSeen вкладка открыта — всё просмотрено.
 */
@Composable
internal fun RewardsTab(log: RewardUsageLog, seenAtMillis: Long, firstDay: Long, onSeen: () -> Unit) {
    // «Новое» — относительно прошлого просмотра, а не этого: метки не гаснут у взрослого на глазах.
    val previouslySeenAt = remember { seenAtMillis }
    LaunchedEffect(Unit) { onSeen() }
    if (log.entries.isEmpty()) {
        EmptyText(
            "Наград пока не использовали. Добавьте во вкладке «Товары» предмет в раздел «Другое» — " +
                "например, «Поход в кино»."
        )
        return
    }
    CardGrid(items = log.entries, key = { "${it.timestampMillis}:${it.itemId}" }) { usage ->
        AdultCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = usage.itemName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (usage.timestampMillis > previouslySeenAt) {
                    Text(
                        text = "новое",
                        style = MaterialTheme.typography.labelMedium,
                        color = GameColors.success
                    )
                }
            }
            Text(
                text = "Использовано$DotSeparator${rewardUsageDetailText(usage, firstDay)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
