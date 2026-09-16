package com.legacy.fingame.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.ui.screens.MainScreen
import com.legacy.fingame.ui.screens.PlaceholderScreen
import com.legacy.fingame.ui.screens.ShopScreen

/** Экраны приложения. Навигация простая: главный экран + оверлеи поверх него. */
enum class Screen { MAIN, SHOP, INVENTORY, QUESTS, LOCATIONS, OPTIONS }

@Composable
fun FinGameApp(
    modifier: Modifier = Modifier,
    vm: GameViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.MAIN) }

    // Системная кнопка «назад» возвращает на главный экран.
    BackHandler(enabled = screen != Screen.MAIN) { screen = Screen.MAIN }

    // Сообщения от модели (например «Куплено на N ₽») показываем одним снекбаром на всё приложение.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumeToast()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { _ ->
      Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
      ) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen"
        ) { current ->
            when (current) {
                Screen.MAIN -> MainScreen(
                    state = state,
                    onOpenShop = { screen = Screen.SHOP },
                    onOpenInventory = { screen = Screen.INVENTORY },
                    onOpenQuests = { screen = Screen.QUESTS },
                    onOpenSettings = { screen = Screen.OPTIONS },
                    onOpenLocations = { screen = Screen.LOCATIONS },
                    onPrevSubLocation = vm::prevSubLocation,
                    onNextSubLocation = vm::nextSubLocation
                )

                Screen.SHOP -> ShopScreen(
                    state = state,
                    onSelectCategory = vm::selectCategory,
                    onIncrease = vm::increaseQty,
                    onDecrease = vm::decreaseQty,
                    onToggleUnique = vm::toggleUnique,
                    onToggleGoal = vm::toggleGoal,
                    onCheckout = vm::checkout,
                    onClose = { screen = Screen.MAIN }
                )

                Screen.INVENTORY -> PlaceholderScreen("Инвентарь", "🎒", onBack = { screen = Screen.MAIN })
                Screen.QUESTS -> PlaceholderScreen("Квесты", "📜", onBack = { screen = Screen.MAIN })
                Screen.LOCATIONS -> PlaceholderScreen("Локации", "🗺️", onBack = { screen = Screen.MAIN })
                Screen.OPTIONS -> PlaceholderScreen("Опции", "⚙️", onBack = { screen = Screen.MAIN })
            }
        }
      }
    }
}
