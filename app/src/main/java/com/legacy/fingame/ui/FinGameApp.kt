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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legacy.fingame.FinGameApplication
import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.game.Screen
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.screens.AnimalSelectScreen
import com.legacy.fingame.ui.screens.MainScreen
import com.legacy.fingame.ui.screens.PlaceholderScreen
import com.legacy.fingame.ui.screens.ShopScreen

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
 * [MainScreen], [ShopScreen] and the [PlaceholderScreen] instances for the yet-unspecified
 * sections (inventory, quests, locations, options), based on [GameUiState.screen].
 *
 * @param modifier modifier applied to the root surface.
 * @param vm view model providing [GameUiState] and the navigation/action callbacks passed down
 *   to each screen; defaults to a [GameViewModel] scoped to this composable, restoring the
 *   player's game from [FinGameApplication.playerPreferences].
 */
@Composable
fun FinGameApp(
    modifier: Modifier = Modifier,
    vm: GameViewModel = viewModel(
        factory = GameViewModel.factory(
            (LocalContext.current.applicationContext as FinGameApplication).playerPreferences
        )
    )
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val application = LocalContext.current.applicationContext as FinGameApplication
    val animalRegistry = application.animalRegistry

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
                        // TODO: DemoContent.petAge is a placeholder; pass the pet's real age stage once
                        //  the pet growth logic exists.
                        petSpritePath = animalRegistry.getIdleSpritePath(
                            animalId = pet.animalId,
                            variantId = pet.variantId,
                            age = DemoContent.petAge
                        )
                    )

                    Screen.SHOP -> ShopScreen(
                        state = state,
                        onSelectCategory = vm::selectCategory,
                        onIncrease = vm::increaseQty,
                        onDecrease = vm::decreaseQty,
                        onClose = vm::closeScreen
                    )

                    Screen.INVENTORY -> PlaceholderScreen("Инвентарь", Sprites.INVENTORY, vm::closeScreen)
                    Screen.QUESTS -> PlaceholderScreen("Квесты", Sprites.QUESTS, vm::closeScreen)
                    Screen.LOCATIONS -> PlaceholderScreen("Локации", Sprites.LOCATIONS, vm::closeScreen)
                    Screen.OPTIONS -> PlaceholderScreen("Опции", Sprites.SETTINGS, vm::closeScreen)
                }
            }
        }
    }
}
