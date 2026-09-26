package com.legacy.fingame.ui.screens

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.economy.goalProgress
import com.legacy.fingame.game.items.GoalLine
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.ui.components.GoalCard
import com.legacy.fingame.ui.components.GoalHintCard
import com.legacy.fingame.ui.theme.FinGameTheme

/** Зазор между карточками целей, видный, пока ряд листают. */
private val GoalsGap = 8.dp

/**
 * Цели игрока на главном экране: ряд карточек во всю ширину слота, который листают вбок, и каждая
 * карточка встаёт на место целиком (`rememberSnapFlingBehavior`), а не останавливается на полпути.
 *
 * Сколько бы целей ни было, слот одной высоты — высоты одной карточки: ограничивать число целей не
 * нужно, а угол с вещами игрока не растёт. Какая карточка из скольких, говорит счётчик `1/3` в
 * строке имени — он не добавляет строки. Пока целей нет, на их месте карточка-подсказка той же
 * высоты.
 *
 * Прокрутка помнится через поворот экрана: `rememberLazyListState` сохраняет, какая карточка
 * открыта. Жестам сцены ряд не мешает — он стоит в углу, вне игровой области.
 *
 * @param goals цели в том порядке, в каком игрок их отмечал ([com.legacy.fingame.game.items.Goals]).
 * @param balance текущие деньги игрока — из них считается прогресс каждой цели.
 * @param onOpenGoal нажатие на карточку цели — с целью, по которой нажали.
 * @param onOpenShop нажатие на карточку-подсказку.
 * @param modifier модификатор ряда; ширину задаёт вызывающий.
 */
@Composable
fun GoalsCarousel(
    goals: List<GoalLine>,
    balance: Int,
    onOpenGoal: (ItemSelection) -> Unit,
    onOpenShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (goals.isEmpty()) {
        GoalHintCard(
            title = GoalsHintTitle,
            text = GoalsHintText,
            contentDescription = GoalsHintSpoken,
            onClick = onOpenShop,
            modifier = modifier.fillMaxWidth()
        )
    } else {
        val listState = rememberLazyListState()
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(GoalsGap),
            flingBehavior = rememberSnapFlingBehavior(
                lazyListState = listState,
                snapPosition = SnapPosition.Start
            )
        ) {
            itemsIndexed(
                items = goals,
                key = { _, goal -> goalKeyOf(goal.selection) }
            ) { index, goal ->
                val price = goal.item.price
                val progress = goalProgress(balance = balance, price = price)
                GoalCard(
                    title = goal.item.name,
                    progress = progress,
                    percentText = goalPercentText(balance = balance, price = price),
                    footerText = goalFooterText(balance = balance, price = price),
                    footerIsReady = goalIsReady(balance = balance, price = price),
                    counterText = if (goals.size > 1) {
                        goalCounterText(index = index, count = goals.size)
                    } else {
                        null
                    },
                    counterSpoken = if (goals.size > 1) {
                        goalCounterSpoken(index = index, count = goals.size)
                    } else {
                        null
                    },
                    onClick = { onOpenGoal(goal.selection) },
                    modifier = Modifier.fillParentMaxWidth()
                )
            }
        }
    }
}

/**
 * @param selection цель.
 * @return Ключ карточки в ряду: строка, которую можно сохранить, — ряд помнит по ней, какая
 * карточка открыта.
 */
private fun goalKeyOf(selection: ItemSelection): String =
    "${selection.itemId}$GoalKeySeparator${selection.variantId}"

/**
 * Разделитель id товара и варианта в ключе карточки — тот же непечатный символ, что в
 * [com.legacy.fingame.utils.GoalsCodec]: в id его не бывает, и ключи двух разных целей не совпадут.
 */
private const val GoalKeySeparator = '\u001F'

/**
 * Цели превью: одна по карману (при 250 монетах), одна на полпути и одна с длинным именем — на
 * ней видно, как имя ужимается.
 */
internal val PreviewGoalLines: List<GoalLine> = listOf(
    GoalLine(
        item = Item(
            id = "ball",
            name = "Мячик",
            price = 60,
            category = ItemCategory.TOYS,
            variantIds = listOf("red")
        ),
        variantId = "red"
    ),
    GoalLine(
        item = Item(
            id = "bike",
            name = "Велосипед",
            price = 500,
            category = ItemCategory.TOYS,
            variantIds = listOf("default")
        ),
        variantId = "default"
    ),
    GoalLine(
        item = Item(
            id = "aquarium",
            name = "Большой аквариум с рыбками",
            price = 1200,
            category = ItemCategory.DECOR,
            variantIds = listOf("default")
        ),
        variantId = "default"
    )
)

/**
 * Рамка превью: ряд целей в ширину строки стат-чипов главного экрана.
 *
 * @param width ширина слота: 262 при обычном шрифте, 304 при 1.3.
 * @param goals цели.
 * @param darkTheme тёмная ли тема.
 */
@Composable
private fun GoalsCarouselPreviewFrame(width: Dp, goals: List<GoalLine>, darkTheme: Boolean = false) {
    FinGameTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.padding(16.dp).width(width)) {
                GoalsCarousel(goals = goals, balance = 250, onOpenGoal = {}, onOpenShop = {})
            }
        }
    }
}

/** Целей нет: подсказка. */
@Preview(name = "Goals — нет целей", showBackground = true, widthDp = 300)
@Composable
private fun GoalsEmptyPreview() {
    GoalsCarouselPreviewFrame(width = 262.dp, goals = emptyList())
}

/** Одна цель: без счётчика. */
@Preview(name = "Goals — 1 цель", showBackground = true, widthDp = 300)
@Composable
private fun GoalsOnePreview() {
    GoalsCarouselPreviewFrame(width = 262.dp, goals = PreviewGoalLines.drop(1).take(1))
}

/** Три цели: первая по карману, счётчик «1/3». */
@Preview(name = "Goals — 3 цели", showBackground = true, widthDp = 300)
@Composable
private fun GoalsThreePreview() {
    GoalsCarouselPreviewFrame(width = 262.dp, goals = PreviewGoalLines)
}

/** Три цели в тёмной теме. */
@Preview(name = "Goals — 3 цели, тёмная", showBackground = true, widthDp = 300)
@Composable
private fun GoalsThreeDarkPreview() {
    GoalsCarouselPreviewFrame(width = 262.dp, goals = PreviewGoalLines, darkTheme = true)
}

/** Три цели на узком телефоне с крупным шрифтом — слот 304. */
@Preview(name = "Goals — 3 цели, 360 dp, 1.3", showBackground = true, widthDp = 340, fontScale = 1.3f)
@Composable
private fun GoalsThreeLargeTextPreview() {
    GoalsCarouselPreviewFrame(width = 304.dp, goals = PreviewGoalLines)
}

/** Длинное имя и крупный шрифт: имя ужимается, строка сумм не переносится. */
@Preview(name = "Goals — длинное имя, 1.3", showBackground = true, widthDp = 340, fontScale = 1.3f)
@Composable
private fun GoalsLongNameLargeTextPreview() {
    GoalsCarouselPreviewFrame(width = 304.dp, goals = PreviewGoalLines.takeLast(1))
}

/** Подсказка с крупным шрифтом. */
@Preview(name = "Goals — нет целей, 1.3", showBackground = true, widthDp = 340, fontScale = 1.3f)
@Composable
private fun GoalsEmptyLargeTextPreview() {
    GoalsCarouselPreviewFrame(width = 304.dp, goals = emptyList())
}
