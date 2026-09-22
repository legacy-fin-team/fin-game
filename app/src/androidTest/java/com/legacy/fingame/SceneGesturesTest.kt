package com.legacy.fingame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.legacy.fingame.game.items.ItemCatalog
import com.legacy.fingame.game.scene.GameLayer
import com.legacy.fingame.game.scene.GameScene
import com.legacy.fingame.game.scene.SceneSprite
import com.legacy.fingame.ui.DemoContent
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.screens.PetStage
import com.legacy.fingame.ui.screens.PetStageTag
import com.legacy.fingame.ui.screens.SceneOffsetXKey
import com.legacy.fingame.ui.screens.SceneOffsetYKey
import com.legacy.fingame.ui.screens.SceneScaleKey
import com.legacy.fingame.ui.theme.FinGameTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the fingers do to the game area: dragging the room about, pinching it larger and smaller,
 * and the card shrinking to the room once the whole of it fits.
 *
 * None of this can be seen from the outside without a device — a pinch cannot be sent through adb
 * and the scene is moved in the layout pass, where nothing is composed again — so the area says how
 * it stands in its semantics ([SceneScaleKey], [SceneOffsetXKey], [SceneOffsetYKey]) and this is
 * read from there.
 */
@RunWith(AndroidJUnit4::class)
class SceneGesturesTest {

    /**
     * The composition the gestures are sent to.
     *
     * Deliberately the plain rule and not the newer one of `junit4.v2`: that one runs composition on
     * a dispatcher that queues its work instead of running it at once, and a test of gestures is
     * better off with a composition that has settled by the time the next finger comes down.
     */
    @Suppress("DEPRECATION")
    @get:Rule
    val compose = createComposeRule()

    /**
     * Side of a game area the way a phone gives it: too small for the whole room, so the scene is
     * blown up past it and there is something to drag.
     */
    private val phoneWindow: Dp = 280.dp

    /**
     * Side of one the way a tablet gives it: room to spare, so the whole scene fits and the card
     * shrinks to it.
     */
    private val tabletWindow: Dp = 460.dp

    /** The scene the area shows: the demo pet standing in the first sub-location, as in a preview. */
    private val scene: GameScene = GameScene.of(
        background = SceneSprite(assetPath = Sprites.locationBackground(0), description = null),
        pet = SceneSprite(
            assetPath = Sprites.pet(
                petId = DemoContent.petId,
                variantId = DemoContent.petVariantId
            ),
            description = "Питомец"
        ),
        animalId = DemoContent.petId,
        worn = emptySet(),
        catalog = ItemCatalog.EMPTY
    )

    /** The game area, as the test takes hold of it. */
    private val stage: SemanticsNodeInteraction
        get() = compose.onNodeWithTag(PetStageTag)

    /** How big a pixel of the artwork is drawn right now, in screen pixels. */
    private fun scale(): Float = stage.fetchSemanticsNode().config[SceneScaleKey]

    /** How far the scene is moved inside the window, in screen pixels. */
    private fun moved(): Offset = stage.fetchSemanticsNode().let { node ->
        Offset(x = node.config[SceneOffsetXKey], y = node.config[SceneOffsetYKey])
    }

    /** Side of the window the scene is shown through, in screen pixels. */
    private fun windowSide(): Int = stage.fetchSemanticsNode().size.width

    /** Side of the scene itself, in screen pixels. */
    private fun sceneSide(): Float = scale() * GameLayer.BACKGROUND.spritePixels

    /** How far the scene may be moved either way before its own edge would come into the window. */
    private fun free(): Float = ((sceneSide() - windowSide()) / 2f).coerceAtLeast(0f)

    /**
     * Shows a game area of a given size and nothing else.
     *
     * @param side side of the box the area is given to fill.
     */
    private fun showStage(side: Dp) {
        compose.setContent {
            FinGameTheme(darkTheme = false) {
                Box(modifier = Modifier.size(side)) {
                    PetStage(scene = scene)
                }
            }
        }
    }

    /**
     * Pinches the area with two fingers, both of them moving the same distance from the middle of
     * it, so what is under them stays where it is and only the size changes.
     *
     * @param from how far either finger starts from the middle, in screen pixels.
     * @param to how far it ends up, in screen pixels.
     */
    private fun pinchBy(from: Float, to: Float) {
        stage.performTouchInput {
            pinch(
                start0 = center + Offset(x = -from, y = 0f),
                end0 = center + Offset(x = -to, y = 0f),
                start1 = center + Offset(x = from, y = 0f),
                end1 = center + Offset(x = to, y = 0f)
            )
        }
    }

    @Test
    fun aFingerCarriesTheSceneAlongWithItAndNoFurtherThanItsOwnEdge() {
        showStage(phoneWindow)

        // A phone shows a part of the room, so there is something to drag in the first place.
        assertTrue(sceneSide() > windowSide())
        assertTrue(free() > 0f)
        assertEquals(Offset.Zero, moved())

        // A short drag to the right brings the scene right, and not as far as the edge.
        stage.performTouchInput {
            swipeRight(startX = centerX - 60f, endX = centerX + 60f)
        }
        val dragged = moved()
        assertTrue(dragged.x > 0f)
        assertTrue(dragged.x < free())

        // A drag across the whole area stops at the edge of the scene instead of pulling an empty
        // band into view.
        stage.performTouchInput { swipeRight() }
        assertEquals(free(), moved().x, 0.5f)

        stage.performTouchInput { swipeUp() }
        assertEquals(-free(), moved().y, 0.5f)
    }

    @Test
    fun twoFingersBlowTheSceneUpAndBackDownByWholePixelsOfTheArt() {
        showStage(phoneWindow)
        val started = scale()
        val window = windowSide()

        pinchBy(from = 40f, to = 160f)
        val blownUp = scale()

        assertTrue(blownUp > started)
        assertEquals(0f, blownUp % 1f, 0f)

        // The scene does not grow without end: a second pinch as wide as the first leaves it where
        // the largest size it may be drawn at has already put it.
        pinchBy(from = 40f, to = 160f)
        assertEquals(blownUp, scale(), 0f)

        pinchBy(from = 160f, to = 40f)
        val shrunk = scale()

        assertTrue(shrunk < blownUp)
        assertEquals(0f, shrunk % 1f, 0f)
        // Nor does it shrink without end: the smallest it may be drawn at is the one that still
        // covers the card, so no band of the card's own surface ever shows around the room.
        assertTrue(sceneSide() >= windowSide().toFloat())
        // Whatever a pinch does to the scene, the card itself never changes size.
        assertEquals(window, windowSide())
    }

    @Test
    fun aSceneShrunkAllTheWayLeavesTheCardTheSizeItWas() {
        showStage(phoneWindow)

        // Pinched together over and over, the scene comes down to the smallest it may be drawn at,
        // which is the smallest whole blow-up that still covers the window.
        var smallest = scale()
        repeat(4) {
            pinchBy(from = 160f, to = 40f)
            smallest = scale()
        }
        pinchBy(from = 160f, to = 40f)
        assertEquals(smallest, scale(), 0f)

        // The card stays exactly the size the area gave it, and the scene still covers it whole.
        assertEquals(with(compose.density) { phoneWindow.roundToPx() }, windowSide())
        assertTrue(sceneSide() >= windowSide().toFloat())
    }

    @Test
    fun aWindowWithRoomToSpareIsTheWholeArea() {
        showStage(tabletWindow)
        val node = stage.fetchSemanticsNode()

        assertEquals(node.size.width, node.size.height)
        assertEquals(with(compose.density) { tabletWindow.roundToPx() }, node.size.width)
        assertTrue(sceneSide() >= node.size.width.toFloat())
    }

    @Test
    fun theSizeAndThePlaceThePlayerLeftTheSceneAtComeBackAfterTheScreenIsRecreated() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            FinGameTheme(darkTheme = false) {
                Box(modifier = Modifier.size(phoneWindow)) {
                    PetStage(scene = scene)
                }
            }
        }

        pinchBy(from = 40f, to = 120f)
        stage.performTouchInput {
            swipeRight(startX = centerX - 60f, endX = centerX + 60f)
        }
        val scale = scale()
        val moved = moved()
        assertTrue(moved.x > 0f)

        restoration.emulateSavedInstanceStateRestore()

        assertEquals(scale, scale(), 0f)
        assertEquals(moved, moved())
    }
}
