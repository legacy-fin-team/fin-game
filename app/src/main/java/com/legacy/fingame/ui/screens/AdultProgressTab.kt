package com.legacy.fingame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.adult.AdultProgress
import com.legacy.fingame.game.adult.ProgressReport
import com.legacy.fingame.game.adult.TopicProgress
import com.legacy.fingame.game.adult.TopicStatus
import com.legacy.fingame.game.quests.QuestCatalog
import com.legacy.fingame.ui.theme.GameColors

/** Цели приложения — что взрослый читает первым. */
const val AdultAppGoalsText =
    "Игра учит обращаться с деньгами через заботу о питомце: планировать траты на период, " +
        "отличать обязательное от желаемого, откладывать и копить, пробовать вклад и ставить " +
        "цели. Здесь видно, какие темы ребёнок уже встречал в игре. Это не оценка — у каждого " +
        "свой темп."

/** Наименьшая ширина колонки карточек — как в остальных вкладках хаба. */
private val ProgressCardMinWidth = 320.dp

/** @return Статус темы нейтральными словами: «Освоено», «В процессе», «Тема ещё впереди». */
fun topicStatusText(status: TopicStatus): String = when (status) {
    TopicStatus.MASTERED -> "Освоено"
    TopicStatus.IN_PROGRESS -> "В процессе"
    TopicStatus.AHEAD -> "Тема ещё впереди"
}

/**
 * @param report отчёт «Прогресса».
 * @return «Освоено 2 темы из 6», а когда ещё ничего — «Все темы ещё впереди».
 */
fun progressSummaryText(report: ProgressReport): String {
    val total = report.topics.size
    if (report.masteredCount == 0 && report.inProgressCount == 0) return "Все темы ещё впереди"
    return buildString {
        append("Освоено ${report.masteredCount} ${topicsWord(report.masteredCount)} из $total")
        if (report.inProgressCount > 0) append("$DotSeparator${report.inProgressCount} в процессе")
    }
}

/** «тема», «темы», «тем» — по числу. */
private fun topicsWord(count: Int): String {
    val lastTwo = count % 100
    val last = count % 10
    return when {
        lastTwo in 11..14 -> "тем"
        last == 1 -> "тема"
        last in 2..4 -> "темы"
        else -> "тем"
    }
}

/**
 * Вкладка «Прогресс»: цели приложения, общий прогресс по темам и каждая тема с тем, что по ней
 * уже было в игре. Только факты и нейтральные слова — никаких оценок ребёнка.
 *
 * @param state состояние игры ребёнка.
 * @param questCatalog квесты — для тем квестов.
 */
@Composable
internal fun ProgressTab(state: GameUiState, questCatalog: QuestCatalog) {
    val report = remember(state, questCatalog) { AdultProgress.compute(state, questCatalog) }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(ProgressCardMinWidth),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp
    ) {
        item(key = "goals", span = StaggeredGridItemSpan.FullLine) {
            AdultCard {
                Text(
                    text = "Чему учит игра",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = AdultAppGoalsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item(key = "summary", span = StaggeredGridItemSpan.FullLine) {
            AdultCard {
                Text(
                    text = "Общий прогресс",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LinearProgressIndicator(
                    progress = { report.masteredFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = GameColors.goalProgress,
                    trackColor = GameColors.goalProgressTrack,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
                Text(
                    text = progressSummaryText(report),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(items = report.topics, key = { "topic:${it.topic.name}" }) { topic ->
            TopicCard(topic)
        }
    }
}

/** Тема: название, статус и признаки — «уже было» и «ещё впереди». */
@Composable
private fun TopicCard(progress: TopicProgress) {
    AdultCard {
        Text(
            text = progress.topic.title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = topicStatusText(progress.status),
            style = MaterialTheme.typography.labelLarge,
            color = statusColor(progress.status)
        )
        if (progress.facts.isEmpty()) {
            Text(
                text = "Тему открывают квесты — их можно добавить во вкладке «Квесты».",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        progress.facts.forEach { fact ->
            Text(
                text = (if (fact.done) "✓ " else "○ ") + fact.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (fact.done) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/** Освоено — зелёным, остальное — спокойными цветами темы: «впереди» не значит «плохо». */
@Composable
private fun statusColor(status: TopicStatus): Color = when (status) {
    TopicStatus.MASTERED -> GameColors.success
    TopicStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
    TopicStatus.AHEAD -> MaterialTheme.colorScheme.onSurfaceVariant
}
