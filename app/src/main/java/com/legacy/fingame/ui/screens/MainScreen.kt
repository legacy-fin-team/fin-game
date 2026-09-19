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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.Screen
import com.legacy.fingame.game.economy.Economy
import com.legacy.fingame.ui.DemoContent
import com.legacy.fingame.ui.components.BalanceChip
import com.legacy.fingame.ui.components.GoalCard
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors

private val ScreenPadding = 16.dp
private val GoalCardWidth = 208.dp
private val PrimaryActionSize = 80.dp
private val SecondaryActionSize = 64.dp

/**
 * Fraction of the available width the pet area may occupy. Combined with
 * [PetAreaHeightFraction] to keep the pet card from growing past the screen in landscape
 * and covering the corner buttons.
 */
private const val PetAreaWidthFraction = 0.74f

/**
 * Fraction of the available height the pet area may occupy. See [PetAreaWidthFraction].
 */
private const val PetAreaHeightFraction = 0.52f

// TODO: DemoGoalProgress is a hardcoded placeholder for the goal card progress bar. Replace with the real progress value once goal data is exposed from app logic.
private const val DemoGoalProgress = 0.4f

/**
 * Main game screen: shows the pet, current balance/goal progress, and navigation entry
 * points to the other screens.
 *
 * Layout:
 * - Center: [PetStage] with the pet sprite and the current sub-location badge above it. The
 *   pet area is sized from both the available width and height ([PetAreaWidthFraction],
 *   [PetAreaHeightFraction]) so it cannot grow past the screen in landscape and cover the
 *   corner buttons.
 * - Top-start: balance chip, the daily bonus button while the bonus is unclaimed, and the goal
 *   progress card.
 * - Top-end: secondary buttons for opening settings and locations.
 * - Bottom: a single row split into two groups — sub-location navigation arrows (start) and
 *   primary action buttons for quests/inventory/shop (end). Both groups are combined into one
 *   row so the enlarged action buttons cannot overlap each other on narrow screens.
 *
 * @param state current game state; [GameUiState.subLocationIndex] selects which title from
 *   [subLocationTitles] is shown above the pet, [GameUiState.balance] fills the balance chip and
 *   [GameUiState.dailyBonusAvailable] decides whether the bonus button is there at all.
 * @param onOpenScreen called with the [Screen] that should be opened when a navigation button
 *   (settings, locations, quests, inventory, shop) is pressed.
 * @param onPrevSubLocation called when the "previous sub-location" arrow is pressed.
 * @param onNextSubLocation called when the "next sub-location" arrow is pressed.
 * @param onClaimDailyBonus called when the player takes the daily bonus.
 * @param modifier modifier applied to the screen root.
 * @param petSpritePath path (relative to `assets/textures/`) to the sprite of the player's pet;
 *   the caller resolves it from the animal the player picked and its current age stage, so this
 *   screen doesn't have to know how animal assets are laid out. Defaults to the demo content pet.
 * @param subLocationTitles titles for each sub-location, indexed by
 *   [GameUiState.subLocationIndex]; defaults to the demo content titles.
 */
@Composable
fun MainScreen(
    state: GameUiState,
    onOpenScreen: (Screen) -> Unit,
    onPrevSubLocation: () -> Unit,
    onNextSubLocation: () -> Unit,
    onClaimDailyBonus: () -> Unit,
    modifier: Modifier = Modifier,
    // TODO: default pulls from demo content; replace with the sprite of the pet at its real age stage.
    petSpritePath: String = Sprites.pet(
        petId = DemoContent.petId,
        variantId = DemoContent.petVariantId,
        age = DemoContent.petAge
    ),
    // TODO: default pulls from demo content; replace with real sub-location names for the current location.
    subLocationTitles: List<String> = DemoContent.subLocationTitles
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(ScreenPadding)
    ) {
        val petAreaSize = minOf(
            maxWidth * PetAreaWidthFraction,
            maxHeight * PetAreaHeightFraction
        )

        PetStage(
            petSpritePath = petSpritePath,
            subLocationTitle = subLocationTitles.getOrNull(state.subLocationIndex),
            areaSize = petAreaSize,
            modifier = Modifier.align(Alignment.Center)
        )

        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BalanceChip(balance = state.balance)
            if (state.dailyBonusAvailable) {
                PillButton(
                    text = "Бонус дня +${Economy.DAILY_BONUS}",
                    onClick = onClaimDailyBonus
                )
            }
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
}

/**
 * Square pet card with the current sub-location badge shown above it.
 *
 * @param petSpritePath path (relative to `assets/textures/`) to the pet's sprite.
 * @param subLocationTitle title of the current sub-location shown in the badge above the pet,
 *   or `null` to hide the badge.
 * @param areaSize side length of the square pet card; the caller computes it from both the
 *   available width and height so the pet cannot grow past the screen in landscape.
 * @param modifier modifier applied to the root column.
 */
@Composable
private fun PetStage(
    petSpritePath: String,
    subLocationTitle: String?,
    areaSize: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(areaSize),
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
                    assetPath = petSpritePath,
                    contentDescription = "Питомец",
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(1f)
                )
            }
        }
    }
}

/**
 * Pill-shaped badge showing the current sub-location name, displayed above the pet.
 *
 * @param title text to display inside the badge.
 * @param modifier modifier applied to the badge surface.
 */
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

/** Preview of [MainScreen] in the light theme, portrait orientation. */
@Preview(name = "MainScreen — Light", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun MainScreenLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = GameUiState(
                    balance = 250,
                    dailyBonusAvailable = true,
                    subLocationIndex = 1
                ),
                onOpenScreen = {},
                onPrevSubLocation = {},
                onNextSubLocation = {},
                onClaimDailyBonus = {}
            )
        }
    }
}

/** Preview of [MainScreen] in the dark theme, portrait orientation. */
@Preview(name = "MainScreen — Dark", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun MainScreenDarkPreview() {
    FinGameTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = GameUiState(balance = 250, subLocationIndex = 1),
                onOpenScreen = {},
                onPrevSubLocation = {},
                onNextSubLocation = {},
                onClaimDailyBonus = {}
            )
        }
    }
}

/** Preview of [MainScreen] in the light theme, landscape orientation. */
@Preview(name = "MainScreen — Landscape", showBackground = true, widthDp = 891, heightDp = 411)
@Composable
private fun MainScreenLandscapePreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = GameUiState(balance = 250, subLocationIndex = 2),
                onOpenScreen = {},
                onPrevSubLocation = {},
                onNextSubLocation = {},
                onClaimDailyBonus = {}
            )
        }
    }
}
