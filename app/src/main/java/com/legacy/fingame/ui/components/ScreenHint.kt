package com.legacy.fingame.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.legacy.fingame.game.hints.Hint
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors

/** Ширина карточки — та же, что у остальных окон игры (см. [GameDialogBlock]). */
private val CardMaxWidth = 320.dp

/** Какую долю высоты экрана карточка может занять, чтобы не выйти за его край. */
private const val CardMaxHeightFraction = 0.86f

/** Поля карточки и зазоры между её частями. */
private val CardPadding = 20.dp
private val BlockGap = 16.dp
private val ParagraphGap = 10.dp

/** Высота затенения у края текста, за которым есть ещё строки. */
private val ScrollFadeHeight = 28.dp

/**
 * Окно подсказки к экрану: эмодзи, заголовок, один–три коротких абзаца и кнопка «Понятно!».
 * Показывается один раз при первом входе на экран — когда именно, решает
 * [com.legacy.fingame.game.hints.HintKeys.pending].
 *
 * Окно не держит игрока: его закрывает и кнопка, и системный «Назад», и нажатие мимо карточки —
 * каждый из способов считается «прочитал», так что подсказка не заслоняет экран дольше, чем игрок
 * того хочет, и больше сама не появляется.
 *
 * Карточка не выше [CardMaxHeightFraction] экрана. Заголовок и кнопка «Понятно!» всегда на виду,
 * а прокручиваются только абзацы между ними: так на узком экране с крупным шрифтом и в альбомной
 * ориентации ребёнок сразу видит, как закрыть окно. Если текст не поместился, у его нижнего края
 * лежит затенение — строки уходят под него, и видно, что текст можно листать дальше; такое же
 * затенение сверху появляется, когда текст уже пролистан.
 *
 * @param hint что показать.
 * @param onDismiss вызывается, когда игрок закрыл окно любым способом.
 * @param modifier модификатор внешнего бокса, центрирующего карточку.
 */
@Composable
fun ScreenHint(
    hint: Hint,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val maxCardHeight = LocalConfiguration.current.screenHeightDp.dp * CardMaxHeightFraction

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = CardMaxWidth)
                    .heightIn(max = maxCardHeight),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, GameColors.cardStroke),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(CardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(ParagraphGap)
                    ) {
                        HintHeader(hint = hint, centered = true)
                    }

                    // Только текст забирает остаток высоты карточки и прокручивается; fill = false
                    // не растягивает карточку, если текст короткий и помещается целиком.
                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .padding(top = ParagraphGap)
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(ParagraphGap)
                        ) {
                            HintParagraphs(hint = hint)
                        }
                        if (scrollState.canScrollBackward) {
                            ScrollFade(
                                fromTop = true,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                        if (scrollState.canScrollForward) {
                            ScrollFade(
                                fromTop = false,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }

                    Box(modifier = Modifier.padding(top = BlockGap)) {
                        PillButton(
                            text = "Понятно!",
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            style = PillStyle.Primary,
                            compact = true
                        )
                    }
                }
            }
        }
    }
}

/**
 * Затенение у края прокручиваемого текста: цвет карточки, тающий к тексту. Строки уходят под него,
 * а не обрываются ровной линией, и по этому видно, что дальше ещё есть текст.
 *
 * @param fromTop у верхнего края (текст пролистан) или у нижнего (текст ещё не дочитан).
 * @param modifier модификатор полосы затенения.
 */
@Composable
private fun ScrollFade(
    fromTop: Boolean,
    modifier: Modifier = Modifier
) {
    val surface = MaterialTheme.colorScheme.surface
    val colors = if (fromTop) {
        listOf(surface, Color.Transparent)
    } else {
        listOf(Color.Transparent, surface)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ScrollFadeHeight)
            .background(Brush.verticalGradient(colors))
    )
}

/**
 * Содержимое подсказки без окна и кнопки: эмодзи, заголовок и абзацы. Им же подсказки
 * показываются списком на экране «Помощь».
 *
 * @param hint что показать.
 * @param modifier модификатор колонки.
 * @param centered выровнять ли заголовок и эмодзи по центру (в окне) или по левому краю (в списке).
 */
@Composable
fun ScreenHintContent(
    hint: Hint,
    modifier: Modifier = Modifier,
    centered: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(ParagraphGap)
    ) {
        HintHeader(hint = hint, centered = centered)
        HintParagraphs(hint = hint)
    }
}

/**
 * Эмодзи и заголовок подсказки. В окне эмодзи стоит отдельной строкой над заголовком, в списке
 * «Помощи» — перед ним в той же строке.
 *
 * @param hint что показать.
 * @param centered выровнять ли по центру (в окне) или по левому краю (в списке).
 */
@Composable
private fun HintHeader(
    hint: Hint,
    centered: Boolean
) {
    if (hint.icon.isNotEmpty() && centered) {
        Text(
            text = hint.icon,
            // Украшение: заголовок рядом и так говорит, о чём подсказка.
            modifier = Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.headlineMedium
        )
    }
    Text(
        text = if (hint.icon.isNotEmpty() && !centered) "${hint.icon} ${hint.title}" else hint.title,
        modifier = Modifier.semantics { heading() },
        style = if (centered) {
            MaterialTheme.typography.titleMedium
        } else {
            MaterialTheme.typography.titleSmall
        },
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start
    )
}

/**
 * Абзацы подсказки — по одному тексту на абзац, зазоры между ними задаёт колонка вызывающего.
 *
 * @param hint что показать.
 */
@Composable
private fun HintParagraphs(hint: Hint) {
    hint.paragraphs.forEach { paragraph ->
        Text(
            text = paragraph,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Подсказка превью: самая длинная из настоящих, главного экрана. */
private val PreviewHint = Hint(
    key = "home",
    icon = "🏠",
    title = "Это дом питомца",
    paragraphs = listOf(
        "Чтобы питомцу было хорошо, покупай ему еду и игрушки в магазине, а потом давай их из " +
            "инвентаря.",
        "Следи за тремя шкалами: «Здоровье», «Голод» и «Удовольствие». Со временем они пустеют. " +
            "Еда наполняет «Голод» — полная шкала значит, что питомец сыт. Игрушки поднимают " +
            "«Удовольствие».",
        "Встретил непонятное слово? Открой настройки и нажми «Помощь» — там всё объяснено."
    )
)

/** Подсказка на самом узком экране, под который свёрстана игра. */
@Preview(name = "Hint — 360dp", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ScreenHintPreview() {
    FinGameTheme(darkTheme = false) {
        ScreenHint(hint = PreviewHint, onDismiss = {})
    }
}

/** Подсказка при самом крупном системном шрифте, который поддерживает игра. */
@Preview(
    name = "Hint — 360dp, font 1.3",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.3f
)
@Composable
private fun ScreenHintLargeFontPreview() {
    FinGameTheme(darkTheme = false) {
        ScreenHint(hint = PreviewHint, onDismiss = {})
    }
}

/** Подсказка в альбомной ориентации на низком телефоне: прокручивается только текст, кнопка на виду. */
@Preview(name = "Hint — Landscape", showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun ScreenHintLandscapePreview() {
    FinGameTheme(darkTheme = false) {
        ScreenHint(hint = PreviewHint, onDismiss = {})
    }
}
