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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legacy.fingame.DemoMode
import com.legacy.fingame.FinGameApplication
import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.game.Screen
import com.legacy.fingame.game.hints.HintKeys
import com.legacy.fingame.game.items.Cart
import com.legacy.fingame.game.items.Goals
import com.legacy.fingame.game.items.Inventory
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.quests.QuestBoard
import com.legacy.fingame.game.scene.GameScene
import com.legacy.fingame.game.scene.SceneSprite
import com.legacy.fingame.game.settings.AudioManager
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.ui.components.ScreenHint
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.screens.AdultLockScreen
import com.legacy.fingame.ui.screens.AdultScreen
import com.legacy.fingame.ui.screens.AnimalSelectScreen
import com.legacy.fingame.ui.screens.BudgetScreen
import com.legacy.fingame.ui.screens.HelpScreen
import com.legacy.fingame.ui.screens.InventoryScreen
import com.legacy.fingame.ui.screens.LogScreen
import com.legacy.fingame.ui.screens.MainScreen
import com.legacy.fingame.ui.screens.QuestsScreen
import com.legacy.fingame.ui.screens.SettingsScreen
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
 * [AnimalSelectScreen], where the animal, its variant and its name are chosen in two steps; the
 * choice is saved right away, so the following launches go straight to the game with the pet, its
 * name, and the sub-location it was left in, already restored by [vm]. A saved
 * choice that is no longer present in the animal data (e.g. the animal or its variant was renamed
 * or removed) can't be played, so the player picks again — but is told that the pet is gone
 * instead of being greeted as a newcomer, and the saved choice is only replaced once a new pet is
 * actually picked.
 *
 * Layout: a full-size [Surface] with an [AnimatedContent] that cross-fades between
 * [MainScreen], [ShopScreen], [InventoryScreen], [QuestsScreen], [BudgetScreen], [LogScreen],
 * [SettingsScreen], [HelpScreen], the lock in front of the adult mode ([AdultLockScreen]) and the
 * adult hub ([AdultScreen]), based on [GameUiState.screen].
 *
 * While there is a pet to look after, this is also where its life goes on: a loop asks
 * [GameViewModel.tick] to catch up with the clock, so the stat bars fall and the pet grows up in
 * front of the player instead of only between launches. In a demo build ([DemoMode.ENABLED]) the
 * main screen also gets the button that pushes that same clock forward, so a demo can show a day of
 * the pet's life without waiting one out.
 *
 * The game is explained in small steps rather than in one welcome window: the first time the player
 * lands on the pet selection screen, on the main screen and on each of the other screens, a
 * [ScreenHint] about that screen is shown on top of it, once. Which one is due is decided by
 * [HintKeys.pending] out of [GameUiState.hintsSeen]; closing it is remembered by
 * [GameViewModel.markHintSeen]. Only one hint is ever on screen, and the pet selection hint and the
 * main screen one never follow each other without the player picking a pet in between.
 *
 * @param modifier modifier applied to the root surface.
 * @param vm view model providing [GameUiState] and the navigation/action callbacks passed down
 *   to each screen; defaults to a [GameViewModel] scoped to this composable, restoring the
 *   player's game from [FinGameApplication.playerPreferences] and pricing the shop out of
 *   [FinGameApplication.itemRegistry].
 * @param onPlaySound plays a sound effect by its key (see [AudioManager.playSound]); the app's
 *   audio lives in the activity, so it is handed in rather than looked up. Silent by default, e.g.
 *   in previews.
 * @param onPlayAnimalSound plays a random sound of the animal with the given id (see
 *   [AudioManager.playAnimalSound]) when the player pats the pet. Silent by default.
 */
@Composable
fun FinGameApp(
    modifier: Modifier = Modifier,
    vm: GameViewModel = viewModel(
        factory = with(LocalContext.current.applicationContext as FinGameApplication) {
            GameViewModel.factory(
                store = playerPreferences,
                catalog = itemRegistry,
                questCatalog = questRegistry,
                careTuning = careTuning
            )
        }
    ),
    onPlaySound: (String) -> Unit = {},
    onPlayAnimalSound: (animalId: String) -> Unit = {}
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val application = LocalContext.current.applicationContext as FinGameApplication
    val animalRegistry = application.animalRegistry
    // Предметы игры и свои предметы взрослого вместе (см. [GameViewModel.catalog]).
    val itemRegistry = vm.catalog
    // Квесты игры и свои квесты взрослого вместе (см. [GameViewModel.questCatalog]).
    val questRegistry = vm.questCatalog

    val savedSelection = state.selection
    val pet = savedSelection?.takeIf { animalRegistry.hasVariant(it.animalId, it.variantId) }
    val pendingHint = HintKeys.pending(
        hasPet = pet != null,
        screen = state.screen,
        seen = state.hintsSeen
    )?.let(application.hintRegistry::find)

    // «Назад» из режима взрослого и из замка ведёт в настройки (см. [GameViewModel.closeScreen]).
    BackHandler(enabled = state.screen != Screen.MAIN) { vm.closeScreen() }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (pet == null) {
            AnimalSelectScreen(
                animals = animalRegistry.getAllAnimals(),
                onSelect = { selection, name -> vm.selectAnimal(selection, name) },
                previousPetMissingFromData = savedSelection != null
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

            // The stage the pet is drawn at, not the raw age: a pet keeps growing by the calendar
            // past its last painted stage, and the clothes on it must not ask for a stage there are
            // no pictures of.
            val petStage = animalRegistry.getAgeStage(pet.animalId, state.petAge)

            AnimatedContent(
                targetState = state.screen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen"
            ) { current ->
                when (current) {
                    Screen.MAIN -> MainScreen(
                        state = state,
                        onOpenScreen = vm::openScreen,
                        onClaimDailyBonus = { vm.claimDailyBonus() },
                        // Only a demo build gets the time button; the player waits for the pet to
                        // get hungry and to grow up, as the game is meant to be played.
                        onFastForward = if (DemoMode.ENABLED) {
                            { vm.fastForward(DemoMode.FAST_FORWARD_MILLIS) }
                        } else {
                            null
                        },
                        scene = GameScene.of(
                            background = SceneSprite(
                                assetPath = Sprites.locationBackground(state.subLocationIndex),
                                description = null
                            ),
                            pet = SceneSprite(
                                assetPath = animalRegistry.getIdleSpritePath(
                                    animalId = pet.animalId,
                                    variantId = pet.variantId,
                                    age = petStage
                                ),
                                description = "Питомец"
                            ),
                            animalId = pet.animalId,
                            worn = state.worn,
                            catalog = itemRegistry,
                            animalAge = petStage
                        ),
                        // Patting the pet only makes it happy to see: hearts on the screen and a
                        // sound, no stats and no money.
                        onPetTap = { onPlayAnimalSound(pet.animalId) },
                        // Цели показывают те же цены, что и магазин (см. GameViewModel.shopCatalog).
                        goals = Goals.linesOf(goals = state.goals, catalog = vm.shopCatalog),
                        onOpenGoal = vm::openGoal
                    )

                    Screen.SHOP -> ShopScreen(
                        state = state,
                        // Цены — с надбавкой ухода (см. [GameViewModel.shopCatalog]), поверх
                        // предметов игры и своих предметов взрослого.
                        items = vm.shopCatalog.getItemsByCategory(state.selectedCategory),
                        // Пустой раздел — «Другое» без своих предметов — в магазине не показывается.
                        categories = ItemCategory.entries.filter { category ->
                            category == state.selectedCategory ||
                                itemRegistry.getItemsByCategory(category).isNotEmpty()
                        },
                        cartLines = Cart.linesOf(
                            quantities = state.quantities,
                            pickedVariants = state.pickedVariants,
                            catalog = vm.shopCatalog
                        ),
                        onSelectCategory = vm::selectCategory,
                        onPickVariant = vm::pickVariant,
                        onIncrease = vm::increaseQty,
                        onDecrease = vm::decreaseQty,
                        onToggleGoal = { selection -> vm.toggleGoal(selection) },
                        onBuy = vm::buyCart,
                        onClose = vm::closeScreen
                    )

                    Screen.INVENTORY -> InventoryScreen(
                        entries = Inventory.entriesOf(
                            owned = state.owned,
                            worn = state.worn,
                            catalog = itemRegistry
                        ),
                        stats = state.stats,
                        onUseItem = { selection -> vm.useItem(selection) },
                        onToggleWorn = { selection -> vm.toggleWorn(selection) },
                        onClose = vm::closeScreen
                    )

                    Screen.QUESTS -> QuestsScreen(
                        entries = remember(state.quests, state.customQuests) {
                            QuestBoard.entriesOf(questRegistry, state.quests)
                        },
                        currentMillis = vm::nowMillis,
                        onStart = { questId -> vm.startQuest(questId) },
                        onChoose = { questId, index -> vm.chooseQuestOption(questId, index) },
                        onAdvance = { questId -> vm.advanceQuest(questId) },
                        onRestart = { questId -> vm.restartQuest(questId) },
                        onClose = vm::closeScreen,
                        balance = state.balance,
                        depositAmount = state.depositAmount,
                        canRestart = DemoMode.ENABLED,
                        ignoreDelays = vm.ignoreQuestDelays
                    )

                    Screen.BUDGET -> BudgetScreen(
                        state = state,
                        onDraftChange = vm::updateBudgetDraft,
                        onConfirmBudget = { vm.confirmBudget() },
                        onCloseDepositEarly = { vm.closeDepositEarly() },
                        onClaimDailyBonus = { vm.claimDailyBonus() },
                        onOpenLog = { vm.openScreen(Screen.LOG) },
                        onClose = vm::closeScreen
                    )

                    Screen.LOG -> LogScreen(
                        log = state.moneyLog,
                        onOpenBudget = { vm.openScreen(Screen.BUDGET) },
                        onClose = vm::closeScreen,
                        balance = state.balance,
                        depositAmount = state.depositAmount
                    )
                    Screen.OPTIONS -> SettingsScreen(
                        settings = state.settings,
                        onSettingsChanged = vm::updateSettings,
                        onOpenAdultMode = { vm.openScreen(Screen.ADULT_LOCK) },
                        onOpenHelp = { vm.openScreen(Screen.HELP) },
                        // A reset makes the player new again in every sense: the screen hints come
                        // back with it, starting with the one on the pet selection screen.
                        onResetProgress = vm::resetProgress,
                        onBack = vm::closeScreen
                    )

                    Screen.ADULT_LOCK -> AdultLockScreen(
                        onSolved = vm::enterAdultMode,
                        onClose = vm::closeScreen
                    )

                    Screen.ADULT_MODE -> AdultScreen(
                        state = state,
                        // Цены целей — те же, что видит ребёнок (см. [GameViewModel.shopCatalog]).
                        itemCatalog = vm.shopCatalog,
                        questCatalog = questRegistry,
                        onAddItem = vm::addCustomItem,
                        onRemoveItem = vm::removeCustomItem,
                        onAddQuest = vm::addCustomQuest,
                        onRemoveQuest = vm::removeCustomQuest,
                        onUpdateQuest = vm::updateCustomQuest,
                        onEnableQuest = vm::enableQuest,
                        onApproveCheck = vm::approveQuestCheck,
                        onRejectCheck = vm::rejectQuestCheck,
                        onAdjustMoney = vm::adjustBalanceByAdult,
                        onRewardsSeen = vm::markRewardUsagesSeen,
                        onClose = vm::exitAdultMode
                    )

                    Screen.HELP -> HelpScreen(
                        entries = application.helpRegistry.getEntries(),
                        hints = application.hintRegistry.getHints(),
                        hintsAlreadyReset = state.hintsSeen.isEmpty(),
                        onResetHints = vm::resetHints,
                        onClose = { vm.openScreen(Screen.OPTIONS) }
                    )
                }
            }

        }

        if (pendingHint != null) {
            ScreenHint(
                hint = pendingHint,
                onDismiss = { vm.markHintSeen(pendingHint.key) }
            )
        }
    }
}
