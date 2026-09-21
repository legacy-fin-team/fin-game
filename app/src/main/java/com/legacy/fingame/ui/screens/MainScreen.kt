package com.legacy.fingame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
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
import com.legacy.fingame.game.scene.SceneOffset
import com.legacy.fingame.game.scene.SceneSprite
import com.legacy.fingame.game.scene.SceneViewport
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
import kotlin.math.roundToInt

private val ScreenPadding = 16.dp
private val GoalCardWidth = 208.dp

/**
 * Sizes of the action buttons on a phone; [com.legacy.fingame.ui.components.SpriteButton] enlarges
 * them on tablets, so these stay the compact values.
 */
private val PrimaryActionSize = 80.dp
private val SecondaryActionSize = 64.dp

/** Gap between two buttons standing next to each other. */
private val ActionGap = 8.dp

/**
 * Gap between the parts of the middle column: the badge, the game area under it and the daily
 * bonus button under that.
 */
private val StageGap = 12.dp

/**
 * Fraction of the available width the pet area may occupy on a screen taller than it is wide.
 * The area is square and there is height to spare there, so the width is what decides how big it
 * comes out, and it keeps clear of the edges of the screen instead of running into them.
 */
private const val PetAreaWidthFraction = 0.74f

/**
 * Narrowest the pet area is ever squeezed to. It only ever comes up on a window so small that the
 * corner columns leave next to nothing between them, where nothing can be laid out side by side
 * anyway and a pet that can still be seen beats a sliver of one.
 */
private val PetAreaMinWidth = 120.dp

/**
 * Width each of the two corner columns — the stats with the goal card on one side, the buttons on
 * the other — is assumed to take. On a screen wide enough for the columns to stand beside the pet
 * rather than above and below it, the middle they leave between them is all the badge above the pet
 * may take, so it can never run under what is in the corners.
 */
private val CornerColumnWidth = GoalCardWidth

/**
 * How much of the width everything standing beside the pet on a wide screen takes at either end:
 * the corner columns at the top and, along the bottom, the group of action buttons — the widest of
 * the two, once a tablet has enlarged the buttons.
 *
 * The pet area comes down into the bottom row on a screen like that, where it is given the whole
 * height between the badge and the bonus button, so it has to keep out of both.
 *
 * @return The width to leave free at either side of the pet area.
 */
@Composable
@ReadOnlyComposable
private fun sideOfStageWidth(): Dp = maxOf(
    CornerColumnWidth,
    GameDimens.buttonSize(PrimaryActionSize) * 3 + ActionGap * 2
)

/**
 * How big one pixel of the game art is drawn in the game area to begin with. The scene is blown up
 * to this much per pixel of the artwork whenever the area is too small to show the whole room at
 * that size — which on a phone it is, so the room comes out larger than the window and the player
 * drags it around (see [SceneViewport.of]).
 */
private val ScenePixelSize = 3.dp

/** How round the corners of the game area are. */
private val SceneCornerRadius = 32.dp

/**
 * Saves how far the player has dragged the scene, so the room stays where it was put over a
 * recomposition or a turn of the device. Written as the two plain numbers a `Bundle` can hold.
 */
internal val SceneOffsetSaver: Saver<SceneOffset, Any> = listSaver(
    save = { offset -> listOf(offset.x, offset.y) },
    restore = { moved ->
        if (moved.size == 2) SceneOffset(x = moved[0], y = moved[1]) else null
    }
)

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
 * - Center: one column with the badge naming the pet and the sub-location, the [PetStage] under it
 *   and the daily bonus button under that while the bonus is unclaimed. The column is measured
 *   rather than guessed at: the badge and the button take the height they need and the game area is
 *   handed every last bit of what is left between them, so a screen held sideways shows a pet as
 *   large as an upright one does instead of a needlessly small one. Across, the area takes its
 *   share of the width ([PetAreaWidthFraction]) and, on a screen wide enough for everything else to
 *   stand beside it, no more than the middle that leaves ([sideOfStageWidth]); the badge is held to
 *   the middle the top corners leave ([CornerColumnWidth]) instead, which is wider, so the name of
 *   the place is readable on a screen where the area itself has to be small.
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
        val stageMaxWidth: Dp
        val stageBadgeMaxWidth: Dp
        if (maxHeight >= maxWidth) {
            // A screen taller than it is wide keeps the corner columns above and below the middle
            // of it rather than beside it, so nothing there has to make room for them.
            stageMaxWidth = maxWidth * PetAreaWidthFraction
            stageBadgeMaxWidth = stageMaxWidth
        } else {
            // A wide one stands them right next to the pet, and what they leave between them is
            // all the middle of the screen there is. The badge only ever reaches the top corners;
            // the area itself comes down into the bottom row as well (see [sideOfStageWidth]).
            stageMaxWidth = minOf(
                maxWidth * PetAreaWidthFraction,
                maxOf(maxWidth - sideOfStageWidth() * 2, PetAreaMinWidth)
            )
            stageBadgeMaxWidth = maxOf(maxWidth - CornerColumnWidth * 2, PetAreaMinWidth)
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val title = stageTitleOf(
                petName = state.petName,
                subLocationTitle = subLocationTitles.getOrNull(state.subLocationIndex)
            )
            if (title != null) {
                StageBadge(
                    title = title,
                    modifier = Modifier.widthIn(max = stageBadgeMaxWidth)
                )
                Spacer(modifier = Modifier.height(StageGap))
            }

            // The area is given the height the badge and the button leave and takes as much of it
            // as its width allows: whatever it does not need is not held back from anything else,
            // since the column is only as tall as what is in it.
            PetStage(
                scene = scene,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = stageMaxWidth)
            )

            if (state.dailyBonusAvailable) {
                Spacer(modifier = Modifier.height(StageGap))
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
                horizontalArrangement = Arrangement.spacedBy(ActionGap),
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
                horizontalArrangement = Arrangement.spacedBy(ActionGap),
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
 * The game area: the square card the pet lives in, and the window onto the scene it is.
 *
 * The card is as large as the room it is given — the caller hands it the width and the height the
 * area may take and it squares that off — with one exception: when the whole scene fits into that
 * much, the card shrinks to exactly the scene. The scene is pixel art blown up by a whole number of
 * screen pixels ([SceneViewport.scale]), so it almost never comes out at the very size of the area,
 * and a card kept at the full size would show a band of its own surface around the room. Shrinking
 * it instead means the room fills the card to the last pixel on every screen, rounded corners and
 * all (see [SceneViewport.windowSide]).
 *
 * When the scene does not fit, which on a phone it does not, only a part of it is visible at a time
 * and the player moves the rest into view with a finger, like a map: the whole scene travels
 * together, and the edges of the room stop the drag so no empty band next to it can be pulled into
 * view.
 *
 * How far the scene is dragged outlives a recomposition and a turn of the device, and an area that
 * changed size holds it to its new edges. It is read in the layout pass and not during composition
 * ([sceneIn]), so a finger moving the room around never composes the sprites it is stacked out of
 * again.
 *
 * @param scene what stands on each layer of the area.
 * @param modifier modifier applied to the card; this is where the caller states how much room the
 *   area has, e.g. as a share of the screen or as the height left over in a column.
 */
@Composable
private fun PetStage(
    scene: GameScene,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val viewport = remember(constraints, density) {
            // An area told it may be as large as it likes is an area with nothing to measure
            // against, and it gets the smallest window there is rather than an endless one.
            val roomWide = if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else 0f
            val roomHigh = if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() else 0f
            SceneViewport.of(
                availableWidth = roomWide,
                availableHeight = roomHigh,
                scenePixels = GameLayer.BACKGROUND.spritePixels,
                pixelSize = with(density) { ScenePixelSize.toPx() }
            )
        }
        var moved by rememberSaveable(stateSaver = SceneOffsetSaver) {
            mutableStateOf(viewport.initialOffset)
        }

        // An area that was resized — a turn of the device, a folded screen — may have been left
        // showing the scene further out than its new edges allow, so the drag is held to them
        // again.
        LaunchedEffect(viewport) { moved = viewport.clamp(moved) }

        Surface(
            modifier = Modifier.size(with(density) { viewport.windowSide.toDp() }),
            shape = RoundedCornerShape(SceneCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GameColors.cardStroke),
            shadowElevation = 3.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(viewport) {
                        if (!viewport.isDraggable) return@pointerInput
                        detectDragGestures { change, dragged ->
                            change.consume()
                            moved = viewport.clamp(
                                moved + SceneOffset(x = dragged.x, y = dragged.y)
                            )
                        }
                    }
            ) {
                SceneLayers(scene = scene, viewport = viewport, moved = { moved })
            }
        }
    }
}

/**
 * The scene itself: the sprites of the room, the pet and everything it wears, stacked into one
 * picture as large as the viewport says the scene is and moved to where the player put it.
 *
 * The stack is built out of the five [GameLayer]s: the scenery of the sub-location, whatever stands
 * behind the pet, the pet itself, whatever stands in front of it and the clothes it wears. The
 * layers are drawn in [GameLayer.DRAW_ORDER] and each of them also carries its [GameLayer.zIndex],
 * so what covers what is decided by the layer and not by the order the sprites happen to be
 * composed in. Inside the scene every layer takes its [GameLayer.sizeFraction] of it — a quarter of
 * the scene for the pet, which is painted at a quarter of the resolution of the room — so a pixel
 * of the one is exactly as big on the screen as a pixel of the other.
 *
 * A sprite whose file is not in the assets yet is not drawn here at all: [SpriteLoader] would hand
 * back the same placeholder for every one of them, and the scene would stack a pile of them on top
 * of each other — the scenery, the pet and everything it wears, all at once. Instead the scene
 * draws what it has and adds a single placeholder over it, so the missing art is still plain to see
 * without the pile.
 *
 * @param scene what stands on each layer.
 * @param viewport geometry of the window and the scene behind it.
 * @param moved how far the scene is dragged, read in the layout pass.
 * @param modifier modifier applied to the stack.
 */
@Composable
private fun SceneLayers(
    scene: GameScene,
    viewport: SceneViewport,
    moved: () -> SceneOffset,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loader = remember(context) { SpriteLoader(context) }
    val (drawn, anythingMissing) = remember(scene, loader) {
        val all = GameLayer.DRAW_ORDER.flatMap { layer -> scene[layer].map { layer to it } }
        val present = all.filter { (_, sprite) -> loader.hasSprite(sprite.assetPath) }
        present to (present.size < all.size)
    }

    Box(
        modifier = modifier.sceneIn(viewport = viewport, moved = moved),
        contentAlignment = Alignment.Center
    ) {
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

/**
 * Lays what this modifier is applied to out as the scene behind a window: as large as the viewport
 * says the scene is, however small the window showing it happens to be, and moved to where the
 * player has dragged it.
 *
 * The scene is laid out at its own size but takes up the window, and no more, in the layout it is
 * a part of: a room larger than its card is the point of the game area, not something the card
 * around it should grow for. Whatever sticks out is left to the window to cut off.
 *
 * @param viewport geometry of the window and the scene behind it.
 * @param moved read at placement time — in the layout pass rather than during composition — so
 *   dragging the scene moves it without composing the sprites it is stacked out of again.
 * @return This modifier with the scene laid out that way.
 */
private fun Modifier.sceneIn(viewport: SceneViewport, moved: () -> SceneOffset): Modifier =
    layout { measurable, constraints ->
        val side = viewport.sceneSide.roundToInt()
        val placeable = measurable.measure(Constraints.fixed(width = side, height = side))
        // A game area always has its size given to it; the scene's own is the fallback for a
        // measurement that leaves an axis open, where there is no window to speak of.
        val windowWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else side
        val windowHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else side

        layout(windowWidth, windowHeight) {
            val offset = viewport.clamp(moved())
            placeable.place(
                x = ((windowWidth - side) / 2f + offset.x).roundToInt(),
                y = ((windowHeight - side) / 2f + offset.y).roundToInt()
            )
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

/**
 * Preview of [MainScreen] in the light theme, landscape orientation: the case the badge above the
 * pet has to survive, with both a name and a sub-location to fit into a screen that has no height —
 * and the case the game area has to fill, with every bit of that height between the badge and the
 * bonus button its own.
 */
@Preview(name = "MainScreen — Landscape", showBackground = true, widthDp = 891, heightDp = 411)
@Composable
private fun MainScreenLandscapePreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = GameUiState(
                    balance = 250,
                    dailyBonusAvailable = true,
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

/**
 * Preview of [MainScreen] on a tablet held sideways, where the buttons are drawn enlarged and the
 * whole room fits into the game area: the card is exactly the scene, with none of its own surface
 * showing around it.
 */
@Preview(name = "MainScreen — Tablet", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun MainScreenTabletPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = GameUiState(
                    balance = 250,
                    dailyBonusAvailable = true,
                    subLocationIndex = 1,
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

/** Preview of [MainScreen] on a tablet held upright, where the width is what the area is cut to. */
@Preview(
    name = "MainScreen — Tablet upright",
    showBackground = true,
    widthDp = 800,
    heightDp = 1280
)
@Composable
private fun MainScreenTabletPortraitPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = GameUiState(
                    balance = 250,
                    dailyBonusAvailable = true,
                    subLocationIndex = 1,
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

/**
 * Preview of the game area at the size a phone gives it: the scene is larger than the card, so the
 * room is shown cut off at its edges and can be dragged around.
 */
@Preview(name = "PetStage — Phone", showBackground = true, widthDp = 312, heightDp = 312)
@Composable
private fun PetStagePhonePreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PetStage(
                scene = DemoScene,
                modifier = Modifier
                    .padding(ScreenPadding)
                    .size(280.dp)
            )
        }
    }
}

/**
 * Preview of the game area at the size a tablet gives it: the whole scene fits, so the card shrinks
 * to exactly the room and nothing of the card itself is left to see around it.
 */
@Preview(name = "PetStage — Tablet", showBackground = true, widthDp = 492, heightDp = 492)
@Composable
private fun PetStageTabletPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PetStage(
                scene = DemoScene,
                modifier = Modifier
                    .padding(ScreenPadding)
                    .size(460.dp)
            )
        }
    }
}
