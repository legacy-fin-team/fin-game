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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.game.Screen
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.screens.MainScreen
import com.legacy.fingame.ui.screens.PlaceholderScreen
import com.legacy.fingame.ui.screens.SettingsScreen
import com.legacy.fingame.ui.screens.ShopScreen

/**
 * Root composable of the app: hosts the current [Screen] behind a fade animation and wires
 * back-press handling to close any non-main screen.
 *
 * Layout: a full-size [Surface] with an [AnimatedContent] that cross-fades between
 * [MainScreen], [ShopScreen], [SettingsScreen] and the [PlaceholderScreen] instances for the
 * yet-unspecified sections (inventory, quests, locations, adult mode), based on
 * [GameUiState.screen].
 *
 * @param modifier modifier applied to the root surface.
 * @param vm view model providing [GameUiState] and the navigation/action callbacks passed down
 *   to each screen; defaults to a [GameViewModel] scoped to this composable.
 */
@Composable
fun FinGameApp(
    modifier: Modifier = Modifier,
    vm: GameViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.screen != Screen.MAIN) { vm.closeScreen() }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                    onNextSubLocation = vm::nextSubLocation
                )

                Screen.SHOP -> ShopScreen(
                    state = state,
                    onSelectCategory = vm::selectCategory,
                    onIncrease = vm::increaseQty,
                    onDecrease = vm::decreaseQty,
                    onClose = vm::closeScreen
                )

                Screen.OPTIONS -> SettingsScreen(
                    settings = state.settings,
                    onSettingsChanged = vm::updateSettings,
                    onOpenAdultMode = { vm.openScreen(Screen.ADULT_MODE) },
                    onResetProgress = vm::resetProgress,
                    onBack = vm::closeScreen
                )

                Screen.ADULT_MODE -> PlaceholderScreen(
                    "Режим взрослого",
                    Sprites.SETTINGS,
                    vm::closeScreen
                )

                Screen.INVENTORY -> PlaceholderScreen("Инвентарь", Sprites.INVENTORY, vm::closeScreen)
                Screen.QUESTS -> PlaceholderScreen("Квесты", Sprites.QUESTS, vm::closeScreen)
                Screen.LOCATIONS -> PlaceholderScreen("Локации", Sprites.LOCATIONS, vm::closeScreen)
            }
        }
    }
}
