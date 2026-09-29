package com.legacy.fingame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.GameViewModel
import com.legacy.fingame.game.economy.Budget
import com.legacy.fingame.game.economy.BudgetState
import com.legacy.fingame.game.items.Cart
import com.legacy.fingame.game.items.CartLine
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.items.ItemUse
import com.legacy.fingame.game.items.ShopShelf
import com.legacy.fingame.game.stats.StatKind
import com.legacy.fingame.ui.components.BalanceChip
import com.legacy.fingame.ui.components.EffectChip
import com.legacy.fingame.ui.components.GameDialog
import com.legacy.fingame.ui.components.GameDialogBlock
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.PillButtonMinLabelSize
import com.legacy.fingame.ui.components.PillStyle
import com.legacy.fingame.ui.components.pillButtonAutoSizeRange
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.components.compactEffectChipHeight
import com.legacy.fingame.ui.components.spriteButtonHeight
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens

/**
 * Sizes of the shop buttons on a phone; [com.legacy.fingame.ui.components.SpriteButton] enlarges
 * them on tablets, so these stay the compact values.
 */
private val CloseButtonSize = 40.dp
private val CategoryButtonSize = 56.dp
private val StarButtonSize = 30.dp
private val VariantButtonSize = 36.dp

/**
 * Size of the "−" and "+" of a counter on a stacked card, and the gap between them and the number.
 *
 * The same 48.dp the sideways card and the budget's own amount pickers use: the counter has to fit
 * the narrowest card the shop lays out twice to a row — `134.dp` inside its padding — with enough
 * left between the buttons for a quantity of two digits at any font scale the game is played at,
 * see [counterValueWidth].
 */
internal val CounterButtonSize = 48.dp
internal val CounterGap = 2.dp

/** Most variants a row shows without scrolling, see [ItemVariantRow]. */
private const val MaxUnscrolledVariants = 3

/**
 * Height of the slot a card's purchase control stands in, whichever of the three it is.
 *
 * 48.dp is the minimum interactive size Material gives a clickable `Surface(onClick = …)` — the
 * surface a [PillButton] is built on — so a card's "Добавить" comes out that tall however small its
 * own 44.dp minimum and its label would let it be. That is also taller than the [CounterButtonSize]
 * counter and the "Куплено" line, so one height for all three is what keeps the cards of a shelf
 * ending level, whatever each of them has to offer.
 */
internal val PurchaseSlotHeight = 48.dp

/**
 * Side of the tick drawn before "Куплено" on a phone at the ordinary font scale. The tick is never
 * drawn smaller than the word beside it is tall, so a larger system font or a tablet grows it too;
 * see [CheckMark].
 */
private val CheckMarkSize = 12.dp

/** Gap between the row of shelves and the "Купить" under it, and between the grid and that row. */
private val BottomBarGap = 8.dp

/**
 * Size of the coin next to an item's price. As big as the price itself is tall and no bigger: a card
 * is read name first, and a coin drawn twice the size of the number beside it made the price the
 * loudest thing on the shelf.
 */
private val PriceIconSize = 16.dp

/**
 * Smallest width a vertical shop card's cell is allowed to shrink to. Bounded from above by the
 * screen's own 16.dp padding on both sides plus [ShopScreen]'s 12.dp gap between columns: on the
 * narrowest phone the layout still promises two columns to (360.dp wide, see the
 * "Shop — 360dp, fontScale 1.3" preview), `GridCells.Adaptive` only keeps two columns while
 * `2 * minSize + 12.dp <= 328.dp` (`360.dp` minus the screen's own padding), i.e. up to `158.dp`;
 * this stays a few dp under that so rounding to pixels never tips it over into one.
 *
 * At exactly that width, a card's own 12.dp padding leaves `134.dp` for [PurchaseControl], which
 * fills all of it (see [com.legacy.fingame.ui.components.PillButton]'s `Modifier.fillMaxWidth()`)
 * rather than wrapping its label — `134.dp` minus the compact pill padding it asks for leaves
 * `110.dp` for the label itself, more than the `106.dp` "Добавить" (the widest label a card's
 * button ever shows) needs at its normal size and an ordinary font scale. A bigger font scale, or
 * a screen [ShopScreen] does not promise two columns to, is what the label's own shrink (see
 * [com.legacy.fingame.ui.components.PillButtonMinLabelSize]) is for.
 */
internal val ItemCellMinSize = 152.dp

/**
 * Sizes the shop swaps in on a short screen — a phone held sideways — where a card is laid out
 * sideways too: the sprite on one side, what is said about the item in the middle, and the way to
 * buy it on the other.
 *
 * The cell is wider, because a card now has to hold all three next to each other; everything else is
 * smaller, because the height a card may take is all the height the screen has, and a card that does
 * not fit into it is a card the player has to scroll for. Unlike [ItemCellMinSize] the cell's own
 * width is not bound by a two-column promise — a phone held sideways is wide enough on its own — so
 * it is sized purely for the three columns to have room. The sprite is not a constant at all: it is
 * given the room the screen turns out to have, see [shortScreenCardSpriteSize].
 */
internal val ShortScreenItemCellMinSize = 288.dp
private val ShortScreenStarButtonSize = 20.dp
private val ShortScreenVariantButtonSize = 32.dp
private val ShortScreenCloseButtonSize = 48.dp

/** Gap between the variant picker of a sideways card and the control that buys the item. */
private val ShortScreenPurchaseGap = 8.dp

/** Gap the shop's grid keeps between its columns and its rows, and around its content. */
internal val ShopGridGap = 12.dp
private val ShopGridContentPadding = 4.dp

/**
 * How many effect chips a stacked card fits into one row, and the gap between them.
 *
 * Two is the most a stacked card is ever given: an item of this game has at most three effects, and
 * a card as tall as it is wide has the height for two rows of chips but not for a line of three
 * across the narrowest phone. How many of the two actually go in a row is counted from the width and
 * the font scale (see [effectChipsPerRow]) — on a `134.dp` card (see [ItemCellMinSize]) a compact
 * chip at the ordinary font scale is `72.dp` wide, so it is one, and the pair is read one above the
 * other rather than squeezed or cut in half. A card laid out sideways is wider and is allowed all
 * three; see [ShortScreenEffectChipsPerRow].
 */
internal const val EffectChipsPerRow = 2

/** Gap between an item's price and the effects it has on the pet. */
private val EffectsRowTopGap = 8.dp

/** Padding between a stacked card's edge and what is in it. */
internal val ItemCardPadding = 12.dp

/** Coin sprite size used next to a sum in the shop's confirmation windows. */
private val DialogCoinSize = 20.dp

/** Size of an item's sprite on a line of the cart in [PurchaseConfirmBlock]. */
private val ConfirmLineSpriteSize = 40.dp

/**
 * How tall the list of items in the confirmation window is allowed to grow before it starts to
 * scroll, so a big cart never pushes the total and the button off the screen.
 */
private val ConfirmLinesMaxHeight = 260.dp

/** How a card exposes its purchase controls. Presentation-only, never stored in the ViewModel. */
enum class ShopItemMode { COUNTER, ADDABLE, PURCHASED }

/**
 * The window the shop currently holds open over itself, if any. Presentation-only: which window is
 * up is nothing to the rest of the game, and none of them is a purchase until the player says so.
 */
private enum class ShopWindow {
    /** Nothing is open: the player is shopping. */
    NONE,

    /** The player asked to pay and is being shown what for; see [PurchaseConfirmBlock]. */
    PURCHASE_CONFIRM,

    /** The cart costs more than the player has; see [NotEnoughMoneyBlock]. */
    NOT_ENOUGH_MONEY
}

/**
 * Shop screen: the registered items of one category at a time, and the balance they are paid from.
 *
 * Layout:
 * - Top: the player's balance and a close button, with the title of the current category under them
 *   — or, on a short screen, all three in a single line, since every line of the header is a line
 *   the cards below it do not get.
 * - Below that: a scrollable grid of item cards ([ShopItemCard]), one per item of [items]; a category
 *   with nothing on its shelves says so instead of showing an empty grid. On a short screen — a phone
 *   held sideways, see [GameDimens.isShortScreen] — the cards are laid out sideways too, sprite
 *   beside the details and the purchase controls beside both, and are sized to the room the grid
 *   turns out to have (see [shortScreenCardSpriteSize]) so that a whole row of them is read without
 *   scrolling for the bottom of a card.
 * - Bottom: one [CategoryButton] per category — the shelf's picture alone — sharing the width of a
 *   row in equal parts, so every shelf is in sight at once on any screen, and the "Купить" button,
 *   the one filled button of this screen, which shows what the cart costs and is disabled while —
 *   and only while — the cart is empty. The name of the shelf being shown is the title over the
 *   grid ([CategoryTitle]); written under the pictures as well, the four names ran together into
 *   one phrase and pushed the last shelves out of the row. "Купить" stands on a line of its own
 *   under the row — across its whole width, or at its end on a phone held sideways — so a cart price
 *   that grows longer never takes room from the shelves. A cart the player cannot afford is still
 *   worth pressing: it is answered with a window saying by how much, not with a dead button.
 * - Over all of it, once "Купить" is pressed: [PurchaseConfirmDialog], where the player sees what
 *   the cart holds, what it costs and whether it takes the running period past what was planned for
 *   it (see [com.legacy.fingame.game.economy.Budget.overspendsOf]) before the coins are gone — the
 *   plan is said out loud there, and broken there too if that is what the player wants; or
 *   [NotEnoughMoneyDialog] when the cart costs more than the balance (see
 *   [GameUiState.cartShortfall]). The money is only spent from the first of them, so a pressed
 *   button is never a spent balance, and the second one spends nothing at all: it leaves the cart,
 *   the category and the scrolled position exactly as they were.
 *
 * @param state current game state: the balance to show, which category is selected, what is in the
 *   cart with what it costs, and which items the player already owns.
 * @param items the items of [GameUiState.selectedCategory], as the catalog registered them.
 * @param cartLines everything in the cart, from every category and not just the shown one, as
 *   [com.legacy.fingame.game.items.Cart] built it; this is what the confirmation lists.
 * @param onSelectCategory called with the category whose button was pressed.
 * @param onPickVariant called with an item id and the id of the variant picked for it.
 * @param onIncrease called with the id of the item to put one more of into the cart.
 * @param onDecrease called with the id of the item to take one of out of the cart.
 * @param onToggleGoal вызывается звёздочкой карточки с товаром в выбранном на ней варианте: товар
 *   становится целью или перестаёт ею быть. Горит ли звёздочка, экран читает из
 *   [GameUiState.isGoal].
 * @param onBuy called when the player confirms the purchase and pays for the cart; returns whether
 *   the purchase actually went through. False means the cart stopped being payable between opening
 *   the confirmation and confirming it, which is answered with [NotEnoughMoneyDialog] instead of a
 *   silently ignored press.
 * @param onClose called when the close button is pressed.
 * @param modifier modifier applied to the screen root.
 * @param categories categories the shop is split into; every [ItemCategory] by default, so a
 *   section is there even before its items are.
 */
@Composable
fun ShopScreen(
    state: GameUiState,
    items: List<Item>,
    cartLines: List<CartLine>,
    onSelectCategory: (ItemCategory) -> Unit,
    onPickVariant: (String, String) -> Unit,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onToggleGoal: (ItemSelection) -> Unit,
    onBuy: () -> Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    categories: List<ItemCategory> = ItemCategory.entries
) {
    // Which window the player opened over the shop, if any. Presentation-only: a cart that is still
    // unconfirmed is no different from any other cart to the rest of the game, and a cart the player
    // was told they cannot afford is not changed by being told.
    // Kept across a turn of the device and a change of theme: the activity is rebuilt then, and a
    // window the player was reading would otherwise vanish with it while the cart behind it stayed.
    var window by rememberSaveable { mutableStateOf(ShopWindow.NONE) }
    // Cards of one screen are cut to the same pattern, so they end up the same height: room for a
    // variant picker is kept on every card of a category where any item has variants at all. Room
    // for the rows of effects is kept the same way, but that one waits until the grid has been
    // measured — how many chips go in a row is a question about the card's own width.
    val reserveVariantRow = items.any { item -> item.hasSeveralVariants }
    // A phone held sideways has no height to spare for a card that stacks its sprite over its
    // price and controls; the card is laid out sideways there instead, see [ShopItemCard].
    val isShortScreen = GameDimens.isShortScreen
    val cellMinSize = if (isShortScreen) ShortScreenItemCellMinSize else ItemCellMinSize
    val fontScale = LocalDensity.current.fontScale

    // On a tablet the content is kept to a width one glance reads and centred in whatever is left,
    // the same way the budget's and the log's are: a grid spread over a tablet's whole width stops
    // being a shelf and becomes a wall. A phone held sideways is not bounded — its width is what
    // gives a sideways card the room for two cards to a row (see [shortScreenCardSpriteSize]), and
    // capping it would fold the grid into a single column.
    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(
                    max = if (GameDimens.isTabletScreen) GameDimens.ContentMaxWidth else Dp.Unspecified
                )
                .padding(16.dp)
        ) {
            // On a screen with height to spare the header is read top to bottom: the balance and the way
            // out, then the name of the shelf below them. On a short one all three stand in a single
            // line — the height that costs is height a card would have had.
            if (isShortScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BalanceChip(balance = state.balance, depositAmount = state.depositAmount)
                    CategoryTitle(
                        category = state.selectedCategory,
                        modifier = Modifier.weight(1f)
                    )
                    SpriteButton(
                        assetPath = Sprites.CLOSE,
                        contentDescription = "Закрыть магазин",
                        onClick = onClose,
                        size = ShortScreenCloseButtonSize
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    BalanceChip(balance = state.balance, depositAmount = state.depositAmount)
                    Spacer(modifier = Modifier.weight(1f))
                    SpriteButton(
                        assetPath = Sprites.CLOSE,
                        contentDescription = "Закрыть магазин",
                        onClick = onClose,
                        size = CloseButtonSize
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                CategoryTitle(category = state.selectedCategory)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (items.isEmpty()) {
                    Text(
                        text = "В этом разделе пока нет товаров",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    // The grid is measured before the cards are built, so a sideways card can be sized
                    // to the room that is actually left between the header and the bottom bar instead of
                    // to a number that happened to fit one phone.
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val cellWidth = shopGridCellWidth(
                            gridWidth = maxWidth,
                            minCellWidth = cellMinSize,
                            gap = ShopGridGap
                        )
                        val spriteSize = shortScreenCardSpriteSize(
                            cardHeight = maxHeight - ShopGridContentPadding * 2,
                            cellWidth = cellWidth
                        )
                        // How many chips go in a row is the same question for the shelf, which reserves
                        // the rows, and for the card, which draws them — so it is asked once, here.
                        val effectChipsPerRow = effectChipsPerRow(
                            availableWidth = if (isShortScreen) {
                                shortScreenDetailsWidth(cellWidth = cellWidth, spriteSize = spriteSize)
                            } else {
                                cellWidth - ItemCardPadding * 2
                            },
                            fontScale = fontScale,
                            maxChips = if (isShortScreen) {
                                ShortScreenEffectChipsPerRow
                            } else {
                                EffectChipsPerRow
                            }
                        )
                        val reservedEffectRows = ShopShelf.effectRowsOf(items, effectChipsPerRow)

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = cellMinSize),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = ShopGridContentPadding),
                            horizontalArrangement = Arrangement.spacedBy(ShopGridGap),
                            verticalArrangement = Arrangement.spacedBy(ShopGridGap)
                        ) {
                            items(items = items, key = { item -> item.id }) { item ->
                                val mode = modeOf(item, state)
                                val inGoals = state.isGoal(item)
                                ShopItemCard(
                                    item = item,
                                    pickedVariantId = state.pickedVariantOf(item),
                                    quantity = state.quantities[item.id] ?: 0,
                                    mode = mode,
                                    reserveVariantRow = reserveVariantRow,
                                    reservedEffectRows = reservedEffectRows,
                                    effectChipsPerRow = effectChipsPerRow,
                                    horizontalLayout = isShortScreen,
                                    horizontalSpriteSize = spriteSize,
                                    inGoals = inGoals,
                                    // Купленную вещь копить не на что: звёздочки у неё нет, если
                                    // только она не осталась целью — тогда её можно снять.
                                    onToggleGoal = if (mode != ShopItemMode.PURCHASED || inGoals) {
                                        {
                                            onToggleGoal(
                                                ItemSelection(item.id, state.pickedVariantOf(item))
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    onPickVariant = { variantId -> onPickVariant(item.id, variantId) },
                                    onIncrease = { onIncrease(item.id) },
                                    onDecrease = { onDecrease(item.id) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(BottomBarGap))

            val categoryRow: @Composable (Modifier) -> Unit = { rowModifier ->
                Row(
                    modifier = rowModifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { category ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CategoryButton(
                                category = category,
                                selected = category == state.selectedCategory,
                                onClick = { onSelectCategory(category) }
                            )
                        }
                    }
                }
            }
            val buyButton: @Composable (Modifier) -> Unit = { buttonModifier ->
                PillButton(
                    text = if (state.cartPrice > 0) "Купить$DotSeparator${state.cartPrice}" else "Купить",
                    // Pressing this opens a window and nothing else: which of the two it is, is the
                    // cart's own answer (see [GameUiState.canBuyCart]). Pressing it twice in a row asks
                    // for the same window twice, which is one window.
                    onClick = {
                        window = if (state.canBuyCart) {
                            ShopWindow.PURCHASE_CONFIRM
                        } else {
                            ShopWindow.NOT_ENOUGH_MONEY
                        }
                    },
                    modifier = buttonModifier,
                    style = PillStyle.Primary,
                    enabled = state.hasCart,
                    compact = !isShortScreen
                )
                // The button is disabled, not taken away, while the cart is empty: the row of
                // shelves above it stays where it is whatever the cart holds.
            }

            // The shelves and "Купить" never share a line: a cart price that grows longer would take
            // its room from the shelves, and on a narrow phone it covered the last two of them.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(BottomBarGap),
                horizontalAlignment = Alignment.End
            ) {
                categoryRow(Modifier.fillMaxWidth())
                // Across the whole width where there is height to give it a line of its own; at the
                // end of its line on a phone held sideways, where a bar that wide would read as a
                // banner rather than a button.
                buyButton(if (isShortScreen) Modifier else Modifier.fillMaxWidth())
            }
        }
    }

    // A window whose reason is gone closes itself rather than standing there saying nothing: an
    // emptied cart leaves nothing to confirm, and a cart that became payable leaves nothing to warn
    // about. The window the player is being shown is therefore read off the cart, not off the press.
    val shownWindow = when (window) {
        ShopWindow.NONE -> ShopWindow.NONE
        ShopWindow.PURCHASE_CONFIRM -> window.takeIf { state.hasCart } ?: ShopWindow.NONE
        ShopWindow.NOT_ENOUGH_MONEY -> window.takeIf { state.cartShortfall > 0 } ?: ShopWindow.NONE
    }

    when (shownWindow) {
        ShopWindow.NONE -> Unit

        ShopWindow.PURCHASE_CONFIRM -> PurchaseConfirmDialog(
            lines = cartLines,
            total = state.cartPrice,
            balance = state.balance,
            budget = state.budget,
            // The purchase is the view model's to allow: a balance that fell between this window
            // opening and this press buys nothing and is answered by the other window instead.
            onConfirm = {
                window = if (onBuy()) ShopWindow.NONE else ShopWindow.NOT_ENOUGH_MONEY
            },
            onDismiss = { window = ShopWindow.NONE }
        )

        ShopWindow.NOT_ENOUGH_MONEY -> NotEnoughMoneyDialog(
            shortfall = state.cartShortfall,
            onDismiss = { window = ShopWindow.NONE }
        )
    }
}

/**
 * Window that asks the player to confirm a purchase: what is in the cart, item by item, and what it
 * all costs together. Nothing is paid for until the button in it is pressed.
 *
 * The cross in the corner, a tap outside the window and the system back gesture all close it and
 * leave the cart untouched, so the player can go back and change their mind about an item.
 *
 * @param lines what the cart holds, as [com.legacy.fingame.game.items.Cart] built it.
 * @param total what the whole cart costs.
 * @param balance the coins the player has, shown next to the total so the purchase can be weighed
 *   against what is left.
 * @param budget the confirmed budget of the running period, which the cart is measured against; null
 *   while no period has been planned, and then there is nothing to measure it against.
 * @param onConfirm called when the player pays for the cart.
 * @param onDismiss called when the window should be closed without buying anything.
 */
@Composable
private fun PurchaseConfirmDialog(
    lines: List<CartLine>,
    total: Int,
    balance: Int,
    budget: BudgetState?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    GameDialog(onDismiss = onDismiss) {
        PurchaseConfirmBlock(
            lines = lines,
            total = total,
            balance = balance,
            budget = budget,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}

/**
 * Window telling the player that the cart costs more than they have: by how much, and what to do
 * about it. Nothing is paid, nothing is taken out of the cart and the shop is still there behind it
 * — the player comes back to the same category, the same scrolled position and the same cart.
 *
 * Which items to put back is left to the player: the window says how much has to go, not what.
 *
 * The cross in the corner, the button in the middle, a tap outside the window and the system back
 * gesture all close it, and all four do exactly the same nothing to the cart.
 *
 * @param shortfall how many coins the cart costs over the balance; see [GameUiState.cartShortfall].
 * @param onDismiss called when the window should be closed.
 */
@Composable
private fun NotEnoughMoneyDialog(
    shortfall: Int,
    onDismiss: () -> Unit
) {
    GameDialog(onDismiss = onDismiss) {
        NotEnoughMoneyBlock(shortfall = shortfall, onDismiss = onDismiss)
    }
}

/**
 * The block the purchase window is made of: what the cart holds, what it costs, what the player is
 * left with, the button that pays for it and the cross that closes it.
 *
 * A cart that takes the period past what was planned for it says so, in as many words as there are
 * kinds of spending it overshoots — but it says it, it does not forbid it: the plan is the player's
 * own, breaking it is the player's own decision, and the button that pays goes on working. Nothing
 * is said at all while no period has been planned, since there is then no plan to break.
 *
 * @param lines what the cart holds.
 * @param total what the whole cart costs.
 * @param balance the coins the player has.
 * @param budget the confirmed budget of the running period, or null when none has been planned.
 * @param onConfirm called when the player pays for the cart.
 * @param onDismiss called when the cross is pressed.
 * @param modifier modifier applied to the block root.
 */
@Composable
private fun PurchaseConfirmBlock(
    lines: List<CartLine>,
    total: Int,
    balance: Int,
    budget: BudgetState?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The arithmetic itself lives in Budget and Cart, where it is tested; here it is only re-run when
    // the cart or the plan changes, not on every recomposition of the window.
    val overspends = remember(budget, lines) {
        Budget.overspendsOf(budget = budget, cartSpend = Cart.spendByKindOf(lines))
    }

    GameDialogBlock(
        title = "Покупка",
        closeDescription = "Отменить покупку",
        onDismiss = onDismiss,
        modifier = modifier,
        actions = {
            PillButton(
                text = "Купить",
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                style = PillStyle.Primary,
                compact = true
            )
            PillButton(
                text = "Отмена",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = PillStyle.Text,
                compact = true
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = ConfirmLinesMaxHeight)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            lines.forEach { line -> CartLineRow(line = line) }
        }

        HorizontalDivider(color = GameColors.cardStroke)

        PriceRow(
            label = "Итого",
            price = total,
            labelStyle = MaterialTheme.typography.titleMedium,
            labelColor = MaterialTheme.colorScheme.onSurface
        )
        PriceRow(
            label = "Останется",
            price = balance - total,
            labelStyle = MaterialTheme.typography.bodyMedium,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        overspends.forEach { (kind, over) ->
            Text(
                text = overspendTextOf(kind = kind, over = over),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * The block the "not enough money" window is made of: by how many coins the cart overshoots the
 * balance, what the player can do about it, and the button that takes the window away again.
 *
 * The sum is shown the way every other sum in the shop is — the coin sprite and the number — so the
 * missing coins read as the same coins the prices are written in.
 *
 * @param shortfall how many coins the cart costs over the balance.
 * @param onDismiss called when the window should be closed, by the cross or by the button.
 * @param modifier modifier applied to the block root.
 */
@Composable
private fun NotEnoughMoneyBlock(
    shortfall: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    GameDialogBlock(
        title = "Не хватает монет",
        closeDescription = "Закрыть окно",
        onDismiss = onDismiss,
        modifier = modifier,
        actions = {
            // Closing the window is the only thing to be done here, so the button does what the
            // cross does and there is no "Отмена" under it to cancel a nothing.
            PillButton(
                text = "Понятно",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = PillStyle.Primary,
                compact = true
            )
        }
    ) {
        PriceRow(
            label = "Ещё нужно",
            price = shortfall,
            labelStyle = MaterialTheme.typography.titleMedium,
            labelColor = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Покупка не прошла. Уберите часть товаров из корзины.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * One line of the cart in [PurchaseConfirmDialog]: the sprite of the picked variant, the name of the
 * item, how many of it are being bought and what that line costs.
 *
 * @param line the cart line to show.
 * @param modifier modifier applied to the row.
 */
@Composable
private fun CartLineRow(
    line: CartLine,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Sprite(
            assetPath = line.iconPath,
            contentDescription = line.item.name,
            modifier = Modifier.size(ConfirmLineSpriteSize)
        )
        Text(
            text = line.item.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (line.quantity > 1) {
            Text(
                text = "×${line.quantity}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        CoinAmount(amount = line.price)
    }
}

/**
 * A named sum in one of the shop's windows: the label on the left, the coins on the right.
 *
 * @param label what the sum is.
 * @param price the sum itself, in coins.
 * @param labelStyle text style of the label, which is how the total is told apart from the rest.
 * @param labelColor color of the label.
 * @param modifier modifier applied to the row.
 */
@Composable
private fun PriceRow(
    label: String,
    price: Int,
    labelStyle: TextStyle,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = labelStyle,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        CoinAmount(amount = price)
    }
}

/**
 * An amount of money: the coin sprite and the number of coins.
 *
 * @param amount coins to show.
 * @param modifier modifier applied to the row.
 */
@Composable
private fun CoinAmount(
    amount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Sprite(
            assetPath = Sprites.COIN,
            contentDescription = null,
            modifier = Modifier.size(DialogCoinSize)
        )
        Text(
            text = amount.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/**
 * Name of the shelf the player is looking at, over the grid of its cards. Shared between the shop's
 * two headers, which differ only in whether it stands on a line of its own or beside the balance.
 *
 * @param category the section being shown.
 * @param modifier modifier applied to the text.
 */
@Composable
private fun CategoryTitle(
    category: ItemCategory,
    modifier: Modifier = Modifier
) {
    Text(
        text = category.title(),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * One button of the row the shop's shelves are switched with: the section's sprite and, under it,
 * the underline every selected sprite button carries.
 *
 * The name of the section is not written under the picture: four names in a row ran together into
 * one phrase ("Игрушки Одежда Декор") and did not fit a narrow phone at all, which pushed the last
 * shelves out of sight behind "Купить". The shelf being shown is named by the title over the grid
 * ([CategoryTitle]) and marked here by the underline, and every button still says its name to a
 * screen reader.
 *
 * @param category the section this button switches to.
 * @param selected whether this is the section being shown.
 * @param onClick called when the button is pressed.
 * @param modifier modifier applied to the button.
 */
@Composable
private fun CategoryButton(
    category: ItemCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SpriteButton(
        assetPath = Sprites.shopCategory(category.xmlName),
        contentDescription = category.title(),
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        size = CategoryButtonSize,
        selected = selected
    )
}

/**
 * Name of a shop section as the player reads it.
 *
 * @return The Russian title of the category.
 */
internal fun ItemCategory.title(): String = when (this) {
    ItemCategory.FOOD -> "Еда"
    ItemCategory.TOYS -> "Игрушки"
    ItemCategory.CLOTHES -> "Одежда"
    ItemCategory.DECOR -> "Декор"
    ItemCategory.OTHER -> "Другое"
}

/**
 * Picks the purchase control an item gets.
 *
 * @param item the item the card is built for.
 * @param state current game state, for what the player already owns.
 * @return [ShopItemMode.COUNTER] for an item that is used up and bought again, i.e. food;
 * [ShopItemMode.PURCHASED] for anything already owned in the picked variant, since buying it again
 * would pay for nothing; [ShopItemMode.ADDABLE] otherwise.
 */
private fun modeOf(item: Item, state: GameUiState): ShopItemMode = when {
    item.category.use == ItemUse.CONSUMED -> ShopItemMode.COUNTER
    state.ownedCountOf(item) > 0 -> ShopItemMode.PURCHASED
    else -> ShopItemMode.ADDABLE
}

/**
 * Single shop item card: the icon of the picked variant with an "add to goals" star toggle, the
 * item's name and price, what the item does to the pet, a variant picker for items offered in
 * several variants, and a purchase control that depends on [mode].
 *
 * What the item does to the pet is read right under its price — the two questions the player weighs
 * against each other stand together, before the choice of variant and before the button that acts on
 * both — and in the very chips the inventory shows the same effects in, so an item looks the same
 * before and after it is bought. An item that does nothing (clothes, decorations) says nothing.
 *
 * The card is built out of slots of a fixed height rather than out of whatever its item happens to
 * need, so the cards of one shelf line up with each other instead of ending at three different
 * heights: an item with one variant keeps the room the variant picker takes (as long as its
 * neighbours have one, see [reserveVariantRow]), and the purchase control sits in a slot as tall as
 * the tallest of them.
 *
 * On a screen with no height to spare ([horizontalLayout]) the card is laid out sideways instead of
 * stacked, and it is not two columns but three: the sprite, then the name, the price and the effects
 * beside it, then the variant picker over the purchase control on the far side. A stacked card is as
 * tall as the grid made it wide, and on a phone held sideways that is taller than the screen; spread
 * across the width instead, a whole row of cards is read without scrolling for the bottom of it —
 * which is what the sprite gives way for, see [shortScreenCardSpriteSize].
 *
 * @param item the item to show.
 * @param pickedVariantId variant the item is shown and would be bought in.
 * @param quantity how many of this item are in the cart; shown by [ShopItemMode.COUNTER].
 * @param mode which purchase control to show; see [ShopItemMode].
 * @param reserveVariantRow whether the card keeps room for a variant picker even when its item has
 *   but one variant; true when any item on the same shelf has several of them.
 * @param reservedEffectRows how many rows of effect chips the card keeps room for, however many its
 *   own item fills; the shelf's own answer, see [ShopShelf.effectRowsOf]. Zero keeps no room at all.
 * @param effectChipsPerRow how many chips one of those rows holds; the same number the shelf counted
 *   its rows with, so what is reserved is what is drawn.
 * @param horizontalLayout whether to lay the card out sideways, sprite beside the details rather
 *   than above them; true on a short screen, see [GameDimens.isShortScreen].
 * @param horizontalSpriteSize side of the sprite in that sideways layout, as the room the screen has
 *   left allows (see [shortScreenCardSpriteSize]); ignored by the stacked layout, where the sprite is
 *   as wide as the card itself.
 * @param inGoals горит ли звёздочка: выбранный вариант товара — цель игрока.
 * @param onToggleGoal нажатие на звёздочку, или null, когда звёздочки у карточки нет (купленная
 *   вещь, которая не цель).
 * @param onPickVariant called with the id of the variant the player picked.
 * @param onIncrease called to put one more of this item into the cart.
 * @param onDecrease called to take one of this item out of the cart.
 * @param modifier modifier applied to the card surface.
 */
@Composable
private fun ShopItemCard(
    item: Item,
    pickedVariantId: String,
    quantity: Int,
    mode: ShopItemMode,
    reserveVariantRow: Boolean,
    reservedEffectRows: Int,
    effectChipsPerRow: Int,
    horizontalLayout: Boolean,
    horizontalSpriteSize: Dp,
    inGoals: Boolean,
    onToggleGoal: (() -> Unit)?,
    onPickVariant: (String) -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GameColors.cardStroke),
        shadowElevation = 2.dp
    ) {
        if (horizontalLayout) {
            Row(
                modifier = Modifier.padding(ShortScreenCardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ShortScreenRailGap)
            ) {
                ItemSprite(
                    item = item,
                    pickedVariantId = pickedVariantId,
                    inGoals = inGoals,
                    onToggleGoals = onToggleGoal,
                    starButtonSize = ShortScreenStarButtonSize,
                    modifier = Modifier.size(horizontalSpriteSize)
                )

                Column(modifier = Modifier.weight(1f)) {
                    ItemNameText(name = item.name)

                    Spacer(modifier = Modifier.height(4.dp))

                    ItemPriceRow(price = item.price)

                    ItemEffectsRow(
                        item = item,
                        reservedRows = reservedEffectRows,
                        chipsPerRow = effectChipsPerRow,
                        alignment = Alignment.Start,
                        modifier = Modifier.padding(top = EffectsRowTopGap)
                    )
                }

                Column(
                    modifier = Modifier.width(ShortScreenPurchaseWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ShortScreenPurchaseGap)
                ) {
                    ItemVariantRow(
                        item = item,
                        pickedVariantId = pickedVariantId,
                        reserveVariantRow = reserveVariantRow,
                        onPickVariant = onPickVariant,
                        contentAlignment = Alignment.Center,
                        buttonSize = ShortScreenVariantButtonSize
                    )

                    PurchaseControl(
                        mode = mode,
                        quantity = quantity,
                        onIncrease = onIncrease,
                        onDecrease = onDecrease,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(ItemCardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ItemSprite(
                    item = item,
                    pickedVariantId = pickedVariantId,
                    inGoals = inGoals,
                    onToggleGoals = onToggleGoal,
                    starButtonSize = StarButtonSize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                ItemNameText(name = item.name)

                Spacer(modifier = Modifier.height(4.dp))

                ItemPriceRow(price = item.price)

                ItemEffectsRow(
                    item = item,
                    reservedRows = reservedEffectRows,
                    chipsPerRow = effectChipsPerRow,
                    alignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = EffectsRowTopGap)
                )

                ItemVariantRow(
                    item = item,
                    pickedVariantId = pickedVariantId,
                    reserveVariantRow = reserveVariantRow,
                    onPickVariant = onPickVariant,
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                PurchaseControl(
                    mode = mode,
                    quantity = quantity,
                    onIncrease = onIncrease,
                    onDecrease = onDecrease,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Name of an item as [ShopItemCard] shows it, cut to one line: shared between the portrait and the
 * [ShopItemCard]'s short-screen layout, which differ only in what sits around this text.
 *
 * @param name the item's name.
 * @param modifier modifier applied to the text.
 */
@Composable
private fun ItemNameText(
    name: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = name,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Sprite of the picked variant, with the "add to goals" star toggle sitting over its top-right
 * corner. Shared between [ShopItemCard]'s two layouts, which only differ in the size the sprite box
 * and the star button are given.
 *
 * @param item the item the sprite belongs to.
 * @param pickedVariantId variant of it the card is showing, the one whose icon is drawn.
 * @param inGoals whether the star is currently toggled on.
 * @param onToggleGoals called when the star is pressed, or null when the card has no star at all.
 * @param starButtonSize size of the star toggle.
 * @param modifier modifier applied to the sprite box; this is where the caller sizes it.
 */
@Composable
private fun ItemSprite(
    item: Item,
    pickedVariantId: String,
    inGoals: Boolean,
    onToggleGoals: (() -> Unit)?,
    starButtonSize: Dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Sprite(
            assetPath = item.getIconPath(pickedVariantId),
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize()
        )
        if (onToggleGoals != null) {
            SpriteButton(
                assetPath = if (inGoals) Sprites.STAR_ON else Sprites.STAR_OFF,
                contentDescription = if (inGoals) "Убрать из целей" else "Добавить в цели",
                onClick = onToggleGoals,
                size = starButtonSize,
                showIndicator = false,
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Top
            )
        }
    }
}

/**
 * An item's price: the coin sprite and the number of coins, written no larger than the item's own
 * name — a card is read name first and price second, and a price set in 22.sp against a 13.sp name
 * had that the wrong way round. Shared between [ShopItemCard]'s two layouts.
 *
 * @param price the item's price, in coins.
 * @param modifier modifier applied to the row.
 */
@Composable
private fun ItemPriceRow(
    price: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Sprite(
            assetPath = Sprites.COIN,
            contentDescription = null,
            modifier = Modifier.size(PriceIconSize)
        )
        Text(
            text = price.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * What an item does to the pet, shown on its card before it is bought: one compact
 * [com.legacy.fingame.ui.components.EffectChip] per effect, the very chips the inventory shows the
 * same item's effects in once it is owned.
 *
 * The chips wrap by whole chips and never by halves: a row holds [chipsPerRow] of them, so an item
 * with three effects reads as two and one on a stacked card rather than as two and a cropped third,
 * and as all three at once on a card laid out sideways. The block keeps the height of [reservedRows]
 * rows whatever its own item fills, which is how a cake and the apple beside it end at the same
 * height; a shelf that reserved nothing (clothes, decorations) renders nothing at all, so no gap is
 * left for it either.
 *
 * @param item the item whose effects to show.
 * @param reservedRows rows of chips the shelf keeps room for; see [ShopShelf.effectRowsOf].
 * @param chipsPerRow how many chips go in one row, the same number [reservedRows] was counted with.
 * @param alignment where the chips sit within the card's width: centred under a vertical card,
 *   left-aligned beside the sprite of a horizontal one.
 * @param modifier modifier applied to the block.
 */
@Composable
private fun ItemEffectsRow(
    item: Item,
    reservedRows: Int,
    chipsPerRow: Int,
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier
) {
    if (reservedRows <= 0) return

    val reservedHeight = compactEffectChipHeight() * reservedRows +
            EffectChipGap * (reservedRows - 1)

    Box(modifier = modifier.heightIn(min = reservedHeight)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(EffectChipGap, alignment),
            verticalArrangement = Arrangement.spacedBy(EffectChipGap),
            maxItemsInEachRow = chipsPerRow
        ) {
            item.effects.forEach { (stat, value) ->
                EffectChip(stat = stat, value = value, compact = true)
            }
        }
    }
}

/**
 * Variant picker of an item's card, reserving its row's height even for an item with a single
 * variant so the cards of one shelf ([reserveVariantRow]) end at the same height; renders nothing
 * at all when the shelf keeps no room for it, so no gap is left for it either.
 *
 * @param item the item the variants belong to.
 * @param pickedVariantId variant currently picked, highlighted in the row.
 * @param reserveVariantRow whether the shelf this card sits on keeps room for this row at all.
 * @param onPickVariant called with the id of the variant the player picked.
 * @param contentAlignment where the row of variant buttons sits within the reserved height; centered
 *   under a vertical card, and over the purchase control of a horizontal one.
 * @param modifier modifier applied to the row's box.
 * @param buttonSize size of one variant button; smaller on a card laid out sideways, where the row
 *   stands in a column whose height a card cannot spare.
 */
@Composable
private fun ItemVariantRow(
    item: Item,
    pickedVariantId: String,
    reserveVariantRow: Boolean,
    onPickVariant: (String) -> Unit,
    contentAlignment: Alignment,
    modifier: Modifier = Modifier,
    buttonSize: Dp = VariantButtonSize
) {
    if (!reserveVariantRow) return

    Box(
        modifier = modifier.height(spriteButtonHeight(buttonSize)),
        contentAlignment = contentAlignment
    ) {
        if (item.hasSeveralVariants) {
            // Three touch areas of 48.dp are 144.dp, wider than the 134.dp of the narrowest card, so
            // a short row is let out a few dp into the card's padding (the drawn sprites are
            // narrower than their areas and stay inside); a longer one scrolls.
            Row(
                modifier = if (item.variantIds.size <= MaxUnscrolledVariants) {
                    Modifier.wrapContentWidth(align = Alignment.CenterHorizontally, unbounded = true)
                } else {
                    Modifier.horizontalScroll(rememberScrollState())
                },
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item.variantIds.forEach { variantId ->
                    SpriteButton(
                        assetPath = item.getIconPath(variantId),
                        contentDescription = "Вариант «$variantId»",
                        onClick = { onPickVariant(variantId) },
                        size = buttonSize,
                        selected = variantId == pickedVariantId
                    )
                }
            }
        }
    }
}

/**
 * The control an item's card offers to buy it with, picked by [mode]: a +/- counter for something
 * bought again and again, an "Добавить"/"Убрать" toggle for a one-off, or a plain "Куплено" label
 * for something already owned. Shared between [ShopItemCard]'s two layouts.
 *
 * Sits in a slot no shorter than the counter, so a card with a button ends at the same height as one
 * with a counter. Its caller passes `Modifier.fillMaxWidth()`, so a card's own width is what
 * [ShopItemMode.ADDABLE]'s [PillButton] stretches into instead of wrapping its own label — see
 * [ItemCellMinSize] for why that is what lets a card's button hold its label at one line without
 * shrinking on an ordinary phone — and what [ShopItemMode.COUNTER] shares out between its two
 * buttons and the quantity between them, see [counterValueWidth].
 *
 * @param mode which control to show.
 * @param quantity how many of the item are in the cart; shown by [ShopItemMode.COUNTER].
 * @param onIncrease called to put one more of the item into the cart.
 * @param onDecrease called to take one of the item out of the cart.
 * @param modifier modifier applied to the control's box.
 */
@Composable
private fun PurchaseControl(
    mode: ShopItemMode,
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.heightIn(min = GameDimens.buttonSize(PurchaseSlotHeight)),
        contentAlignment = Alignment.Center
    ) {
        when (mode) {
            ShopItemMode.COUNTER -> {
                val counterStyle = MaterialTheme.typography.labelLarge
                val (counterMinSize, counterMaxSize) = pillButtonAutoSizeRange(
                    minLabelSize = PillButtonMinLabelSize,
                    styleFontSize = counterStyle.fontSize,
                    density = LocalDensity.current
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CounterGap)
                ) {
                    // Nothing to take out of the cart is a button that says so by going grey,
                    // rather than one that darkens under the finger and then does nothing.
                    SpriteButton(
                        assetPath = Sprites.MINUS,
                        contentDescription = "Уменьшить количество",
                        onClick = onDecrease,
                        size = CounterButtonSize,
                        enabled = quantity > 0,
                        showIndicator = false
                    )
                    // The number is given everything the buttons leave (see [counterValueWidth])
                    // and shrinks within it instead of wrapping: a two-digit quantity used to be
                    // boxed into 28.dp and came out as one digit over another. `softWrap` is left
                    // alone on purpose — turning it off is what stops `autoSize` measuring the
                    // width at all, see PillButton's own note.
                    Text(
                        text = quantity.toString(),
                        style = counterStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = counterMinSize,
                            maxFontSize = counterMaxSize
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    SpriteButton(
                        assetPath = Sprites.PLUS,
                        contentDescription = "Увеличить количество",
                        onClick = onIncrease,
                        size = CounterButtonSize,
                        showIndicator = false
                    )
                }
            }

            // Stretched to the width PurchaseControl's own caller was given (a shop card's full
            // content width), rather than wrapping the label: that width is what autoSize's label
            // shrink is measured against, and a card is wide before the label is ever asked to
            // shrink at all. `compact` trims the padding around the label to match, so the width is
            // spent on the label rather than the margin around it.
            // Tonal and not Primary: a filled green button on every card of a grid is a green
            // lattice, and the one filled button of this screen is the "Купить" that pays for the
            // whole cart. A card's button adds to it, it does not conclude anything.
            ShopItemMode.ADDABLE -> PillButton(
                text = if (quantity > 0) "Убрать" else "Добавить",
                onClick = { if (quantity > 0) onDecrease() else onIncrease() },
                modifier = Modifier.fillMaxWidth(),
                style = PillStyle.Tonal,
                compact = true
            )

            // Not a button at all: there is nothing left to press here, and a disabled pill on a
            // cream card reads as a broken button rather than as an answer. A tick and a word in the
            // colour good news is written in say the same thing and take nothing away from the
            // cards around it that still have something to offer.
            ShopItemMode.PURCHASED -> {
                val purchasedStyle = MaterialTheme.typography.labelMedium
                val (purchasedMinSize, purchasedMaxSize) = pillButtonAutoSizeRange(
                    minLabelSize = PillButtonMinLabelSize,
                    styleFontSize = purchasedStyle.fontSize,
                    density = LocalDensity.current
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CheckMark(color = GameColors.success, textStyle = purchasedStyle)
                    // The word takes what the tick leaves and shrinks into it whole: on the
                    // narrowest card with the system text turned up, the tick at the size of the
                    // word and the word itself come to a few dp more than the card has.
                    Text(
                        text = "Куплено",
                        modifier = Modifier.weight(1f, fill = false),
                        style = purchasedStyle,
                        color = GameColors.success,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = purchasedMinSize,
                            maxFontSize = purchasedMaxSize
                        )
                    )
                }
            }
        }
    }
}

/**
 * A tick, drawn rather than typed: the game's pixel font has no glyph for one, and a character
 * borrowed from a fallback font would stand out of the pixel line it sits in.
 *
 * As big as the word it stands before: [CheckMarkSize] enlarged for a tablet, or the size of
 * [textStyle]'s font when that comes out larger — a system font of 1.3 grows the word and would
 * otherwise leave a tick smaller than the letters beside it.
 *
 * @param color color of the stroke.
 * @param textStyle style of the word the tick stands before.
 * @param modifier modifier applied to the drawing.
 */
@Composable
private fun CheckMark(
    color: Color,
    textStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    val fontSize = with(LocalDensity.current) { textStyle.fontSize.toDp() }
    val side = maxOf(GameDimens.buttonSize(CheckMarkSize), fontSize)

    Canvas(modifier = modifier.size(side)) {
        val tick = Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.55f)
            lineTo(size.width * 0.4f, size.height * 0.85f)
            lineTo(size.width * 0.9f, size.height * 0.2f)
        }
        drawPath(
            path = tick,
            color = color,
            style = Stroke(width = size.width * 0.18f, cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )
    }
}

/**
 * Food the previews go shopping with, standing in for what the catalog reads from the assets: two
 * items the pet feels two ways about and one — the cake — it feels three, which is the shelf where
 * a card's row of effects has to wrap onto a second row while the cards stay the same height.
 */
private val PreviewItems = listOf(
    Item(
        id = "apple",
        name = "Яблоко",
        price = 15,
        category = ItemCategory.FOOD,
        variantIds = listOf("default"),
        declaredEffects = mapOf(StatKind.HUNGER to 20, StatKind.HEALTH to 5)
    ),
    Item(
        id = "fish",
        name = "Рыбка",
        price = 25,
        category = ItemCategory.FOOD,
        variantIds = listOf("default"),
        declaredEffects = mapOf(StatKind.HUNGER to 35, StatKind.PLEASURE to 5)
    ),
    Item(
        id = "cake",
        name = "Пирожное",
        price = 40,
        category = ItemCategory.FOOD,
        variantIds = listOf("default"),
        declaredEffects = mapOf(
            StatKind.HUNGER to 30,
            StatKind.PLEASURE to 15,
            StatKind.HEALTH to -5
        )
    )
)

/**
 * Clothes the previews go shopping with: the category where a thing really does come in several
 * sorts, so this is what the variant picker on a card is previewed on — and the one the pet feels
 * nothing about, so it is also what a shelf with no room kept for effects is previewed on.
 */
private val PreviewClothes = listOf(
    Item(
        id = "hat",
        name = "Шляпа",
        price = 100,
        category = ItemCategory.CLOTHES,
        variantIds = listOf("black", "white", "violet")
    ),
    Item(
        id = "scarf",
        name = "Шарф",
        price = 80,
        category = ItemCategory.CLOTHES,
        variantIds = listOf("red", "green")
    )
)

/** The cart the previews go to the till with: two apples and one fish. */
private val PreviewCartLines = listOf(
    CartLine(item = PreviewItems[0], variantId = "default", quantity = 2),
    CartLine(item = PreviewItems[1], variantId = "default", quantity = 1)
)

/** Preview of [ShopScreen] in the light theme. */
@Preview(name = "Shop — Light", showBackground = true)
@Composable
private fun ShopScreenLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(
                    balance = 300,
                    quantities = mapOf("apple" to 2),
                    cartPrice = 30
                ),
                items = PreviewItems,
                cartLines = listOf(PreviewCartLines.first()),
                onSelectCategory = {},
                onPickVariant = { _, _ -> },
                onIncrease = {},
                onDecrease = {},
                onToggleGoal = {},
                onBuy = { true },
                onClose = {}
            )
        }
    }
}

/**
 * Preview of [ShopScreen] in the light theme, landscape orientation: the clothes rack, i.e. the
 * shelf whose cards carry a variant picker over their purchase control, which is the tallest that
 * third column of a sideways card ever gets — and it still has to end above the bottom bar.
 */
@Preview(name = "Shop — Landscape", showBackground = true, widthDp = 891, heightDp = 411)
@Composable
private fun ShopScreenLandscapePreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(
                    balance = 300,
                    selectedCategory = ItemCategory.CLOTHES,
                    quantities = mapOf("fish" to 1),
                    cartPrice = 25
                ),
                items = PreviewClothes,
                cartLines = listOf(PreviewCartLines.last()),
                onSelectCategory = {},
                onPickVariant = { _, _ -> },
                onIncrease = {},
                onDecrease = {},
                onToggleGoal = {},
                onBuy = { true },
                onClose = {}
            )
        }
    }
}

/**
 * Preview of [ShopScreen] at 320.dp, the narrowest width the game's own preview widths go down to.
 * [ItemCellMinSize] is wide enough that this falls back to one column rather than two — checked here
 * so that fallback itself stays free of a card's "Добавить" button breaking the word across two lines.
 */
@Preview(name = "Shop — Narrow 320dp", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun ShopScreenNarrowPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(
                    balance = 300,
                    selectedCategory = ItemCategory.CLOTHES
                ),
                items = PreviewClothes,
                cartLines = emptyList(),
                onSelectCategory = {},
                onPickVariant = { _, _ -> },
                onIncrease = {},
                onDecrease = {},
                onToggleGoal = {},
                onBuy = { true },
                onClose = {}
            )
        }
    }
}

/**
 * Preview of [ShopScreen] at 360.dp with the system font scaled up to 1.3x — two columns, a card
 * only as wide as [ItemCellMinSize] promises, and a font scale big enough that a card's "Добавить"
 * button no longer fits at its normal size and has to shrink (see
 * [com.legacy.fingame.ui.components.PillButtonMinLabelSize]) rather than being cropped.
 */
@Preview(
    name = "Shop — 360dp, fontScale 1.3",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    fontScale = 1.3f
)
@Composable
private fun ShopScreenMediumFontScalePreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(
                    balance = 300,
                    selectedCategory = ItemCategory.CLOTHES
                ),
                items = PreviewClothes,
                cartLines = emptyList(),
                onSelectCategory = {},
                onPickVariant = { _, _ -> },
                onIncrease = {},
                onDecrease = {},
                onToggleGoal = {},
                onBuy = { true },
                onClose = {}
            )
        }
    }
}

/**
 * Preview of [ShopScreen] at 320.dp with the system font scaled up to 1.5x — one column this time
 * (see [ShopScreenNarrowPreview]), but the top of both the width and the font scale range together,
 * checked so their combination is no exception to a card's "Добавить" button never being cropped.
 */
@Preview(
    name = "Shop — 320dp, fontScale 1.5",
    showBackground = true,
    widthDp = 320,
    heightDp = 640,
    fontScale = 1.5f
)
@Composable
private fun ShopScreenNarrowLargeFontScalePreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(
                    balance = 300,
                    selectedCategory = ItemCategory.CLOTHES
                ),
                items = PreviewClothes,
                cartLines = emptyList(),
                onSelectCategory = {},
                onPickVariant = { _, _ -> },
                onIncrease = {},
                onDecrease = {},
                onToggleGoal = {},
                onBuy = { true },
                onClose = {}
            )
        }
    }
}

/**
 * Preview of the food shelf at 360.dp, i.e. at the narrowest a card is ever laid out two to a row:
 * the apple and the fish move two of the pet's bars each and the cake moves three, so the shelf
 * keeps two rows of chips for every card of it and the three cards end at the same height.
 */
@Preview(name = "Shop — Food shelf 360dp", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun ShopScreenFoodShelfPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(balance = 300),
                items = PreviewItems,
                cartLines = emptyList(),
                onSelectCategory = {},
                onPickVariant = { _, _ -> },
                onIncrease = {},
                onDecrease = {},
                onToggleGoal = {},
                onBuy = { true },
                onClose = {}
            )
        }
    }
}

/**
 * The same food shelf with the system font scaled up: two chips still sit side by side in a card's
 * width, so the cake's three effects still read as two rows and not as three.
 */
@Preview(
    name = "Shop — Food shelf 360dp, fontScale 1.3",
    showBackground = true,
    widthDp = 360,
    heightDp = 720,
    fontScale = 1.3f
)
@Composable
private fun ShopScreenFoodShelfLargeFontScalePreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(balance = 300),
                items = PreviewItems,
                cartLines = emptyList(),
                onSelectCategory = {},
                onPickVariant = { _, _ -> },
                onIncrease = {},
                onDecrease = {},
                onToggleGoal = {},
                onBuy = { true },
                onClose = {}
            )
        }
    }
}

/**
 * The food shelf at the narrowest width and the biggest font scale the game is played at, with the
 * cart already holding as many apples as it takes (see
 * [com.legacy.fingame.game.GameViewModel.MAX_ITEM_QUANTITY]): the case the counter was broken in.
 * The quantity has to stand on one line between the two buttons, and all three have to stay inside
 * the card — a two-digit number used to come out as one digit above the other.
 */
@Preview(
    name = "Shop — Full cart 360dp, fontScale 1.3",
    showBackground = true,
    widthDp = 360,
    heightDp = 720,
    fontScale = 1.3f
)
@Composable
private fun ShopScreenFullCartPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(
                    balance = 3_000,
                    quantities = mapOf(
                        "apple" to GameViewModel.MAX_ITEM_QUANTITY,
                        "fish" to 9
                    ),
                    cartPrice = 1_710
                ),
                items = PreviewItems,
                cartLines = PreviewCartLines,
                onSelectCategory = {},
                onPickVariant = { _, _ -> },
                onIncrease = {},
                onDecrease = {},
                onToggleGoal = {},
                onBuy = { true },
                onClose = {}
            )
        }
    }
}

/**
 * Preview of the clothes rack at the same width: a shelf where nothing touches the pet's bars, so
 * the cards end right under the variant picker with no room kept for effects at all.
 */
@Preview(name = "Shop — Clothes shelf 360dp", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun ShopScreenClothesShelfPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(balance = 300, selectedCategory = ItemCategory.CLOTHES),
                items = PreviewClothes,
                cartLines = emptyList(),
                onSelectCategory = {},
                onPickVariant = { _, _ -> },
                onIncrease = {},
                onDecrease = {},
                onToggleGoal = {},
                onBuy = { true },
                onClose = {}
            )
        }
    }
}

/**
 * Preview of the food shelf on a phone held sideways, where a card is laid out sideways too: the
 * sprite, then the name with the price and all three of the cake's effects in one row beside it,
 * then the counter that buys it. The whole first row of cards has to be readable here without
 * scrolling — the bottom of every card above the bar with the categories and "Купить".
 */
@Preview(name = "Shop — Landscape food", showBackground = true, widthDp = 891, heightDp = 411)
@Composable
private fun ShopScreenLandscapeFoodPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(balance = 300, quantities = mapOf("cake" to 1), cartPrice = 40),
                items = PreviewItems,
                cartLines = emptyList(),
                onSelectCategory = {},
                onPickVariant = { _, _ -> },
                onIncrease = {},
                onDecrease = {},
                onToggleGoal = {},
                onBuy = { true },
                onClose = {}
            )
        }
    }
}

/**
 * The same shelf on the smallest phone held sideways this game is played on: less height for the
 * card and less width for the row it stands in, so the sprite gives way on both counts (see
 * [shortScreenCardSpriteSize]) and the card still ends above the bottom bar.
 */
@Preview(name = "Shop — Landscape 800x360", showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun ShopScreenLandscapeNarrowPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(balance = 300, quantities = mapOf("cake" to 1), cartPrice = 40),
                items = PreviewItems,
                cartLines = emptyList(),
                onSelectCategory = {},
                onPickVariant = { _, _ -> },
                onIncrease = {},
                onDecrease = {},
                onToggleGoal = {},
                onBuy = { true },
                onClose = {}
            )
        }
    }
}

/**
 * The clothes rack at that same smallest size: the shelf whose cards are the tallest, on the screen
 * with the least height to give them.
 */
@Preview(
    name = "Shop — Landscape 800x360, clothes",
    showBackground = true,
    widthDp = 800,
    heightDp = 360
)
@Composable
private fun ShopScreenLandscapeNarrowClothesPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(balance = 300, selectedCategory = ItemCategory.CLOTHES),
                items = PreviewClothes,
                cartLines = emptyList(),
                onSelectCategory = {},
                onPickVariant = { _, _ -> },
                onIncrease = {},
                onDecrease = {},
                onToggleGoal = {},
                onBuy = { true },
                onClose = {}
            )
        }
    }
}

/** Preview of [ShopScreen] in the dark theme: the clothes rack, with a hat the player already owns. */
@Preview(name = "Shop — Dark", showBackground = true)
@Composable
private fun ShopScreenDarkPreview() {
    FinGameTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(
                    balance = 120,
                    selectedCategory = ItemCategory.CLOTHES,
                    quantities = mapOf("fish" to 1),
                    cartPrice = 25,
                    owned = mapOf(ItemSelection("hat", "black") to 1)
                ),
                items = PreviewClothes,
                cartLines = listOf(PreviewCartLines.last()),
                onSelectCategory = {},
                onPickVariant = { _, _ -> },
                onIncrease = {},
                onDecrease = {},
                onToggleGoal = {},
                onBuy = { true },
                onClose = {}
            )
        }
    }
}

/**
 * Preview of the window the player confirms a purchase in, with no period planned to weigh the cart
 * against — and so with nothing said about a plan.
 */
@Preview(name = "Shop — Purchase confirmation", showBackground = true)
@Composable
private fun PurchaseConfirmDialogPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PurchaseConfirmBlock(
                lines = PreviewCartLines,
                total = PreviewCartLines.sumOf { line -> line.price },
                balance = 300,
                budget = null,
                onConfirm = {},
                onDismiss = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/**
 * The plan the previews shop against: a period where the necessities were given 40 coins, 30 of
 * them are spent already, and the cart holds 55 more of food — 45 past the plan — while the 100
 * planned for the rest of it is untouched, so exactly one of the two lines has anything to say.
 */
private val PreviewBudget = BudgetState(
    plannedMust = 40,
    plannedWant = 100,
    plannedSavings = 60,
    plannedDeposit = 0,
    spentMust = 30,
    spentWant = 0,
    startDay = 0
)

/**
 * Preview of the same window over a period whose plan the cart breaks, at the narrowest width and
 * the biggest font scale the game is played at: the red line has to fit under the sums without
 * pushing the button that pays off the window, and that button has to still be a button.
 */
@Preview(
    name = "Shop — Purchase over the plan, 360dp, fontScale 1.3",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    fontScale = 1.3f
)
@Composable
private fun PurchaseOverThePlanDialogPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PurchaseConfirmBlock(
                lines = PreviewCartLines,
                total = PreviewCartLines.sumOf { line -> line.price },
                balance = 300,
                budget = PreviewBudget,
                onConfirm = {},
                onDismiss = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/**
 * How many coins the previews of the "not enough money" window are short of: a sum of two digits,
 * the widest the line ever gets on the shop's own prices.
 */
private const val PreviewShortfall = 55

/** Preview of the window that says the cart costs more than the player has. */
@Preview(name = "Shop — Not enough money", showBackground = true, widthDp = 360, heightDp = 400)
@Composable
private fun NotEnoughMoneyDialogPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            NotEnoughMoneyBlock(
                shortfall = PreviewShortfall,
                onDismiss = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/**
 * Preview of the "not enough money" window at the narrowest width the game's previews go down to,
 * with the system font scaled up: the case its title, its sum and the sentence under them have to
 * survive without a word being cut short — the text wraps, the button's label shrinks (see
 * [com.legacy.fingame.ui.components.PillButtonMinLabelSize]).
 */
@Preview(
    name = "Shop — Not enough money, 320dp, fontScale 1.3",
    showBackground = true,
    widthDp = 320,
    heightDp = 400,
    fontScale = 1.3f
)
@Composable
private fun NotEnoughMoneyDialogNarrowPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            NotEnoughMoneyBlock(
                shortfall = PreviewShortfall,
                onDismiss = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
