package com.legacy.fingame.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.adult.LOCK_TASK_COUNT
import com.legacy.fingame.game.adult.LockTask
import com.legacy.fingame.game.adult.isSolved
import com.legacy.fingame.game.adult.lockTasksOf
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.PillStyle
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Размер крестика — как на остальных экранах. */
private val LockCloseSize = 40.dp

/** Ширина поля ответа: двузначное число с запасом при крупном шрифте. */
private val AnswerBoxWidth = 88.dp

/** Высота строки примера — удобно попасть пальцем. */
private val TaskRowHeight = 52.dp

/** Самый длинный ответ: 9 × 9 = 81. */
private const val AnswerMaxDigits = 2

/** Ширина замка: примеры и клавиатура не растягиваются на планшете во всю ширину. */
private val LockMaxWidth = 420.dp

/** Разделитель ответов в сохраняемой строке. */
private const val AnswersSeparator = ","

/**
 * Замок перед режимом взрослого: три примера на умножение, ответы набираются своей цифровой
 * клавиатурой — системная клавиатура закрыла бы полэкрана и выглядела бы чужой.
 *
 * Ответ набирается в выбранную строку (по нажатию на строку или сам: набрав две цифры, ввод
 * переходит к следующей). «Дальше» ждёт трёх ответов; неверно — строки вздрагивают, появляются
 * новые примеры и подсказка «Не сошлось, попробуй ещё».
 *
 * Примеры и ввод переживают поворот экрана ([rememberSaveable]).
 *
 * @param onSolved вызывается, когда все три примера решены.
 * @param onClose крестик — назад в настройки.
 * @param modifier модификатор корня экрана.
 * @param random кости для примеров; в превью — заданные.
 */
@Composable
fun AdultLockScreen(
    onSolved: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    random: Random = Random.Default
) {
    var seed by rememberSaveable { mutableIntStateOf(random.nextInt()) }
    val tasks = remember(seed) { lockTasksOf(Random(seed)) }
    var answersText by rememberSaveable { mutableStateOf(emptyAnswers()) }
    val answers = answersText.split(AnswersSeparator)
    var active by rememberSaveable { mutableIntStateOf(0) }
    var wrong by rememberSaveable { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val allTyped = answers.all { it.isNotEmpty() }
    val short = GameDimens.isShortScreen
    val scope = rememberCoroutineScope()

    fun setAnswer(index: Int, value: String) {
        answersText = answers.toMutableList().also { it[index] = value }
            .joinToString(AnswersSeparator)
    }

    fun type(digit: Int) {
        wrong = false
        val current = answers[active]
        if (current.length >= AnswerMaxDigits) return
        val next = current + digit
        setAnswer(active, next)
        if (next.length == AnswerMaxDigits && active < LOCK_TASK_COUNT - 1) active += 1
    }

    fun erase() {
        wrong = false
        if (answers[active].isEmpty() && active > 0) active -= 1
        setAnswer(active, answers[active].dropLast(1))
    }

    fun submit() {
        if (isSolved(tasks, answers)) {
            onSolved()
            return
        }
        wrong = true
        seed = random.nextInt()
        answersText = emptyAnswers()
        active = 0
        scope.launch {
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 360
                    -24f at 60
                    20f at 120
                    -14f at 180
                    8f at 240
                    -4f at 300
                }
            )
        }
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Шапка ровно над примерами и клавиатурой: та же ширина, что у них.
        Row(
            modifier = Modifier
                .widthIn(max = if (short) LockMaxWidth * 2 else LockMaxWidth)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Для взрослых",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            SpriteButton(
                assetPath = Sprites.CLOSE,
                contentDescription = "Назад в настройки",
                onClick = onClose,
                size = LockCloseSize,
                showIndicator = false
            )
        }

        val tasksBlock: @Composable (Modifier) -> Unit = { blockModifier ->
            Column(
                modifier = blockModifier.graphicsLayer { translationX = shake.value },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (wrong) "Не сошлось, попробуй ещё" else "Реши примеры",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (wrong) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                tasks.forEachIndexed { index, task ->
                    TaskRow(
                        task = task,
                        answer = answers[index],
                        active = index == active,
                        onClick = { active = index }
                    )
                }
            }
        }
        val keypadBlock: @Composable (Modifier) -> Unit = { blockModifier ->
            Column(
                modifier = blockModifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Keypad(onDigit = ::type, onErase = ::erase)
                PillButton(
                    text = "Дальше",
                    onClick = ::submit,
                    style = PillStyle.Primary,
                    enabled = allTyped,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            if (short) {
                // Лёжа высоты нет, а ширины с запасом: примеры слева, клавиатура справа.
                Row(
                    modifier = Modifier
                        .widthIn(max = LockMaxWidth * 2)
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    tasksBlock(Modifier.weight(1f))
                    keypadBlock(Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = LockMaxWidth)
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    tasksBlock(Modifier.fillMaxWidth())
                    keypadBlock(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** Три пустых ответа одной строкой — так их проще сохранить при повороте экрана. */
private fun emptyAnswers(): String = List(LOCK_TASK_COUNT) { "" }.joinToString(AnswersSeparator)

/**
 * Строка примера: «7 × 8 =» и поле с тем, что набрано. Выбранное поле обведено цветом кнопки.
 *
 * @param task пример.
 * @param answer что набрано.
 * @param active сюда ли идёт ввод.
 * @param onClick выбрать эту строку для ввода.
 */
@Composable
private fun TaskRow(task: LockTask, answer: String, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TaskRowHeight)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${task.a} умножить на ${task.b}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        Text(
            text = "${task.a}$NoBreakSpace×$NoBreakSpace${task.b}$NoBreakSpace=",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            softWrap = false
        )
        Surface(
            modifier = Modifier
                .width(AnswerBoxWidth)
                .height(TaskRowHeight - 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = if (active) 2.dp else 1.dp,
                color = if (active) MaterialTheme.colorScheme.primary else GameColors.cardStroke
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = answer.ifEmpty { "?" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (answer.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Цифровая клавиатура: 1–9 тремя рядами, внизу «Стереть» и 0.
 *
 * @param onDigit нажата цифра.
 * @param onErase нажато «Стереть».
 */
@Composable
private fun Keypad(onDigit: (Int) -> Unit, onErase: () -> Unit) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit ->
                    PillButton(
                        text = digit.toString(),
                        onClick = { onDigit(digit) },
                        style = PillStyle.Tonal,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(
                text = "Стереть",
                onClick = onErase,
                style = PillStyle.Outlined,
                compact = true,
                modifier = Modifier.weight(2f)
            )
            PillButton(
                text = "0",
                onClick = { onDigit(0) },
                style = PillStyle.Tonal,
                compact = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(name = "AdultLock — Light", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AdultLockLightPreview() {
    FinGameTheme(darkTheme = false) {
        AdultLockScreen(onSolved = {}, onClose = {}, random = Random(1))
    }
}

@Preview(name = "AdultLock — Dark", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AdultLockDarkPreview() {
    FinGameTheme(darkTheme = true) {
        AdultLockScreen(onSolved = {}, onClose = {}, random = Random(1))
    }
}

@Preview(
    name = "AdultLock — 360dp, font 1.3",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    fontScale = 1.3f
)
@Composable
private fun AdultLockLargeTextPreview() {
    FinGameTheme(darkTheme = false) {
        AdultLockScreen(onSolved = {}, onClose = {}, random = Random(1))
    }
}

@Preview(name = "AdultLock — Landscape", showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun AdultLockLandscapePreview() {
    FinGameTheme(darkTheme = false) {
        AdultLockScreen(onSolved = {}, onClose = {}, random = Random(1))
    }
}

@Preview(name = "AdultLock — Tablet", showBackground = true, widthDp = 800, heightDp = 1280)
@Composable
private fun AdultLockTabletPreview() {
    FinGameTheme(darkTheme = false) {
        AdultLockScreen(onSolved = {}, onClose = {}, random = Random(1))
    }
}
