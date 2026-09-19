package com.legacy.fingame.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legacy.fingame.FinGameApplication
import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.game.Screen
import com.legacy.fingame.game.items.Inventory
import com.legacy.fingame.game.scene.GameScene
import com.legacy.fingame.game.scene.SceneSprite
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.screens.AnimalSelectScreen
import com.legacy.fingame.ui.screens.InventoryScreen
import com.legacy.fingame.ui.screens.MainScreen
import com.legacy.fingame.ui.screens.PlaceholderScreen
import com.legacy.fingame.ui.screens.ShopScreen
import kotlinx.coroutines.delay

/**
 * How many times the pet is checked on per [PetStats.TICK_MILLIS] while the player is watching it, so
 * a bar never sits a whole tick behind what the clock says.
 */
private const val TICK_POLLS_PER_TICK = 10L

/**
 * Root composable of the app: hosts the current [Screen] behind a fade animation and wires
 * back-press handling to close any non-main screen.
 *
 * Until the player has picked a pet — i.e. on the very first launch — the whole app is replaced by
 * [AnimalSelectScreen]; the choice is saved right away, so the following launches go straight to
 * the game with the pet, and the sub-location it was left in, already restored by [vm]. A saved
 * choice that is no longer present in the animal data (e.g. the animal or its variant was renamed
 * or removed) can't be played, so the player picks again — but is told that the pet is gone
 * instead of being greeted as a newcomer, and the saved choice is only replaced once a new pet is
 * actually picked.
 *
 * Layout: a full-size [Surface] with an [AnimatedContent] that cross-fades between
 * [MainScreen], [ShopScreen], [InventoryScreen] and the [PlaceholderScreen] instances for the
 * yet-unspecified sections (quests, locations, options), based on [GameUiState.screen].
 *
 * While there is a pet to look after, this is also where its life goes on: a loop asks
 * [GameViewModel.tick] to catch up with the clock, so the stat bars fall and the pet grows up in
 * front of the player instead of only between launches.
 *
 * @param modifier modifier applied to the root surface.
 * @param vm view model providing [GameUiState] and the navigation/action callbacks passed down
 *   to each screen; defaults to a [GameViewModel] scoped to this composable, restoring the
 *   player's game from [FinGameApplication.playerPreferences] and pricing the shop out of
 *   [FinGameApplication.itemRegistry].
 */
@Composable
fun FinGameApp(
    modifier: Modifier = Modifier,
    vm: GameViewModel = viewModel(
        factory = with(LocalContext.current.applicationContext as FinGameApplication) {
            GameViewModel.factory(store = playerPreferences, catalog = itemRegistry)
        }
    )
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val application = LocalContext.current.applicationContext as FinGameApplication
    val animalRegistry = application.animalRegistry
    val itemRegistry = application.itemRegistry

    val savedSelection = state.selection
    val pet = savedSelection?.takeIf { animalRegistry.hasVariant(it.animalId, it.variantId) }

    BackHandler(enabled = state.screen != Screen.MAIN) { vm.closeScreen() }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (pet == null) {
            AnimalSelectScreen(
                animals = animalRegistry.getAllAnimals(),
                onSelect = vm::selectAnimal,
                previousPetLost = savedSelection != null
            )
        } else {
            // The pet lives on while the player watches it: the loop keeps asking the view model to
            // catch up with the clock, so the bars go down and the pet grows without the player
            // having to leave the screen and come back. A poll that finds nothing to do costs
            // nothing, hence the interval well below one decay tick.
            LaunchedEffect(vm) {
                while (true) {
                    vm.tick()
                    delay(PetStats.TICK_MILLIS / TICK_POLLS_PER_TICK)
                }
            }

            AnimatedContent(
                targetState = state.screen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen"
            ) { current ->
                when (current) {
                    Screen.MAIN -> MainScreen(
                        state = state,
                        onOpenScreen = vm::openScreen,
                        onPrevSubLocation = vm::prevSubLocation,
                        onNextSubLocation = vm::nextSubLocation,
                        onClaimDailyBonus = { vm.claimDailyBonus() },
                        scene = GameScene.of(
                            background = SceneSprite(
                                assetPath = Sprites.locationBackground(state.subLocationIndex),
                                description = null
                            ),
                            pet = SceneSprite(
                                assetPath = animalRegistry.getIdleSpritePath(
                                    animalId = pet.animalId,
                                    variantId = pet.variantId,
                                    age = state.petAge
                                ),
                                description = "Питомец"
                            ),
                            worn = state.worn,
                            catalog = itemRegistry
                        )
                    )

                    Screen.SHOP -> ShopScreen(
                        state = state,
                        items = itemRegistry.getItemsByCategory(state.selectedCategory),
                        onSelectCategory = vm::selectCategory,
                        onPickVariant = vm::pickVariant,
                        onIncrease = vm::increaseQty,
                        onDecrease = vm::decreaseQty,
                        onBuy = { vm.buyCart() },
                        onClose = vm::closeScreen
                    )

                    Screen.INVENTORY -> InventoryScreen(
                        entries = Inventory.entriesOf(
                            owned = state.owned,
                            worn = state.worn,
                            catalog = itemRegistry
                        ),
                        onUseItem = { selection -> vm.useItem(selection) },
                        onToggleWorn = { selection -> vm.toggleWorn(selection) },
                        onClose = vm::closeScreen
                    )

                    Screen.QUESTS -> PlaceholderScreen("Квесты", Sprites.QUESTS, vm::closeScreen)
                    Screen.LOCATIONS -> PlaceholderScreen("Локации", Sprites.LOCATIONS, vm::closeScreen)
                    Screen.OPTIONS -> PlaceholderScreen("Опции", Sprites.SETTINGS, vm::closeScreen)
                }
            }
        }
    }
}
