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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.legacy.fingame.DemoMode
import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.Screen
import com.legacy.fingame.game.economy.Economy
import com.legacy.fingame.game.items.ItemCatalog
import com.legacy.fingame.game.scene.GameLayer
import com.legacy.fingame.game.scene.GameScene
import com.legacy.fingame.game.scene.SceneSprite
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import com.legacy.fingame.ui.DemoContent
import com.legacy.fingame.ui.components.BalanceChip
import com.legacy.fingame.ui.components.GoalCard
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.components.StatChip
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens
import com.legacy.fingame.utils.SpriteLoader

private val ScreenPadding = 16.dp
private val GoalCardWidth = 208.dp

/**
 * Sizes of the action buttons on a phone; [com.legacy.fingame.ui.components.SpriteButton] enlarges
 * them on tablets, so these stay the compact values.
 */
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

/**
 * Fraction of the available height the pet area may occupy on a short screen — a phone turned on
 * its side. Smaller than [PetAreaHeightFraction] so the badge above the pet, which may need a
 * second line there to say the name in full, and the daily bonus button under it still fit between
 * the pet and the edges of the screen.
 */
private const val ShortScreenPetAreaHeightFraction = 0.42f

/**
 * Width each of the two corner columns — the stats with the goal card on one side, the buttons on
 * the other — is assumed to take. The badge above the pet is kept out of that much room on either
 * side, so a badge wider than the pet card still cannot run under what is in the corners.
 */
private val CornerColumnWidth = GoalCardWidth

// TODO: DemoGoalProgress is a hardcoded placeholder for the goal card progress bar. Replace with the real progress value once goal data is exposed from app logic.
private const val DemoGoalProgress = 0.4f

/**
 * Stats shown in a row of their own under the balance: every stat the pet has except
 * [StatKind.HEALTH], which sits next to the money instead.
 */
private val SecondaryStats: List<StatKind> = StatKind.entries.filter { it != StatKind.HEALTH }

/** Separator between the pet's name and the sub-location it is in, in the badge above the pet. */
private const val StageTitleSeparator = " · "

/**
 * How many lines the badge above the pet may take. The separator of [stageTitleOf] carries spaces
 * on both sides, so a badge too narrow for the whole title breaks it between the pet's name and the
 * sub-location instead of cutting the name of the place off.
 */
private const val StageBadgeMaxLines = 2

/**
 * Game area the previews and the default of [MainScreen] show: the demo content pet standing in the
 * scenery of the first sub-location, with nothing on.
 * TODO: replace with the real scene once every caller builds one from the player's own pet.
 */
private val DemoScene: GameScene = GameScene.of(
    background = SceneSprite(assetPath = Sprites.locationBackground(0), description = null),
    pet = SceneSprite(
        assetPath = Sprites.pet(petId = DemoContent.petId, variantId = DemoContent.petVariantId),
        description = "Питомец"
    ),
    animalId = DemoContent.petId,
    worn = emptySet(),
    catalog = ItemCatalog.EMPTY
)

/**
 * Main game screen: shows the pet, current balance/goal progress, and navigation entry
 * points to the other screens.
 *
 * Layout:
 * - Center: [PetStage] with the pet sprite and the badge naming the pet and the sub-location above
 *   it, and the daily bonus button right under the pet while the bonus is unclaimed. The pet area
 *   is sized from both the available width and height ([PetAreaWidthFraction],
 *   [PetAreaHeightFraction], and [ShortScreenPetAreaHeightFraction] on a screen held sideways) so
 *   it cannot grow past the screen in landscape and cover the corner buttons. The badge is not
 *   held to the width of the card: it may spread over the whole middle of the screen, between the
 *   corner columns ([CornerColumnWidth]), so the name of the place is readable on a screen where
 *   the card itself has to be small.
 * - Top-start: the balance chip with the pet's health next to it, the rest of the stats
 *   ([SecondaryStats]) in a row under them, and the goal progress card. Every stat is a
 *   [StatChip] — an icon and a percentage — so the stats take a corner instead of half the screen.
 * - Top-end: secondary buttons for opening settings and locations, and — in a demo build only — the
 *   button that skips [DemoMode.FAST_FORWARD_HOURS] hours of the pet's life.
 * - Bottom: a single row split into two groups — sub-location navigation arrows (start) and
 *   primary action buttons for quests/inventory/shop (end). Both groups are combined into one
 *   row so the enlarged action buttons cannot overlap each other on narrow screens.
 *
 * @param state current game state; [GameUiState.petName] and [GameUiState.subLocationIndex] make up
 *   the badge above the pet, [GameUiState.balance] fills the balance chip,
 *   [GameUiState.dailyBonusAvailable] decides whether the bonus button is there at all and
 *   [GameUiState.stats] fills the stat chips.
 * @param onOpenScreen called with the [Screen] that should be opened when a navigation button
 *   (settings, locations, quests, inventory, shop) is pressed.
 * @param onPrevSubLocation called when the "previous sub-location" arrow is pressed.
 * @param onNextSubLocation called when the "next sub-location" arrow is pressed.
 * @param onClaimDailyBonus called when the player takes the daily bonus.
 * @param modifier modifier applied to the screen root.
 * @param onFastForward called when the demo's time button is pressed, or `null` — the default — when
 *   there is to be no such button at all, which is every build that is not a demo one (see
 *   [DemoMode.ENABLED]). A screen that gets `null` here is the screen the player sees.
 * @param scene what stands in the game area, already sorted into its layers; the caller builds it
 *   from the pet, its age stage and what it wears (see [GameScene.of]), so this screen doesn't have
 *   to know how the assets are laid out. Defaults to the demo content pet alone.
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
    onFastForward: (() -> Unit)? = null,
    // TODO: default pulls from demo content; replace with the real scene of the player's pet.
    scene: GameScene = DemoScene,
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
        val heightFraction = if (GameDimens.isShortScreen) {
            ShortScreenPetAreaHeightFraction
        } else {
            PetAreaHeightFraction
        }
        val petAreaSize = minOf(
            maxWidth * PetAreaWidthFraction,
            maxHeight * heightFraction
        )
        // The pet card is sized by the height on a short screen and ends up narrow; the badge over
        // it is not, so it takes the whole middle of the screen when it has a long name to say.
        val stageBadgeMaxWidth = maxOf(petAreaSize, maxWidth - CornerColumnWidth * 2)

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PetStage(
                scene = scene,
                title = stageTitleOf(
                    petName = state.petName,
                    subLocationTitle = subLocationTitles.getOrNull(state.subLocationIndex)
                ),
                areaSize = petAreaSize,
                badgeMaxWidth = stageBadgeMaxWidth
            )

            if (state.dailyBonusAvailable) {
                Spacer(modifier = Modifier.height(12.dp))
                PillButton(
                    text = "Бонус дня +${Economy.DAILY_BONUS}",
                    onClick = onClaimDailyBonus
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BalanceChip(balance = state.balance)
                StatChip(stat = StatKind.HEALTH, stats = state.stats)
            }

            if (SecondaryStats.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryStats.forEach { stat ->
                        StatChip(stat = stat, stats = state.stats)
                    }
                }
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

            if (onFastForward != null) {
                PillButton(
                    text = "Вперёд ${DemoMode.FAST_FORWARD_HOURS} ч",
                    onClick = onFastForward
                )
            }
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
 * Builds the text of the badge above the pet out of what there is to say about it.
 *
 * @param petName name the player gave the pet, or an empty string when it has none.
 * @param subLocationTitle title of the sub-location the pet is in, or `null` when there is none.
 * @return The pet's name and the sub-location, whichever of them there is, or `null` when the badge
 * would say nothing at all and is better not shown.
 */
private fun stageTitleOf(petName: String, subLocationTitle: String?): String? = listOfNotNull(
    petName.takeIf { it.isNotBlank() },
    subLocationTitle?.takeIf { it.isNotBlank() }
).joinToString(StageTitleSeparator).takeIf { it.isNotEmpty() }

/**
 * Square game area with the badge naming the pet and its sub-location shown above it.
 *
 * The area is where the pet lives, and it is stacked out of the five [GameLayer]s: the scenery of the
 * sub-location, whatever stands behind the pet, the pet itself, whatever stands in front of it and
 * the clothes it wears. The layers are drawn in [GameLayer.DRAW_ORDER] and each of them also carries
 * its [GameLayer.zIndex], so what covers what is decided by the layer and not by the order the
 * sprites happen to be composed in.
 *
 * The scenery is stretched over the whole area and everything standing in it is drawn at its
 * layer's [GameLayer.sizeFraction] of that — a quarter of the area for the pet, which is painted at
 * a quarter of the resolution of the room. That way the room is stretched four times as much as the
 * pet is, and a pixel of the one ends up exactly as big on the screen as a pixel of the other.
 *
 * A sprite whose file is not in the assets yet is not drawn here at all: [SpriteLoader] would hand
 * back the same placeholder for every one of them, and the area would stack a pile of them on top of
 * each other — the scenery, the pet and everything it wears, all at once. Instead the area draws
 * what it has and adds a single placeholder over it, so the missing art is still plain to see
 * without the pile.
 *
 * @param scene what stands on each layer of the area.
 * @param title text of the badge above the pet, or `null` to hide the badge.
 * @param areaSize side length of the square pet card; the caller computes it from both the
 *   available width and height so the pet cannot grow past the screen in landscape.
 * @param badgeMaxWidth how wide the badge above the card may grow. It is not tied to [areaSize]:
 *   a card sized by the height of a screen held sideways is narrow, and a name cut down to that
 *   width would say nothing, so the badge gets the middle of the screen instead.
 * @param modifier modifier applied to the root column.
 */
@Composable
private fun PetStage(
    scene: GameScene,
    title: String?,
    areaSize: Dp,
    badgeMaxWidth: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loader = remember(context) { SpriteLoader(context) }
    val (drawn, anythingMissing) = remember(scene, loader) {
        val all = GameLayer.DRAW_ORDER.flatMap { layer -> scene[layer].map { layer to it } }
        val present = all.filter { (_, sprite) -> loader.hasSprite(sprite.assetPath) }
        present to (present.size < all.size)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (title != null) {
            StageBadge(
                title = title,
                modifier = Modifier.widthIn(max = badgeMaxWidth)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Surface(
            modifier = Modifier.size(areaSize),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GameColors.cardStroke),
            shadowElevation = 3.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                drawn.forEach { (layer, sprite) ->
                    Sprite(
                        assetPath = sprite.assetPath,
                        contentDescription = sprite.description,
                        modifier = Modifier
                            .zIndex(layer.zIndex)
                            .fillMaxSize(layer.sizeFraction)
                            .aspectRatio(1f)
                    )
                }

                if (anythingMissing) {
                    Sprite(
                        assetPath = SpriteLoader.MISSING_SPRITE,
                        contentDescription = "Часть картинок ещё не нарисована",
                        modifier = Modifier
                            .zIndex(GameLayer.CLOTHES.zIndex)
                            .fillMaxSize(GameLayer.ANIMAL.sizeFraction)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
}

/**
 * Pill-shaped badge naming the pet and the sub-location it is in, displayed above the pet.
 *
 * The badge is only as wide as its text, up to whatever the caller allows it; a name that still
 * does not fit on one line is wrapped onto a second one ([StageBadgeMaxLines]) rather than cut
 * short, since a sub-location the player cannot read the name of is the same as an unnamed one.
 *
 * @param title text to display inside the badge, as built by [stageTitleOf].
 * @param modifier modifier applied to the badge surface; this is where the caller limits how wide
 *   the badge may grow.
 */
@Composable
private fun StageBadge(
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
            textAlign = TextAlign.Center,
            maxLines = StageBadgeMaxLines,
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
                    subLocationIndex = 1,
                    petName = "Барсик",
                    stats = PetStats(
                        mapOf(
                            StatKind.HEALTH to 90,
                            StatKind.HUNGER to 45,
                            StatKind.PLEASURE to 70
                        )
                    )
                ),
                onOpenScreen = {},
                onPrevSubLocation = {},
                onNextSubLocation = {},
                onClaimDailyBonus = {}
            )
        }
    }
}

/** Preview of [MainScreen] as a demo build shows it: with the time button in the top corner. */
@Preview(name = "MainScreen — Demo", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun MainScreenDemoPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = GameUiState(
                    balance = 250,
                    subLocationIndex = 1,
                    petName = "Барсик"
                ),
                onOpenScreen = {},
                onPrevSubLocation = {},
                onNextSubLocation = {},
                onClaimDailyBonus = {},
                onFastForward = {}
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
                state = GameUiState(
                    balance = 250,
                    subLocationIndex = 1,
                    petName = "Барсик",
                    // A pet that has been left alone for a while: the hunger chip warns about it.
                    stats = PetStats(
                        mapOf(
                            StatKind.HEALTH to 60,
                            StatKind.HUNGER to 10,
                            StatKind.PLEASURE to 35
                        )
                    )
                ),
                onOpenScreen = {},
                onPrevSubLocation = {},
                onNextSubLocation = {},
                onClaimDailyBonus = {}
            )
        }
    }
}

/** Preview of [MainScreen] on a tablet, where the buttons are drawn enlarged. */
@Preview(name = "MainScreen — Tablet", showBackground = true, device = Devices.TABLET)
@Composable
private fun MainScreenTabletPreview() {
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

/**
 * Preview of [MainScreen] in the light theme, landscape orientation: the case the badge above the
 * pet has to survive, with both a name and a sub-location to fit into a screen that has no height.
 */
@Preview(name = "MainScreen — Landscape", showBackground = true, widthDp = 891, heightDp = 411)
@Composable
private fun MainScreenLandscapePreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = GameUiState(
                    balance = 250,
                    subLocationIndex = 2,
                    petName = "Барсик"
                ),
                onOpenScreen = {},
                onPrevSubLocation = {},
                onNextSubLocation = {},
                onClaimDailyBonus = {}
            )
        }
    }
}
