package com.legacy.fingame.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legacy.fingame.domain.Goal
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Крупная иконка-эмодзи для питомца/товара. [spritePath] зарезервирован под будущую
 * загрузку картинок из ассетов — пока ассетов нет, поэтому всегда рисуем эмодзи.
 */
@Composable
fun GameIcon(
    emoji: String,
    spritePath: String? = null,
    size: Dp,
    modifier: Modifier = Modifier
) {
    // TODO: когда появятся спрайты — загружать изображение по spritePath вместо эмодзи.
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = (size.value * 0.62f).sp
        )
    }
}

/** Круглая кнопка-эмодзи с тенью и обводкой — основной интерактивный элемент игры. */
@Composable
fun RoundIconButton(
    emoji: String,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .size(size)
            .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = size / 2),
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = emoji,
                fontSize = (size.value * 0.46f).sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private val thousandsFormat = DecimalFormat(
    "#,###",
    DecimalFormatSymbols(Locale.forLanguageTag("ru")).apply { groupingSeparator = ' ' }
)

/** Плашка с балансом игрока: монетка + число с разделителем тысяч + " ₽". */
@Composable
fun BalanceChip(
    balance: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.wrapContentSize(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🪙", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${thousandsFormat.format(balance)} ₽",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/** Карточка текущей цели накопления с прогресс-баром и процентом. */
@Composable
fun GoalCard(
    goal: Goal?,
    progress: Float,
    modifier: Modifier = Modifier
) {
    if (goal == null) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, GameColors.cardStroke)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🎯", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Цель не выбрана",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val clampedProgress = progress.coerceIn(0f, 1f)
    val percentText = "${(clampedProgress * 100).roundToInt()}%"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GameColors.cardStroke),
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = goal.emoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { clampedProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp),
                                color = GameColors.goalProgress,
                                trackColor = GameColors.goalProgressTrack
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = percentText,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
            }
        }
    }
}

/** Кнопка-таблетка с приглушённым disabled-состоянием. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val containerColor = if (enabled) MaterialTheme.colorScheme.primary else GameColors.disabledContainer
    val contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else GameColors.disabledContent
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .wrapContentSize()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

// ---------- Previews ----------

@Preview(name = "Components — Light", showBackground = true)
@Composable
private fun GameComponentsLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PreviewContent()
        }
    }
}

@Preview(name = "Components — Dark", showBackground = true)
@Composable
private fun GameComponentsDarkPreview() {
    FinGameTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PreviewContent()
        }
    }
}

@Composable
private fun PreviewContent() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameIcon(emoji = "🐱", size = 56.dp)
            RoundIconButton(emoji = "🛍️", contentDescription = "Открыть магазин", onClick = {})
            BalanceChip(balance = 128450)
        }
        GoalCard(
            goal = Goal(itemId = "bike", title = "Велосипед", targetPrice = 15000, emoji = "🚲"),
            progress = 0.64f
        )
        GoalCard(goal = null, progress = 0f)
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            PillButton(text = "Купить", onClick = {})
            PillButton(text = "Недоступно", onClick = {}, enabled = false)
        }
    }
}
