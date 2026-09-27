package com.legacy.fingame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.help.HelpEntry
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens

/** Поля экрана и зазоры между строками списка. */
private val ScreenPadding = 16.dp
private val HeaderGap = 8.dp
private val ListGap = 12.dp
private val RowVerticalPadding = 10.dp
private val RowTextGap = 4.dp

/** Размер кнопки «Назад» — тот же, что у крестика на остальных экранах. */
private val BackButtonSize = 40.dp

/**
 * Экран справки: список игровых терминов и их объяснение простыми словами, открывается из
 * [OptionsScreen].
 *
 * Устроен как [LogScreen] — список, а не окно: список тем длиннее диалога и явно предполагает
 * прокрутку, а не разовое чтение. Каждый термин — одна строка списка, объединённая в один узел для
 * TalkBack ([Modifier.semantics] с `mergeDescendants`), чтобы заголовок и текст читались одной
 * фразой, а не двумя остановками свайпа.
 *
 * @param entries термины в том порядке, в котором их вернул
 * [com.legacy.fingame.game.help.HelpRegistry].
 * @param onClose вызывается при нажатии «Назад».
 * @param modifier модификатор корня экрана.
 */
@Composable
fun HelpScreen(
    entries: List<HelpEntry>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = GameDimens.ContentMaxWidth).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                SpriteButton(
                    assetPath = Sprites.CLOSE,
                    contentDescription = "Назад",
                    onClick = onClose,
                    size = BackButtonSize,
                    showIndicator = false
                )
            }

            Spacer(modifier = Modifier.height(HeaderGap))

            Text(
                text = "Помощь",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(ListGap))
        }

        LazyColumn(
            modifier = Modifier.widthIn(max = GameDimens.ContentMaxWidth).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(items = entries, key = { it.id }) { entry ->
                HelpEntryRow(entry = entry)
                HorizontalDivider(color = GameColors.divider)
            }
        }
    }
}

/**
 * Одна строка справки: термин над его объяснением.
 *
 * @param entry термин, который показывает строка.
 * @param modifier модификатор строки.
 */
@Composable
private fun HelpEntryRow(entry: HelpEntry, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = RowVerticalPadding)
            // Заголовок и текст — одна фраза для TalkBack, а не два отдельных свайпа.
            .semantics(mergeDescendants = true) {
                contentDescription = "${entry.title}. ${entry.text}"
            },
        verticalArrangement = Arrangement.spacedBy(RowTextGap)
    ) {
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = entry.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Термины превью: несколько настоящих, чтобы увидеть, как список выглядит на экране. */
private val PreviewEntries = listOf(
    HelpEntry(
        id = "budget",
        title = "Бюджет",
        text = "План на период: сколько потратишь на нужное, сколько на приятное и сколько отложишь."
    ),
    HelpEntry(
        id = "must_spend",
        title = "Обязательные траты",
        text = "Еда и игрушки — то, без чего питомцу не обойтись. Их покупают в первую очередь."
    ),
    HelpEntry(
        id = "deposit",
        title = "Вклад",
        text = "Монеты, которые ты кладёшь в банк на время. Банк платит за это процент."
    ),
    HelpEntry(
        id = "goal",
        title = "Цель",
        text = "Вещь, на которую ты копишь. Отметь её звёздочкой в магазине."
    )
)

/** Превью экрана справки. */
@Preview(name = "HelpScreen", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun HelpScreenPreview() {
    FinGameTheme(darkTheme = false) {
        HelpScreen(entries = PreviewEntries, onClose = {})
    }
}

/** Превью на узком экране с крупным системным шрифтом. */
@Preview(
    name = "HelpScreen — Narrow phone, large text",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.3f
)
@Composable
private fun HelpScreenLargeTextPreview() {
    FinGameTheme(darkTheme = false) {
        HelpScreen(entries = PreviewEntries, onClose = {})
    }
}

/** Превью в альбомной ориентации. */
@Preview(name = "HelpScreen — Landscape", showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun HelpScreenLandscapePreview() {
    FinGameTheme(darkTheme = false) {
        HelpScreen(entries = PreviewEntries, onClose = {})
    }
}
