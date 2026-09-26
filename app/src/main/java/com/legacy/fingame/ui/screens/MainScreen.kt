package com.legacy.fingame.ui.screens

import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.legacy.fingame.DemoMode
import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.Screen
import com.legacy.fingame.game.economy.Economy
import com.legacy.fingame.game.items.GoalLine
import com.legacy.fingame.game.items.ItemCatalog
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.scene.GameLayer
import com.legacy.fingame.game.scene.GameScene
import com.legacy.fingame.game.scene.HeartBurst
import com.legacy.fingame.game.scene.PetTouchController
import com.legacy.fingame.game.scene.SceneOffset
import com.legacy.fingame.game.scene.SceneSprite
import com.legacy.fingame.game.scene.SceneViewport
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import com.legacy.fingame.ui.DemoContent
import com.legacy.fingame.ui.components.BalanceChip
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.PillStyle
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.components.StatChip
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens
import com.legacy.fingame.utils.SpriteLoader
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val ScreenPadding = 16.dp

/**
 * Sizes of the action buttons on a phone; [com.legacy.fingame.ui.components.SpriteButton] enlarges
 * them on tablets, so these stay the compact values.
 */
private val PrimaryActionSize = 80.dp
private val SecondaryActionSize = 64.dp

/** Gap between two buttons standing next to each other. */
private val ActionGap = 8.dp

/**
 * Gap between the chips and the goal card stacked in the corner with the player's things. One step
 * of the screen's grid, like every other gap on it, so the corner lines up with the rest.
 */
private val ChipGap = 8.dp

/** Gap between the settings and time buttons. */
private val ControlGap = 12.dp

/**
 * Smallest gap between the two groups of the bottom row — the money buttons and the action buttons.
 * Wider than [ActionGap], which separates buttons of one group, so the two groups still read as two
 * even when the row has been squeezed (see [bottomRowFit]).
 */
private val BottomGroupGap = 16.dp

/**
 * Smallest a button of the bottom row may be squeezed to. Below this a button stops being
 * comfortable to hit, so the row is allowed to run wider instead of shrinking any further — which
 * on any screen this game is meant for it never has to (see [bottomRowFit]).
 */
private val MinTouchTarget = 40.dp

/**
 * Gap left between the blocks standing in the corners of the screen and what is laid out between
 * them.
 */
private val BlockGap = 12.dp

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
 * How big one pixel of the game art is drawn in the game area to begin with. The scene is blown up
 * to this much per pixel of the artwork whenever the area is too small to show the whole room at
 * that size — which on a phone it is, so the room comes out larger than the window and the player
 * drags it around (see [SceneViewport.of]).
 */
private val ScenePixelSize = 3.dp

/**
 * How many times past the size it starts at the player may blow the scene up with a pinch. Twice
 * is enough for the pet to fill a good part of the window without the room turning into a handful
 * of huge squares.
 */
private const val SceneMaxZoom = 2f

/** How round the corners of the game area are. */
private val SceneCornerRadius = 32.dp

/** Depth of the hearts over a patted pet: over every layer of the room, the clothes included. */
private val HeartsZIndex = GameLayer.CLOTHES.zIndex + 1f

/** Side of a heart as a part of the side of the scene: 8 pixels of the art out of 128. */
private val HeartSizeFraction =
    HeartBurst.HEART_PIXELS.toFloat() / GameLayer.BACKGROUND.spritePixels

/**
 * Name of the game area in the semantics tree: what a test takes hold of to drag and pinch the
 * scene, and whose size tells it how big the window came out.
 */
@VisibleForTesting
internal const val PetStageTag = "PetStage"

/**
 * How big a pixel of the artwork is drawn in the game area right now, in screen pixels.
 *
 * The scene is dragged and pinched in the layout pass and never composed again, which is what keeps
 * the gestures smooth and leaves a test nothing to read; this says out loud what the layout is
 * doing. It is a property of nobody's making but ours, so a screen reader passes over it in silence
 * and the game area is announced by the sprites in it, as it was before.
 */
@VisibleForTesting
internal val SceneScaleKey: SemanticsPropertyKey<Float> = SemanticsPropertyKey("SceneScale")
private var SemanticsPropertyReceiver.sceneScale by SceneScaleKey

/**
 * How far the scene is moved to the right inside the window, in screen pixels. Told to a test the
 * same way, and for the same reason, as [SceneScaleKey].
 */
@VisibleForTesting
internal val SceneOffsetXKey: SemanticsPropertyKey<Float> = SemanticsPropertyKey("SceneOffsetX")
private var SemanticsPropertyReceiver.sceneOffsetX by SceneOffsetXKey

/** How far the scene is moved down inside the window, in screen pixels. See [SceneOffsetXKey]. */
@VisibleForTesting
internal val SceneOffsetYKey: SemanticsPropertyKey<Float> = SemanticsPropertyKey("SceneOffsetY")
private var SemanticsPropertyReceiver.sceneOffsetY by SceneOffsetYKey

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

/**
 * Stats shown beside the balance, in the order they are laid out: every stat the pet has, health
 * first, since that is the one the player looks for.
 */
private val PlayerStats: List<StatKind> = StatKind.entries.sortedBy { it != StatKind.HEALTH }

/**
 * How many stat chips stand on one line while there is width for them: all of them, so the pet's
 * state reads as one strip instead of a ragged two-and-one block.
 *
 * A compact stat chip keeps the room of "100%" whatever it says (see [StatChip]): a 16 dp icon, a
 * 4 dp gap, four glyphs of `labelSmall` at 11 sp and 8 dp of padding on either side — `82.dp` at the
 * ordinary font scale and `96.dp` at 1.3. Three of them with the [ActionGap] between come to 262 dp
 * and 304 dp, and the narrowest screen the game is laid out for (360 dp) leaves 328 — room for all
 * three, though not beside the settings button, so there the strip stands under the button instead
 * (see [PlayerCorner]). A screen with less than that wraps the last chip onto a line of its own
 * rather than squeezing them.
 */
private const val ChipsPerRow = 3

/** Separator between the pet's name and the sub-location it is in, in the badge above the pet. */
private const val StageTitleSeparator = DotSeparator

/**
 * How many lines the badge above the pet may take. The separator of [stageTitleOf] carries a space
 * after it — the one before it does not break — so a badge too narrow for the whole title breaks it
 * between the pet's name and the sub-location instead of cutting the name of the place off, and the
 * dot stays at the end of the first line rather than opening the second.
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
 * How much of their full size the buttons along the bottom of the screen may keep.
 *
 * The row holds the same buttons on every screen, and on a narrow one they add up to more than
 * there is width. Rather than let the last of them be squeezed into a dot by what the ones before
 * it took — which is what a plain row does — every button and every gap of the row is multiplied by
 * this one number, so the row shrinks as a whole and the buttons stay the size of one another.
 *
 * @param availableWidth width the row has, in dp.
 * @param neededWidth width the row takes at full size, gaps included, in dp.
 * @param smallestButton size of the smallest button of the row at full size, in dp.
 * @param minTouchTarget smallest that button may end up being, in dp.
 * @return How much of its full size each part of the row keeps: `1` when the row fits as it is, and
 * never so little that the smallest button of it would come out under [minTouchTarget] — a row that
 * would have to shrink further is left sticking out instead, since a button too small to hit is of
 * no use to anyone.
 */
internal fun bottomRowFit(
    availableWidth: Float,
    neededWidth: Float,
    smallestButton: Float,
    minTouchTarget: Float
): Float {
    if (neededWidth <= 0f || availableWidth >= neededWidth) return 1f
    val smallest =
        if (smallestButton > 0f) (minTouchTarget / smallestButton).coerceAtMost(1f) else 1f
    return (availableWidth / neededWidth).coerceIn(smallest, 1f)
}

/**
 * Size of one of the four blocks standing in the corners of the main screen, in screen pixels.
 *
 * @property width how wide the block came out.
 * @property height how tall it came out.
 */
internal data class CornerBlock(val width: Int, val height: Int)

/**
 * Where the demo build's time button stands under the buttons in the top end corner.
 *
 * The button carries a label and comes out several times wider than the sprite buttons it hangs
 * under, so it sticks out towards the middle of the screen — and on a narrow one it reaches all the
 * way across to what the player's things take in the corner opposite. When it does, it steps down
 * past them as well instead of running into them; when it does not, it simply follows the buttons
 * it belongs to.
 *
 * @param width width of the screen, in screen pixels.
 * @param timeButtonWidth width of the button itself, in screen pixels.
 * @param topStart size of the block with the player's things, in the corner opposite.
 * @param topEnd size of the block of buttons the time button hangs under.
 * @param gap how much room to leave between blocks.
 * @return How far down the top of the button goes, in screen pixels from the top of the screen. The
 * button is pinned to the end of the screen across, so this is all there is to say about where it
 * stands.
 */
internal fun timeButtonTopOf(
    width: Int,
    timeButtonWidth: Int,
    topStart: CornerBlock,
    topEnd: CornerBlock,
    gap: Int
): Int {
    val underButtons = topEnd.height + gap
    val reachesAcross = width - timeButtonWidth < topStart.width + gap
    return if (reachesAcross) max(underButtons, topStart.height + gap) else underButtons
}

/**
 * Where a line of the corner with the player's things goes down the screen, given the settings
 * button standing in the top end corner beside it.
 *
 * The button is a fixed sprite and is never squeezed: a corner that handed it only what the
 * player's things left over drew it at no size at all on a narrow phone. Instead every line of the
 * player's corner either fits in the width the button leaves beside it and stays where it is, or is
 * too wide for that and is moved down past the bottom of the button, where it has the whole width.
 *
 * @param top where the line would stand with nothing beside it, in screen pixels from the top of
 *   the corner.
 * @param width how wide the line came out, in screen pixels.
 * @param roomBeside the width the button leaves beside it, the gap between the two included.
 * @param clearanceBottom how far down the button reaches, gap under it included.
 * @return Where the top of the line goes: [top] when it fits beside the button, otherwise [top] or
 * the bottom of the button, whichever is lower.
 */
internal fun clearedTopOf(top: Int, width: Int, roomBeside: Int, clearanceBottom: Int): Int =
    if (width <= roomBeside) top else max(top, clearanceBottom)

/**
 * The room the corner blocks of the main screen leave in the middle of it for the pet, in screen
 * pixels from the start and the top of the screen.
 *
 * @property left where the room begins across.
 * @property top where it begins down the screen.
 * @property right where it ends across.
 * @property bottom where it ends down the screen.
 */
internal data class StageBand(val left: Int, val top: Int, val right: Int, val bottom: Int) {

    /** How wide the room is. */
    val width: Int get() = right - left

    /** How tall it is. */
    val height: Int get() = bottom - top
}

/**
 * Works out what the corner blocks leave the pet.
 *
 * Which way the blocks stand beside it depends on the shape of the screen. On one taller than it is
 * wide they take the top and the bottom of it and the pet gets the band between them, the whole
 * width across — that is the upright phone everyone knows. On a wide one they stand along the sides
 * instead, two to each, and the pet gets the column between them, the whole height down — a phone
 * held sideways has no height to give away, and taking the corners out of it would leave the pet a
 * sliver.
 *
 * @param width width of the screen, in screen pixels.
 * @param height height of the screen, in screen pixels.
 * @param topStart size of the block in the top start corner.
 * @param topEnd size of the block in the top end corner.
 * @param bottomStart size of the block in the bottom start corner.
 * @param bottomEnd size of the block in the bottom end corner.
 * @param gap how much room to leave between the blocks and the pet.
 * @return The band left in the middle. A screen with less room than the blocks alone need leaves a
 * band of no size at all rather than an upside-down one, sitting where the blocks meet.
 */
internal fun stageBandOf(
    width: Int,
    height: Int,
    topStart: CornerBlock,
    topEnd: CornerBlock,
    bottomStart: CornerBlock,
    bottomEnd: CornerBlock,
    gap: Int
): StageBand = if (height >= width) {
    val (top, bottom) = heldApart(
        start = max(topStart.height, topEnd.height) + gap,
        end = height - max(bottomStart.height, bottomEnd.height) - gap
    )
    StageBand(left = 0, top = top, right = width, bottom = bottom)
} else {
    val (left, right) = heldApart(
        start = max(topStart.width, bottomStart.width) + gap,
        end = width - max(topEnd.width, bottomEnd.width) - gap
    )
    StageBand(left = left, top = 0, right = right, bottom = height)
}

/**
 * Keeps the two ends of a band in order.
 *
 * @param start where the band begins.
 * @param end where it ends.
 * @return The two as they are, or — when the blocks on either side have taken more than the screen
 * has, so that the end would come before the start — the point halfway between them, twice: a band
 * of no size where the two sides meet.
 */
private fun heldApart(start: Int, end: Int): Pair<Int, Int> =
    if (start <= end) start to end else ((start + end) / 2).let { it to it }

/**
 * Main game screen: shows the pet, current balance/goal progress, and navigation entry
 * points to the other screens.
 *
 * Layout: four blocks in the corners and the pet in whatever they leave, laid out by
 * [MainScreenStage] from the sizes the blocks actually come out at rather than from guesses about
 * them — a screen that has to say more, because the text is set larger or the pet has a long name,
 * hands the pet less room instead of running one thing over another.
 * - Top-start: the balance chip, the pet's stats ([PlayerStats]) in one strip under it and the
 *   player's goals under them — a row of cards scrolled sideways ([GoalsCarousel]), exactly as wide
 *   as the chips above it ([PlayerCorner]).
 *   Every stat is a compact [StatChip] — an icon and a percentage — so the stats take a corner
 *   instead of half the screen.
 * - Top-end: the button for opening settings, and under it — in a demo build only — the button
 *   that skips [DemoMode.FAST_FORWARD_HOURS] hours of the pet's life. The two are stacked wherever
 *   there is height to stack them and laid along the top of a screen that has none, i.e. of a
 *   phone held sideways, where the action buttons sit right under that corner (see
 *   [ControlsCorner]). Stacked, the time button is wider than the button above it and hangs down
 *   past the player's things when it reaches that far across ([timeButtonTopOf]).
 * - Bottom-start and bottom-end: the buttons that lead to the budget and the log, and the action
 *   buttons for quests/inventory/shop. Both groups shrink together by [bottomRowFit] on a screen too
 *   narrow for the row, so every button of it — money and action alike — stays the size of the
 *   others instead of one of them being squeezed (see [MoneyActions], [PrimaryActions]).
 * - Middle: the badge naming the pet and the sub-location, the [PetStage] under it and the daily
 *   bonus button under that while the bonus is unclaimed ([PetColumn]). The badge and the button
 *   take the room they need and the game area is handed every last bit of what is left, so a screen
 *   held sideways shows a pet as large as an upright one does.
 *
 * @param state current game state; [GameUiState.petName] and [GameUiState.subLocationIndex] make up
 *   the badge above the pet, [GameUiState.balance] fills the balance chip,
 *   [GameUiState.dailyBonusAvailable] decides whether the bonus button is there at all and
 *   [GameUiState.stats] fills the stat chips.
 * @param onOpenScreen called with the [Screen] that should be opened when a navigation button
 *   (settings, budget, log, quests, inventory, shop) is pressed.
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
 * @param onPetTap called when the player pats the pet — taps it in the game area — and the pat
 *   counts, i.e. no more often than [PetTouchController.MIN_TAP_INTERVAL_MILLIS]; the hearts over
 *   the pet are the screen's own business, the sound of the pat is the caller's.
 * @param goals цели игрока в том порядке, в каком он их отмечал, уже сведённые с каталогом
 *   ([com.legacy.fingame.game.items.Goals.linesOf]); пустой список — карточка-подсказка на их месте.
 * @param onOpenGoal вызывается с целью, по карточке которой нажали.
 */
@Composable
fun MainScreen(
    state: GameUiState,
    onOpenScreen: (Screen) -> Unit,
    onClaimDailyBonus: () -> Unit,
    modifier: Modifier = Modifier,
    onFastForward: (() -> Unit)? = null,
    // TODO: default pulls from demo content; replace with the real scene of the player's pet.
    scene: GameScene = DemoScene,
    // TODO: default pulls from demo content; replace with real sub-location names for the current location.
    subLocationTitles: List<String> = DemoContent.subLocationTitles,
    onPetTap: () -> Unit = {},
    goals: List<GoalLine> = emptyList(),
    onOpenGoal: (ItemSelection) -> Unit = {}
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(ScreenPadding)
    ) {
        val upright = maxHeight >= maxWidth
        val actionSize = GameDimens.buttonSize(PrimaryActionSize)
        val fit = bottomRowFit(
            availableWidth = maxWidth.value,
            // Five buttons of one size — money and action alike — a gap inside either group and a
            // wider one between them.
            neededWidth = (actionSize * 5 + ActionGap * 3 + BottomGroupGap).value,
            smallestButton = actionSize.value,
            minTouchTarget = MinTouchTarget.value
        )

        // The buttons of the top end corner are stacked wherever there is height to stack them,
        // and laid along the top of a screen that has none — a phone held sideways, where the
        // action buttons sit right under that corner.
        val controlsInRow = !upright && GameDimens.isShortScreen

        // Stacked, the top end corner is the settings button alone: a sprite of a fixed size the
        // player's things keep clear of themselves (see [PlayerCorner]) rather than a block the
        // stage squeezes to whatever they leave over.
        val settingsSize = GameDimens.buttonSize(SecondaryActionSize)

        MainScreenStage(
            modifier = Modifier.fillMaxSize(),
            topStartKeepsClear = !controlsInRow,
            topStart = {
                PlayerCorner(
                    state = state,
                    goals = goals,
                    onOpenGoal = onOpenGoal,
                    onOpenShop = { onOpenScreen(Screen.SHOP) },
                    clearance = if (controlsInRow) {
                        DpSize.Zero
                    } else {
                        DpSize(width = settingsSize + BlockGap, height = settingsSize + ChipGap)
                    }
                )
            },
            topEnd = {
                ControlsCorner(
                    inRow = controlsInRow,
                    onOpenScreen = onOpenScreen,
                    onFastForward = onFastForward.takeIf { controlsInRow }
                )
            },
            // Stacked, the corner hangs the demo build's time button under the settings button;
            // laid along the top, it has taken the button in beside the others already.
            timeButton = {
                if (!controlsInRow && onFastForward != null) {
                    FastForwardButton(onClick = onFastForward)
                }
            },
            bottomStart = {
                MoneyActions(
                    size = PrimaryActionSize * fit,
                    gap = ActionGap * fit,
                    onOpenScreen = onOpenScreen
                )
            },
            bottomEnd = {
                PrimaryActions(
                    size = PrimaryActionSize * fit,
                    gap = ActionGap * fit,
                    onOpenScreen = onOpenScreen,
                    questsBadge = state.hasUnseenQuestStep
                )
            },
            center = {
                PetColumn(
                    scene = scene,
                    title = stageTitleOf(
                        petName = state.petName,
                        subLocationTitle = subLocationTitles.getOrNull(state.subLocationIndex)
                    ),
                    // Only an upright screen holds the area to a share of the width: on a wide one
                    // the band between the corner blocks is narrow enough as it is.
                    stageMaxWidth = if (upright) {
                        maxWidth * PetAreaWidthFraction
                    } else {
                        Dp.Unspecified
                    },
                    dailyBonusAvailable = state.dailyBonusAvailable,
                    onClaimDailyBonus = onClaimDailyBonus,
                    onPetTap = onPetTap
                )
            }
        )
    }
}

/**
 * Lays the main screen out: a block in each corner and the pet in whatever they leave.
 *
 * Everything is placed from what the blocks measure, so nothing has to assume how wide the goal
 * card is or how tall a button with a longer label comes out:
 * - the two top blocks share one line. The one with the player's things keeps at least the width it
 *   needs to say them and the buttons beside it take what is left, wrapping their labels rather
 *   than running under the goal card;
 * - the bottom blocks share one line the same way the top ones do: both the money buttons and the
 *   action buttons are already sized to the screen by [bottomRowFit], so the two corners meet where
 *   the row's own math says they will, with nothing left over for either side to claim (see
 *   [MoneyActions]);
 * - the demo build's time button hangs under the top end corner, pinned to the end of the screen,
 *   and steps down past the player's things when it is wide enough to reach them
 *   ([timeButtonTopOf]). The corner it hangs under counts as reaching down to the bottom of it, so
 *   nothing else is laid out over it either;
 * - the pet gets the band the corners leave ([stageBandOf]) and is centered in it.
 *
 * @param topStart block for the top start corner, e.g. [PlayerCorner].
 * @param topEnd block for the top end corner, e.g. [ControlsCorner].
 * @param timeButton the demo build's time button, hung under [topEnd]; a slot that puts nothing
 *   there at all when the build is not a demo one, or when the corner itself has taken the button
 *   in beside the other buttons.
 * @param bottomStart block for the bottom start corner, e.g. [MoneyActions].
 * @param bottomEnd block for the bottom end corner, e.g. [PrimaryActions].
 * @param center what goes in the middle, e.g. [PetColumn]. Laid out last, out of what the corners
 *   leave.
 * @param modifier modifier applied to the layout; the screen always states its size here, since a
 *   layout of corners has no size of its own to speak of.
 * @param gap how much room to leave between the corner blocks and the middle.
 * @param topStartKeepsClear whether [topStart] keeps clear of [topEnd] by itself, as [PlayerCorner]
 *   does of the settings button standing beside it: then both top blocks are measured at their own
 *   size, the full width for the one and what it needs for the other, and neither is squeezed by
 *   what the other takes. Otherwise — a phone held sideways, where [topEnd] is a whole row of
 *   buttons — the player's things keep the width they need and the buttons take what is left.
 */
@Composable
private fun MainScreenStage(
    topStartKeepsClear: Boolean,
    topStart: @Composable () -> Unit,
    topEnd: @Composable () -> Unit,
    timeButton: @Composable () -> Unit,
    bottomStart: @Composable () -> Unit,
    bottomEnd: @Composable () -> Unit,
    center: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    gap: Dp = BlockGap
) {
    Layout(
        contents = listOf(topStart, topEnd, timeButton, bottomStart, bottomEnd, center),
        modifier = modifier
    ) { measurables, constraints ->
        val (topStartAt, topEndAt, timeButtonAt, bottomStartAt, bottomEndAt) = measurables
        val centerAt = measurables[5]
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val gapPx = gap.roundToPx()
        val loose = Constraints(maxWidth = width, maxHeight = height)

        val topStartOne = topStartAt.first()
        val topEndOne = topEndAt.first()
        val topEndPlaced: Placeable
        val topStartPlaced: Placeable
        if (topStartKeepsClear) {
            topEndPlaced = topEndOne.measure(loose)
            topStartPlaced = topStartOne.measure(loose)
        } else {
            val topEndRoom =
                (width - gapPx - topStartOne.minIntrinsicWidth(height)).coerceIn(0, width)
            topEndPlaced = topEndOne.measure(
                loose.copy(maxWidth = min(topEndOne.maxIntrinsicWidth(height), topEndRoom))
            )
            topStartPlaced = topStartOne.measure(
                loose.copy(maxWidth = (width - gapPx - topEndPlaced.width).coerceAtLeast(0))
            )
        }
        // Нижние углы делят одну строку так же, как верхние: угол с кнопками действий берёт,
        // сколько ему нужно, а кнопкам денег остаётся остальное — их надписи ужимаются сами.
        val bottomEndPlaced = bottomEndAt.first().measure(loose)
        val bottomStartPlaced = bottomStartAt.first().measure(
            loose.copy(maxWidth = (width - gapPx - bottomEndPlaced.width).coerceAtLeast(0))
        )

        // The time button is free to be wider than the corner it hangs under and to stick out
        // towards the middle of the screen, so it is measured against the whole width.
        val timeButtonPlaced = timeButtonAt.firstOrNull()?.measure(loose)
        val timeButtonTop = timeButtonPlaced?.let { button ->
            timeButtonTopOf(
                width = width,
                timeButtonWidth = button.width,
                topStart = topStartPlaced.block,
                topEnd = topEndPlaced.block,
                gap = gapPx
            )
        }
        val topEndBlock = if (timeButtonPlaced == null || timeButtonTop == null) {
            topEndPlaced.block
        } else {
            CornerBlock(
                width = max(topEndPlaced.width, timeButtonPlaced.width),
                height = timeButtonTop + timeButtonPlaced.height
            )
        }

        val band = stageBandOf(
            width = width,
            height = height,
            topStart = topStartPlaced.block,
            topEnd = topEndBlock,
            bottomStart = bottomStartPlaced.block,
            bottomEnd = bottomEndPlaced.block,
            gap = gapPx
        )
        val centerPlaced = centerAt.first().measure(
            Constraints(maxWidth = band.width, maxHeight = band.height)
        )

        layout(width, height) {
            topStartPlaced.place(x = 0, y = 0)
            topEndPlaced.place(x = width - topEndPlaced.width, y = 0)
            if (timeButtonPlaced != null && timeButtonTop != null) {
                // Pinned to the same end of the screen the settings button above it is, so however
                // much wider the button comes out, the two share one edge down their right side.
                timeButtonPlaced.place(x = width - timeButtonPlaced.width, y = timeButtonTop)
            }
            bottomStartPlaced.place(x = 0, y = height - bottomStartPlaced.height)
            bottomEndPlaced.place(
                x = width - bottomEndPlaced.width,
                y = height - bottomEndPlaced.height
            )
            centerPlaced.place(
                x = band.left + (band.width - centerPlaced.width) / 2,
                y = band.top + (band.height - centerPlaced.height) / 2
            )
        }
    }
}

/** The size a measured corner block came out at, as [stageBandOf] wants it stated. */
private val Placeable.block: CornerBlock get() = CornerBlock(width = width, height = height)

/**
 * The corner with what the player has: the money, the pet's stats in one strip under it and the
 * goal the player is saving towards.
 *
 * The money keeps a line to itself and the stats share the one below it ([ChipsPerRow]): the
 * balance chip carries the money and the deposit and grows with both, so standing it next to a stat
 * would leave the stat a sliver on a narrow screen — and the three stats in a row of their own read
 * as one thing, the state of the pet, instead of a ragged two-and-one block. Every stat chip keeps
 * the room of its widest value, "100%", whatever it says right now (see [StatChip]), so the strip —
 * and everything on the screen laid out from its width — stays put while the stats run down.
 *
 * The corner keeps clear of the settings button beside it by itself ([clearance]): a line that fits
 * in the width the button leaves stays where it is, a wider one is moved down past the button
 * ([clearedTopOf]). The button is never squeezed for the corner's sake, and the corner is never
 * squeezed for the button's.
 *
 * The goal card is drawn exactly as wide as the chips above it rather than at some width of its own:
 * measured here instead of being told to fill what it is given, so the right edge of the card lands
 * on the right edge of the widest chip row and the corner comes out as one block with one edge down
 * its side. A [Column] cannot do that — `fillMaxWidth` there would take the whole width the corner
 * was offered, most of it empty.
 *
 * @param state game state the chips and the card are filled from.
 * @param goals цели игрока для ряда карточек под чипами ([GoalsCarousel]).
 * @param onOpenGoal нажатие на карточку цели.
 * @param onOpenShop нажатие на карточку-подсказку, пока целей нет.
 * @param clearance the room the settings button takes in the top end corner of the space the corner
 *   is given, gaps included; [DpSize.Zero] when there is nothing there to keep clear of.
 * @param modifier modifier applied to the corner.
 */
@Composable
private fun PlayerCorner(
    state: GameUiState,
    goals: List<GoalLine>,
    onOpenGoal: (ItemSelection) -> Unit,
    onOpenShop: () -> Unit,
    clearance: DpSize,
    modifier: Modifier = Modifier
) {
    val balance: @Composable () -> Unit = {
        BalanceChip(balance = state.balance, depositAmount = state.depositAmount)
    }
    val stats: @Composable () -> Unit = {
        // The stats wrap rather than being squeezed: a screen too narrow for all of them side by
        // side starts a new line instead of shrinking the chips below what they can say.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ActionGap),
            verticalArrangement = Arrangement.spacedBy(ChipGap),
            maxItemsInEachRow = ChipsPerRow
        ) {
            PlayerStats.forEach { stat ->
                StatChip(stat = stat, stats = state.stats, compact = true)
            }
        }
    }
    // Ряд целей меряется ниже так же, как мерилась одна карточка, — ровно в ширину чипов; высоту
    // он берёт у карточки, сколько бы целей в нём ни было.
    val goal: @Composable () -> Unit = {
        GoalsCarousel(
            goals = goals,
            balance = state.balance,
            onOpenGoal = onOpenGoal,
            onOpenShop = onOpenShop
        )
    }

    // Сцена спрашивает у угла его наименьшую/наибольшую ширину (intrinsic), а ряд целей — это
    // LazyRow, который на такой вопрос падает. Ширину угла задают деньги и чипы (цели берут ровно
    // их ширину), поэтому ответ собирается только из них, а ряд целей не спрашивается вовсе.
    // Intrinsic-высоту угла не спрашивает никто ([MainScreenStage] меряет его обычным measure),
    // и честно ответить на неё без ряда целей нельзя: высоту слоту даёт карточка внутри LazyRow.
    // Поэтому высота не переопределена — если её когда-нибудь спросят, падение укажет сюда.
    val measurePolicy = remember(clearance) {
        object : MultiContentMeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<List<Measurable>>,
                constraints: Constraints
            ): MeasureResult {
                val gapPx = ChipGap.roundToPx()
                val loose = constraints.copy(minWidth = 0, minHeight = 0)
                val roomBeside =
                    (constraints.maxWidth - clearance.width.roundToPx()).coerceAtLeast(0)
                val clearanceBottom = clearance.height.roundToPx()

                // The money always fits beside the button: it is measured against what the button
                // leaves.
                val balancePlaced =
                    measurables[0].first().measure(loose.copy(maxWidth = roomBeside))
                val statsPlaced = measurables[1].first().measure(loose)
                val chipsWidth = max(balancePlaced.width, statsPlaced.width)
                // The card is handed the width the chips came out at, and no choice about it.
                val goalPlaced = measurables[2].first().measure(
                    loose.copy(minWidth = chipsWidth, maxWidth = chipsWidth)
                )

                val statsTop = clearedTopOf(
                    top = balancePlaced.height + gapPx,
                    width = statsPlaced.width,
                    roomBeside = roomBeside,
                    clearanceBottom = clearanceBottom
                )
                val goalTop = clearedTopOf(
                    top = statsTop + statsPlaced.height + gapPx,
                    width = goalPlaced.width,
                    roomBeside = roomBeside,
                    clearanceBottom = clearanceBottom
                )

                return layout(
                    width = max(chipsWidth, goalPlaced.width),
                    height = goalTop + goalPlaced.height
                ) {
                    balancePlaced.place(x = 0, y = 0)
                    statsPlaced.place(x = 0, y = statsTop)
                    goalPlaced.place(x = 0, y = goalTop)
                }
            }

            override fun IntrinsicMeasureScope.minIntrinsicWidth(
                measurables: List<List<IntrinsicMeasurable>>,
                height: Int
            ): Int = measurables.take(2).maxOf { it.first().minIntrinsicWidth(height) }

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                measurables: List<List<IntrinsicMeasurable>>,
                height: Int
            ): Int = measurables.take(2).maxOf { it.first().maxIntrinsicWidth(height) }
        }
    }

    Layout(
        contents = listOf(balance, stats, goal),
        modifier = modifier,
        measurePolicy = measurePolicy
    )
}

/**
 * The corner with the button that opens settings, joined on a screen with no height to stack it
 * by the demo build's button that skips a while of the pet's life.
 *
 * Which way they are laid out is decided by the shape of the screen, since the corner has to fit
 * next to the other blocks either way. A screen with height to spare stacks them into a column, and
 * the demo build's time button hangs under them as its own block (see [MainScreenStage]); a phone
 * held sideways has no height — the action buttons sit right under this corner there — and lays all
 * of them along the top instead, counted from the corner outwards in the same order the column has
 * them from the top down.
 *
 * @param inRow whether to lay the buttons along the top rather than stack them.
 * @param onOpenScreen called with the screen a button opens.
 * @param onFastForward called when the demo's time button is pressed, or `null` when this corner is
 *   not the one carrying that button — or when the build is not a demo one at all.
 * @param modifier modifier applied to the row or the column.
 */
@Composable
private fun ControlsCorner(
    inRow: Boolean,
    onOpenScreen: (Screen) -> Unit,
    onFastForward: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val settings: @Composable () -> Unit = {
        SpriteButton(
            assetPath = Sprites.SETTINGS,
            contentDescription = "Открыть настройки",
            onClick = { onOpenScreen(Screen.OPTIONS) },
            size = SecondaryActionSize,
            showIndicator = false
        )
    }
    val fastForward: @Composable () -> Unit = {
        if (onFastForward != null) {
            FastForwardButton(onClick = onFastForward)
        }
    }

    if (inRow) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(ControlGap, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            fastForward()
            settings()
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(ControlGap),
            horizontalAlignment = Alignment.End
        ) {
            settings()
            fastForward()
        }
    }
}

/**
 * The demo build's button that skips [DemoMode.FAST_FORWARD_HOURS] hours of the pet's life.
 *
 * Both corners that can end up carrying it — stacked under the settings button, or beside it along
 * the top of a screen held sideways (see [ControlsCorner]) — draw it from here, so it cannot come
 * out one way upright and another way sideways.
 *
 * It is a [PillStyle.Tonal] button and a `compact` one: it is there to wind the demo forward, not to
 * be the thing the player is meant to press, and the screen keeps its one filled button for the
 * daily bonus under the pet.
 *
 * The label is the short "+12 ч" rather than "Вперёд 12 ч": the strip of stats keeps the room of
 * "100%" in every chip now, and on an ordinary 411 dp phone that leaves about a hundred dp beside
 * the player's things — room for five glyphs and the compact padding, not for eleven. The long label
 * did not fit there and dropped the button below the whole corner, standing alone in the empty half
 * of the screen (see [timeButtonTopOf]). The words are still said in full to a screen reader.
 *
 * @param onClick called when the button is pressed.
 * @param modifier modifier applied to the button.
 */
@Composable
private fun FastForwardButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PillButton(
        text = "+${DemoMode.FAST_FORWARD_HOURS}${NoBreakSpace}ч",
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = "Вперёд на ${DemoMode.FAST_FORWARD_HOURS} часов"
        },
        style = PillStyle.Tonal,
        compact = true
    )
}

/**
 * The corner with the buttons that lead to the money screens: the budget plan and the log.
 *
 * Same [Row] geometry as [PrimaryActions] — same gap, same button size — so the two bottom corners
 * read as one panel rather than two different kinds of controls: the five buttons come out the same
 * size and, both corners being pinned to the bottom of the screen, stand on one line. All five are
 * bare icons with no caption under them: [Sprites.BUDGET] and [Sprites.LOG] are in `assets/` now, so
 * these two need no `label` fallback either and stopped being the odd pair of captioned plates in a
 * row of pictures.
 *
 * @param size size of one button — the same [PrimaryActionSize] the action buttons take, so the
 *   five of them come out one size and stand on one line; shrunk together with everything else
 *   along the bottom of the screen by [bottomRowFit].
 * @param gap gap between them, shrunk by the same amount.
 * @param onOpenScreen called with the screen a button opens.
 * @param modifier modifier applied to the row.
 */
@Composable
private fun MoneyActions(
    size: Dp,
    gap: Dp,
    onOpenScreen: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpriteButton(
            assetPath = Sprites.BUDGET,
            contentDescription = "Открыть бюджет",
            onClick = { onOpenScreen(Screen.BUDGET) },
            size = size,
            showIndicator = false
        )
        SpriteButton(
            assetPath = Sprites.LOG,
            contentDescription = "Открыть журнал",
            onClick = { onOpenScreen(Screen.LOG) },
            size = size,
            showIndicator = false
        )
    }
}

/**
 * Размер точки на кнопке квестов: есть шаг, который игрок ещё не видел. Точка стоит в углу кнопки и
 * размер кнопки не меняет.
 */
private val QuestBadgeSize = 12.dp

/** Толщина кольца цвета фона вокруг точки [QuestBadgeSize]: оно отделяет точку от картинки кнопки. */
private val QuestBadgeRing = 2.dp

/**
 * The corner with the buttons that open the quests, the inventory and the shop.
 *
 * None of them belongs to a group one item of which is selected, so none keeps room under itself
 * for the underline that would mark it — a button lifted off the row by room for a mark it can never
 * carry stands out for a reason the player cannot see (see [SpriteButton]).
 *
 * @param size size of one button; shrunk together with everything else along the bottom of the
 *   screen by [bottomRowFit].
 * @param gap gap between them, shrunk by the same amount.
 * @param onOpenScreen called with the screen a button opens.
 * @param questsBadge whether the quests button carries the dot of a step the player has not seen.
 * @param modifier modifier applied to the row.
 */
@Composable
private fun PrimaryActions(
    size: Dp,
    gap: Dp,
    onOpenScreen: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    questsBadge: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            SpriteButton(
                assetPath = Sprites.QUESTS,
                contentDescription = if (questsBadge) {
                    "Открыть квесты, есть новый шаг"
                } else {
                    "Открыть квесты"
                },
                onClick = { onOpenScreen(Screen.QUESTS) },
                size = size,
                showIndicator = false
            )
            if (questsBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(QuestBadgeSize)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                        .border(QuestBadgeRing, MaterialTheme.colorScheme.background, CircleShape)
                )
            }
        }
        SpriteButton(
            assetPath = Sprites.INVENTORY,
            contentDescription = "Открыть инвентарь",
            onClick = { onOpenScreen(Screen.INVENTORY) },
            size = size,
            showIndicator = false
        )
        SpriteButton(
            assetPath = Sprites.SHOP,
            contentDescription = "Открыть магазин",
            onClick = { onOpenScreen(Screen.SHOP) },
            size = size,
            showIndicator = false
        )
    }
}

/**
 * The middle of the screen: the badge naming the pet and the place it is in, the game area under it
 * and the daily bonus button under that.
 *
 * The badge and the button take the height they need and the game area is given every last bit of
 * what the column has left ([PetStage] is weighted, but only takes what it can square off), so the
 * pet is as large as the screen allows and the column is no taller than what is in it.
 *
 * @param scene what stands in the game area.
 * @param title text of the badge, or `null` when there is nothing to say and the badge is left out.
 * @param stageMaxWidth widest the game area may be, or [Dp.Unspecified] to let it take the whole
 *   width of the column.
 * @param dailyBonusAvailable whether the daily bonus is there to take, i.e. whether the button
 *   under the pet is shown at all.
 * @param onClaimDailyBonus called when the player takes the bonus.
 * @param onPetTap called when the player pats the pet (see [PetStage]).
 * @param modifier modifier applied to the column.
 */
@Composable
private fun PetColumn(
    scene: GameScene,
    title: String?,
    stageMaxWidth: Dp,
    dailyBonusAvailable: Boolean,
    onClaimDailyBonus: () -> Unit,
    onPetTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (title != null) {
            StageBadge(title = title)
            Spacer(modifier = Modifier.height(StageGap))
        }

        PetStage(
            scene = scene,
            onPetTap = onPetTap,
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = stageMaxWidth)
        )

        if (dailyBonusAvailable) {
            Spacer(modifier = Modifier.height(StageGap))
            // The one filled button of the screen: taking the bonus is the thing the player is
            // meant to press here, and everything else on the screen is quieter than it.
            PillButton(
                text = "Бонус дня +${Economy.DAILY_BONUS}",
                onClick = onClaimDailyBonus,
                style = PillStyle.Primary
            )
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
 * The card is exactly as large as the room it is given — the caller hands it the width and the
 * height the area may take and it squares that off — whatever the scene is doing: a pinch changes
 * how much of the room is visible, never the size of the card ([SceneViewport.windowSide]). The
 * scene is pixel art blown up by a whole number of screen pixels ([SceneViewport.scale]) and is
 * never drawn smaller than the window ([SceneViewport.minScale]), so the card's own surface never
 * shows around the room, on a phone or on a tablet alike.
 *
 * On a phone the scene is larger than the window and only a part of it is visible at a time; the
 * player moves the rest into view with a finger, like a map: the whole scene travels together, and
 * the edges of the room stop the drag so no empty band next to it can be pulled into view. Two
 * fingers pinch the scene larger or smaller around the spot they hold, between
 * [SceneViewport.minScale] and [SceneViewport.maxScale]; the scene steps from one whole blow-up to
 * the next as they go, so the pixel grid stays as crisp in the middle of a pinch as it is at rest. A
 * tablet may leave the player nothing to drag, but the card is exactly the same size either way.
 *
 * How big the scene is drawn and how far it is dragged outlive a recomposition and a turn of the
 * device, and an area that changed size holds both to what it now allows. Both are read in the
 * layout pass and not during composition ([sceneWindow], [sceneIn]), so a finger moving the room
 * around never composes the sprites it is stacked out of again.
 *
 * A tap on the pet pats it: hearts rise over its head ([SceneLayers]) and [onPetTap] is called, no
 * more often than [PetTouchController.MIN_TAP_INTERVAL_MILLIS]. A drag or a pinch that starts on
 * the pet moves the room as anywhere else and pats nothing.
 *
 * @param scene what stands on each layer of the area.
 * @param modifier modifier applied to the card; this is where the caller states how much room the
 *   area has, e.g. as a share of the screen or as the height left over in a column.
 * @param onPetTap called when the player pats the pet and the pat counts.
 */
@Composable
@VisibleForTesting
internal fun PetStage(
    scene: GameScene,
    modifier: Modifier = Modifier,
    onPetTap: () -> Unit = {}
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val base = remember(constraints, density) {
            // An area told it may be as large as it likes is an area with nothing to measure
            // against, and it gets the smallest window there is rather than an endless one.
            val roomWide = if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else 0f
            val roomHigh = if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() else 0f
            SceneViewport.of(
                availableWidth = roomWide,
                availableHeight = roomHigh,
                scenePixels = GameLayer.BACKGROUND.spritePixels,
                pixelSize = with(density) { ScenePixelSize.toPx() },
                maxZoom = SceneMaxZoom
            )
        }
        var scale by rememberSaveable { mutableStateOf(base.scale) }
        var moved by rememberSaveable(stateSaver = SceneOffsetSaver) {
            mutableStateOf(base.initialOffset)
        }
        val viewport = { base.withScale(scale) }

        // An area that was resized — a turn of the device, a folded screen — may have been left
        // showing the scene at a size it no longer allows, or moved further out than its new edges
        // do, so both are held to it again.
        LaunchedEffect(base) {
            scale = base.heldScale(scale)
            moved = base.withScale(scale).clamp(moved)
        }

        Surface(
            modifier = Modifier
                .sceneWindow { base.windowSide }
                .testTag(PetStageTag)
                .semantics {
                    sceneScale = scale
                    sceneOffsetX = moved.x
                    sceneOffsetY = moved.y
                },
            shape = RoundedCornerShape(SceneCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GameColors.cardStroke),
            shadowElevation = 3.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(base) {
                        // How far the pinch has carried the scale, fractions and all. The scene
                        // follows it in whole steps, and this is what remembers where between two
                        // of them the fingers actually are.
                        var pinched = viewport().scale
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val current = viewport()
                            pinched = (pinched * zoom)
                                .coerceIn(current.minScale, current.maxScale)
                            val zoomed = current.zoomedAt(
                                rawScale = pinched,
                                focusX = centroid.x - size.width / 2f,
                                focusY = centroid.y - size.height / 2f,
                                moved = current.clamp(moved + SceneOffset(x = pan.x, y = pan.y))
                            )
                            scale = zoomed.viewport.scale
                            moved = zoomed.offset
                        }
                    }
            ) {
                SceneLayers(
                    scene = scene,
                    viewport = viewport,
                    moved = { moved },
                    onPetTap = onPetTap
                )
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
 * The pet does not stand in the middle of the room but on its floor: the pet and everything on its
 * grid — the clothes it wears, and the placeholder for missing art that is drawn in its place — are
 * lowered by [SceneViewport.petFloorShift], a whole number of screen pixels that grows with the
 * scene as it is pinched, so the pet stays on the room's pixel grid at any size ([isOnPetGrid],
 * [loweredOntoFloor]).
 *
 * A sprite whose file is not in the assets yet is not drawn here at all: [SpriteLoader] would hand
 * back the same placeholder for every one of them, and the scene would stack a pile of them on top
 * of each other — the scenery, the pet and everything it wears, all at once. Instead the scene
 * draws what it has and adds a single placeholder over it, so the missing art is still plain to see
 * without the pile.
 *
 * The pet sprite itself listens for taps: a tap on it pats the pet, which sends a wave of hearts up
 * over its head on a layer above everything else in the room ([PetHearts]) and calls [onPetTap].
 * How often a pat counts is [PetTouchController]'s business; the waves live on the device's uptime
 * clock rather than the game's, since a demo skipping hours ahead has nothing to do with how long
 * a heart takes to rise. The taps are caught by the sprite and not by the whole area, so Compose's
 * own hit testing — which already knows where the scene was dragged, how large it is drawn and how
 * far the pet is lowered onto the floor — decides what the finger is on.
 *
 * @param scene what stands on each layer.
 * @param viewport geometry of the window and the scene behind it, read in the layout pass.
 * @param moved how far the scene is dragged, read in the layout pass as well.
 * @param onPetTap called when the player pats the pet and the pat counts.
 * @param modifier modifier applied to the stack.
 */
@Composable
private fun SceneLayers(
    scene: GameScene,
    viewport: () -> SceneViewport,
    moved: () -> SceneOffset,
    onPetTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loader = remember(context) { SpriteLoader(context) }
    val (drawn, anythingMissing) = remember(scene, loader) {
        val all = GameLayer.DRAW_ORDER.flatMap { layer -> scene[layer].map { layer to it } }
        val present = all.filter { (_, sprite) -> loader.hasSprite(sprite.assetPath) }
        present to (present.size < all.size)
    }

    val touch = remember { PetTouchController() }
    var bursts by remember { mutableStateOf(emptyList<HeartBurst>()) }
    var nowMillis by remember { mutableLongStateOf(0L) }
    val currentOnPetTap by rememberUpdatedState(onPetTap)
    // Read through a state rather than straight off [scene]: the pet's tap handler is set up once
    // and outlives the pet growing up, and the hearts have to find the head the pet has now.
    val currentAge by rememberUpdatedState(scene.animalAge)
    val pat: () -> Unit = {
        val now = SystemClock.uptimeMillis()
        if (touch.onTap(now, age = currentAge) != null) {
            nowMillis = now
            bursts = touch.alive(now)
            currentOnPetTap()
        }
    }

    // The hearts move while there are any: once a frame the clock is read again and the waves that
    // are over are dropped, and once the last one is, the loop ends until the next pat.
    if (bursts.isNotEmpty()) {
        LaunchedEffect(Unit) {
            while (true) {
                withFrameMillis { }
                val now = SystemClock.uptimeMillis()
                nowMillis = now
                bursts = touch.alive(now)
            }
        }
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
                    .loweredOntoFloor(enabled = layer.isOnPetGrid, viewport = viewport)
                    .fillMaxSize(layer.sizeFraction)
                    .aspectRatio(1f)
                    .then(
                        if (layer == GameLayer.ANIMAL) {
                            Modifier.pointerInput(Unit) { detectTapGestures(onTap = { pat() }) }
                        } else {
                            Modifier
                        }
                    )
            )
        }

        PetHearts(bursts = bursts, nowMillis = { nowMillis }, viewport = viewport)

        if (anythingMissing) {
            Sprite(
                assetPath = SpriteLoader.MISSING_SPRITE,
                contentDescription = "Часть картинок ещё не нарисована",
                modifier = Modifier
                    .zIndex(GameLayer.CLOTHES.zIndex)
                    .loweredOntoFloor(enabled = true, viewport = viewport)
                    .fillMaxSize(GameLayer.ANIMAL.sizeFraction)
                    .aspectRatio(1f)
            )
        }
    }
}

/**
 * The waves of hearts rising over the pet after it was patted, drawn over everything else in the
 * room (see [HeartsZIndex]) and on the pet's own grid: lowered onto the floor with it, and every
 * move counted in pixels of the artwork and multiplied by the scene's scale, so the hearts stay on
 * the pet however the room is pinched and dragged. Each heart is moved by a whole number of screen
 * pixels, so its own pixels stay crisp. The area clips the scene, so a heart never rises over the
 * badge or the buttons around the card.
 *
 * The hearts are moved in the layout and drawing passes ([nowMillis] is read there), so they rise
 * without composing the room again on every frame.
 *
 * @param bursts the waves still in the air.
 * @param nowMillis what time it is on the clock the waves were set off on.
 * @param viewport geometry of the scene, for how many screen pixels a pixel of the art is.
 */
@Composable
private fun PetHearts(
    bursts: List<HeartBurst>,
    nowMillis: () -> Long,
    viewport: () -> SceneViewport
) {
    bursts.forEach { burst ->
        key(burst.id) {
            repeat(HeartBurst.HEARTS) { index ->
                Sprite(
                    assetPath = Sprites.HEART,
                    contentDescription = null,
                    modifier = Modifier
                        .zIndex(HeartsZIndex)
                        .loweredOntoFloor(enabled = true, viewport = viewport)
                        .offset {
                            val heart = burst.heartAt(index, nowMillis() - burst.startMillis)
                            val scale = viewport().scale
                            IntOffset(
                                x = ((heart?.x ?: 0f) * scale).roundToInt(),
                                y = ((heart?.y ?: 0f) * scale).roundToInt()
                            )
                        }
                        .graphicsLayer {
                            alpha = burst.heartAt(index, nowMillis() - burst.startMillis)
                                ?.alpha ?: 0f
                        }
                        .fillMaxSize(HeartSizeFraction)
                        .aspectRatio(1f)
                )
            }
        }
    }
}

/**
 * Whether the sprites of this layer are drawn on the pet's own grid — the pet itself and the
 * clothes it wears — and so go down onto the floor together with it.
 */
private val GameLayer.isOnPetGrid: Boolean
    get() = this == GameLayer.ANIMAL || this == GameLayer.CLOTHES

/**
 * Lowers what this modifier is applied to onto the floor of the room, by
 * [SceneViewport.petFloorShift].
 *
 * The shift is read at placement time, so a pinch moves the pet down with the room without
 * composing it again.
 *
 * @param enabled whether to lower it at all; the scenery of the room stays where it is.
 * @param viewport geometry of the scene, for how many screen pixels the shift is at its size.
 * @return This modifier, lowered when [enabled].
 */
private fun Modifier.loweredOntoFloor(enabled: Boolean, viewport: () -> SceneViewport): Modifier =
    if (enabled) offset { IntOffset(x = 0, y = viewport().petFloorShift) } else this

/**
 * Lays what this modifier is applied to out as the window of the game area: a square of the side
 * the viewport has picked, whatever the layout around it was prepared to give.
 *
 * The side is asked for at measuring time rather than during composition, so an area that is
 * resized — a turn of the device, a folded screen — re-measures the card without composing anything
 * again; a pinch never changes it at all.
 *
 * @param side side of the window, in screen pixels, as [SceneViewport.windowSide] states it.
 * @return This modifier with the window sized that way.
 */
private fun Modifier.sceneWindow(side: () -> Float): Modifier =
    layout { measurable, constraints ->
        val window = side().roundToInt().coerceAtLeast(0)
        val placeable = measurable.measure(Constraints.fixed(width = window, height = window))
        layout(window, window) { placeable.place(x = 0, y = 0) }
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
 * @param viewport geometry of the window and the scene behind it, read at measuring time — in the
 *   layout pass rather than during composition — so a pinch resizes the scene without composing the
 *   sprites it is stacked out of again.
 * @param moved read at placement time, for the same reason, so dragging the scene only moves it.
 * @return This modifier with the scene laid out that way.
 */
private fun Modifier.sceneIn(viewport: () -> SceneViewport, moved: () -> SceneOffset): Modifier =
    layout { measurable, constraints ->
        val current = viewport()
        val side = current.sceneSide.roundToInt()
        val placeable = measurable.measure(Constraints.fixed(width = side, height = side))
        // A game area always has its size given to it; the scene's own is the fallback for a
        // measurement that leaves an axis open, where there is no window to speak of.
        val windowWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else side
        val windowHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else side

        layout(windowWidth, windowHeight) {
            val offset = current.clamp(moved())
            placeable.place(
                x = ((windowWidth - side) / 2f + offset.x).roundToInt(),
                y = ((windowHeight - side) / 2f + offset.y).roundToInt()
            )
        }
    }

/**
 * Pill-shaped badge naming the pet and the sub-location it is in, displayed above the pet.
 *
 * The badge is only as wide as its text, up to whatever the column it stands in allows; a name that
 * still does not fit on one line is wrapped onto a second one ([StageBadgeMaxLines]) rather than
 * cut short, since a sub-location the player cannot read the name of is the same as an unnamed one.
 *
 * @param title text to display inside the badge, as built by [stageTitleOf].
 * @param modifier modifier applied to the badge surface.
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = StageBadgeMaxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** State the previews show: a pet with a name, money, stats and a bonus waiting to be taken. */
private val PreviewState = GameUiState(
    balance = 250,
    dailyBonusAvailable = true,
    hasUnseenQuestStep = true,
    subLocationIndex = 1,
    petName = "Барсик",
    stats = PetStats(
        mapOf(
            StatKind.HEALTH to 90,
            StatKind.HUNGER to 45,
            StatKind.PLEASURE to 70
        )
    )
)

/** Preview of [MainScreen] in the light theme, portrait orientation. */
@Preview(name = "MainScreen — Light", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun MainScreenLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = PreviewState,
                onOpenScreen = {},
                onClaimDailyBonus = {},
                goals = PreviewGoalLines
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
                state = PreviewState,
                onOpenScreen = {},
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
                onClaimDailyBonus = {},
                goals = PreviewGoalLines.drop(1).take(1)
            )
        }
    }
}

/**
 * Preview of the tightest upright screen the game is laid out for: a narrow phone with everything
 * on it at once — the time button, the bonus button and a pet with a name to say.
 */
@Preview(
    name = "MainScreen — Narrow phone",
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun MainScreenNarrowPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = PreviewState,
                onOpenScreen = {},
                onClaimDailyBonus = {},
                onFastForward = {}
            )
        }
    }
}

/** Preview of the smallest screen of all: the bottom row is shrunk to fit it (see [bottomRowFit]). */
@Preview(
    name = "MainScreen — Smallest phone",
    showBackground = true,
    widthDp = 320,
    heightDp = 640
)
@Composable
private fun MainScreenSmallestPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = PreviewState,
                onOpenScreen = {},
                onClaimDailyBonus = {},
                onFastForward = {}
            )
        }
    }
}

/** Preview of a narrow phone with the system text turned up as far as the game is laid out for. */
@Preview(
    name = "MainScreen — Narrow phone, large text",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.3f
)
@Composable
private fun MainScreenLargeTextPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = PreviewState,
                onOpenScreen = {},
                onClaimDailyBonus = {},
                onFastForward = {},
                goals = PreviewGoalLines
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
                state = PreviewState,
                onOpenScreen = {},
                onClaimDailyBonus = {},
                onFastForward = {}
            )
        }
    }
}

/** Preview of the tightest screen held sideways: a narrow phone with no height at all to give. */
@Preview(
    name = "MainScreen — Narrow landscape",
    showBackground = true,
    widthDp = 800,
    heightDp = 360
)
@Composable
private fun MainScreenNarrowLandscapePreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = PreviewState,
                onOpenScreen = {},
                onClaimDailyBonus = {},
                onFastForward = {},
                goals = PreviewGoalLines
            )
        }
    }
}

/**
 * Preview of [MainScreen] on a tablet held sideways, where the buttons are drawn enlarged and the
 * game area has room to spare: the scene covers the whole card, with none of the card's own surface
 * showing around it.
 */
@Preview(name = "MainScreen — Tablet", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun MainScreenTabletPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainScreen(
                state = PreviewState,
                onOpenScreen = {},
                onClaimDailyBonus = {},
                onFastForward = {},
                goals = PreviewGoalLines
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
                state = PreviewState,
                onOpenScreen = {},
                onClaimDailyBonus = {}
            )
        }
    }
}

/**
 * Preview of the game area at the size a phone gives it: the scene is larger than the card, so the
 * room is shown cut off at its edges and can be dragged and pinched around.
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
 * Preview of the game area at the size a tablet gives it: there is room to spare, so the scene
 * covers the whole card instead of a part of it, and nothing of the card itself is left to see
 * around the room.
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
