package com.legacy.fingame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.domain.Goal
import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.ui.components.BalanceChip
import com.legacy.fingame.ui.components.GameIcon
import com.legacy.fingame.ui.components.GoalCard
import com.legacy.fingame.ui.components.RoundIconButton
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors

private val ScreenPadding = 16.dp
private val GoalCardWidth = 208.dp
private val PrimaryActionSize = 60.dp
private val SecondaryActionSize = 52.dp
private const val PetAreaWidthFraction = 0.74f

/**
 * Главный экран игры: «сцена» текущей подлокации с аватаром питомца по центру
 * и игровыми кнопками, разнесёнными по углам (см. эскиз заказчика).
 */
@Composable
fun MainScreen(
    state: GameUiState,
    onOpenShop: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLocations: () -> Unit,
    onPrevSubLocation: () -> Unit,
    onNextSubLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scene = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            MaterialTheme.colorScheme.background
        )
    )
    val subLocationTitle = state.subLocations.getOrNull(state.subLocationIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(scene)
            .systemBarsPadding()
            .padding(ScreenPadding)
    ) {
        // --- Центр: подлокация + аватар питомца ---
        PetStage(
            petEmoji = state.petEmoji,
            petName = state.petName,
            subLocationTitle = subLocationTitle,
            modifier = Modifier.align(Alignment.Center)
        )

        // --- Левый верхний угол: баланс + цель ---
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BalanceChip(balance = state.balance)
            GoalCard(
                goal = state.goal,
                progress = state.goalProgress,
                modifier = Modifier.widthIn(max = GoalCardWidth)
            )
        }

        // --- Правый верхний угол: настройки + локации ---
        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            RoundIconButton(
                emoji = "⚙️",
                contentDescription = "Открыть настройки",
                onClick = onOpenSettings,
                size = SecondaryActionSize
            )
            RoundIconButton(
                emoji = "🗺️",
                contentDescription = "Открыть локации",
                onClick = onOpenLocations,
                size = SecondaryActionSize
            )
        }

        // --- Левый нижний угол: переключение подлокаций ---
        Row(
            modifier = Modifier.align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(
                emoji = "⬅️",
                contentDescription = "Предыдущая подлокация",
                onClick = onPrevSubLocation,
                size = SecondaryActionSize
            )
            RoundIconButton(
                emoji = "➡️",
                contentDescription = "Следующая подлокация",
                onClick = onNextSubLocation,
                size = SecondaryActionSize
            )
        }

        // --- Правый нижний угол: основные действия ---
        Row(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(
                emoji = "📜",
                contentDescription = "Открыть квесты",
                onClick = onOpenQuests,
                size = PrimaryActionSize
            )
            RoundIconButton(
                emoji = "🎒",
                contentDescription = "Открыть инвентарь",
                onClick = onOpenInventory,
                size = PrimaryActionSize
            )
            RoundIconButton(
                emoji = "🛒",
                contentDescription = "Открыть магазин",
                onClick = onOpenShop,
                size = PrimaryActionSize
            )
        }
    }
}

/** Квадратная «сцена» с питомцем: плашка подлокации сверху, имя питомца снизу. */
@Composable
private fun PetStage(
    petEmoji: String,
    petName: String,
    subLocationTitle: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(PetAreaWidthFraction),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (subLocationTitle != null) {
            SubLocationBadge(title = subLocationTitle)
            Spacer(modifier = Modifier.height(12.dp))
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            val petSize = maxWidth * 0.66f
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, GameColors.cardStroke),
                shadowElevation = 3.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        GameIcon(emoji = petEmoji, size = petSize)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = petName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** Небольшая плашка с названием текущей подлокации. */
@Composable
private fun SubLocationBadge(
    title: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------- Previews ----------

private val previewState = GameUiState(
    balance = 12450,
    goal = Goal(itemId = "bike", title = "Велосипед", targetPrice = 15000, emoji = "🚲"),
    goalProgress = 0.83f,
    subLocationIndex = 1,
    subLocations = listOf("Комната", "Кухня", "Двор", "Балкон"),
    petName = "Барсик",
    petEmoji = "🐱"
)

@Preview(name = "MainScreen — Light", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun MainScreenLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = previewState,
                onOpenShop = {},
                onOpenInventory = {},
                onOpenQuests = {},
                onOpenSettings = {},
                onOpenLocations = {},
                onPrevSubLocation = {},
                onNextSubLocation = {}
            )
        }
    }
}

@Preview(name = "MainScreen — Dark", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun MainScreenDarkPreview() {
    FinGameTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = previewState,
                onOpenShop = {},
                onOpenInventory = {},
                onOpenQuests = {},
                onOpenSettings = {},
                onOpenLocations = {},
                onPrevSubLocation = {},
                onNextSubLocation = {}
            )
        }
    }
}
