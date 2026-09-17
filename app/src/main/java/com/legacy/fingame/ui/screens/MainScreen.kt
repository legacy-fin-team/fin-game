package com.legacy.fingame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.Screen
import com.legacy.fingame.ui.DemoContent
import com.legacy.fingame.ui.components.BalanceChip
import com.legacy.fingame.ui.components.GoalCard
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors

private val ScreenPadding = 16.dp
private val GoalCardWidth = 208.dp
private val PrimaryActionSize = 64.dp
private val SecondaryActionSize = 56.dp
private const val PetAreaWidthFraction = 0.74f

// Demo-only progress value for the goal card: no goal data exists in the UI layer.
private const val DemoGoalProgress = 0.4f

@Composable
fun MainScreen(
    state: GameUiState,
    onOpenScreen: (Screen) -> Unit,
    onPrevSubLocation: () -> Unit,
    onNextSubLocation: () -> Unit,
    petId: String = DemoContent.petId,
    subLocationTitles: List<String> = DemoContent.subLocationTitles,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(ScreenPadding)
    ) {
        PetStage(
            petId = petId,
            subLocationTitle = subLocationTitles.getOrNull(state.subLocationIndex),
            modifier = Modifier.align(Alignment.Center)
        )

        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BalanceChip()
            GoalCard(
                progress = DemoGoalProgress,
                modifier = Modifier.widthIn(max = GoalCardWidth)
            )
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            SpriteButton(
                assetPath = Sprites.SETTINGS,
                contentDescription = "Открыть настройки",
                onClick = { onOpenScreen(Screen.OPTIONS) },
                size = SecondaryActionSize
            )
            SpriteButton(
                assetPath = Sprites.LOCATIONS,
                contentDescription = "Открыть локации",
                onClick = { onOpenScreen(Screen.LOCATIONS) },
                size = SecondaryActionSize
            )
        }

        Row(
            modifier = Modifier.align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpriteButton(
                assetPath = Sprites.ARROW_LEFT,
                contentDescription = "Предыдущая подлокация",
                onClick = onPrevSubLocation,
                size = SecondaryActionSize
            )
            SpriteButton(
                assetPath = Sprites.ARROW_RIGHT,
                contentDescription = "Следующая подлокация",
                onClick = onNextSubLocation,
                size = SecondaryActionSize
            )
        }

        Row(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpriteButton(
                assetPath = Sprites.QUESTS,
                contentDescription = "Открыть квесты",
                onClick = { onOpenScreen(Screen.QUESTS) },
                size = PrimaryActionSize
            )
            SpriteButton(
                assetPath = Sprites.INVENTORY,
                contentDescription = "Открыть инвентарь",
                onClick = { onOpenScreen(Screen.INVENTORY) },
                size = PrimaryActionSize
            )
            SpriteButton(
                assetPath = Sprites.SHOP,
                contentDescription = "Открыть магазин",
                onClick = { onOpenScreen(Screen.SHOP) },
                size = PrimaryActionSize
            )
        }
    }
}

/** Square pet card with the current sub-location badge above it. */
@Composable
private fun PetStage(
    petId: String,
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

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GameColors.cardStroke),
            shadowElevation = 3.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Sprite(
                    assetPath = Sprites.pet(petId),
                    contentDescription = "Питомец",
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f)
                )
            }
        }
    }
}

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

@Preview(name = "MainScreen — Light", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun MainScreenLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = GameUiState(subLocationIndex = 1),
                onOpenScreen = {},
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
                state = GameUiState(subLocationIndex = 1),
                onOpenScreen = {},
                onPrevSubLocation = {},
                onNextSubLocation = {}
            )
        }
    }
}
