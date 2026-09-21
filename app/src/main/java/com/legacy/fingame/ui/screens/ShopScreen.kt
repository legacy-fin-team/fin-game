package com.legacy.fingame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.items.CartLine
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.items.ItemUse
import com.legacy.fingame.ui.components.BalanceChip
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.components.spriteButtonHeight
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens

/**
 * Sizes of the shop buttons on a phone; [com.legacy.fingame.ui.components.SpriteButton] enlarges
 * them on tablets, so these stay the compact values.
 */
private val CloseButtonSize = 64.dp
private val CategoryButtonSize = 56.dp
private val CounterButtonSize = 48.dp
private val StarButtonSize = 40.dp
private val VariantButtonSize = 36.dp
private val PriceIconSize = 32.dp
private val ItemCellMinSize = 170.dp

/**
 * Sizes the shop swaps in on a short screen — a phone held sideways — where a card is laid out
 * sideways too: the sprite on one side, everything that is said about the item on the other.
 *
 * The cell is wider, because a card now has to hold the sprite and the widest of the purchase
 * controls next to each other; the sprite and the coin are smaller, because the height a card may
 * take is all the height the screen has.
 */
private val ShortScreenItemCellMinSize = 264.dp
private val ShortScreenCardSpriteSize = 96.dp
private val ShortScreenStarButtonSize = 28.dp
private val ShortScreenPriceIconSize = 24.dp

/** Gap between the shelves and the column of controls standing beside them on a short screen. */
private val ShortScreenRailGap = 12.dp

/** Sizes of the window that asks the player to confirm the purchase. */
private val ConfirmMaxWidth = 320.dp
private val ConfirmCloseButtonSize = 44.dp
private val ConfirmLineSpriteSize = 40.dp
private val ConfirmCoinSize = 20.dp

/**
 * How tall the list of items in the confirmation window is allowed to grow before it starts to
 * scroll, so a big cart never pushes the total and the button off the screen.
 */
private val ConfirmLinesMaxHeight = 260.dp

/** How a card exposes its purchase controls. Presentation-only, never stored in the ViewModel. */
enum class ShopItemMode { COUNTER, ADDABLE, PURCHASED }

/**
 * Shop screen: the registered items of one category at a time, and the balance they are paid from.
 *
 * Layout:
 * - Top: the player's balance and a close button.
 * - Below that: the title of the current category, then a scrollable grid of item cards
 *   ([ShopItemCard]), one per item of [items]; a category with nothing on its shelves says so
 *   instead of showing an empty grid. On a short screen — a phone held sideways, see
 *   [GameDimens.isShortScreen] — the cards are laid out sideways too, sprite beside the details
 *   rather than above them, so a card's height stops depending on how wide the grid made it.
 * - Bottom: one button per category in a row that scrolls horizontally (so the row can hold any
 *   number of categories) next to the "Купить" button, which shows what the cart costs and stays
 *   disabled while the cart is empty or the player cannot afford it.
 * - Over all of it, once "Купить" is pressed: [PurchaseConfirmDialog], where the player sees what
 *   the cart holds and what it costs before the coins are gone. The money is only spent from there,
 *   so a pressed button is never a spent balance.
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
 * @param onBuy called when the player confirms the purchase and pays for the cart.
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
    onBuy: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    categories: List<ItemCategory> = ItemCategory.entries
) {
    // Whether the player asked to pay and is being shown what for. Presentation-only: a cart that is
    // still unconfirmed is no different from any other cart to the rest of the game.
    var confirming by remember { mutableStateOf(false) }
    // Cards of one screen are cut to the same pattern, so they end up the same height: room for a
    // variant picker is kept on every card of a category where any item has variants at all.
    val reserveVariantRow = items.any { item -> item.hasSeveralVariants }
    // A phone held sideways has no height to spare for a card that stacks its sprite over its
    // price and controls; the card is laid out sideways there instead, see [ShopItemCard].
    val isShortScreen = GameDimens.isShortScreen

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            BalanceChip(balance = state.balance)
            Spacer(modifier = Modifier.weight(1f))
            SpriteButton(
                assetPath = Sprites.CLOSE,
                contentDescription = "Закрыть магазин",
                onClick = onClose,
                size = CloseButtonSize
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = state.selectedCategory.title(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

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
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(
                        minSize = if (isShortScreen) ShortScreenItemCellMinSize else ItemCellMinSize
                    ),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = items, key = { item -> item.id }) { item ->
                        ShopItemCard(
                            item = item,
                            pickedVariantId = state.pickedVariantOf(item),
                            quantity = state.quantities[item.id] ?: 0,
                            mode = modeOf(item, state),
                            reserveVariantRow = reserveVariantRow,
                            horizontalLayout = isShortScreen,
                            onPickVariant = { variantId -> onPickVariant(item.id, variantId) },
                            onIncrease = { onIncrease(item.id) },
                            onDecrease = { onDecrease(item.id) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    SpriteButton(
                        assetPath = Sprites.shopCategory(category.xmlName),
                        contentDescription = category.title(),
                        onClick = { onSelectCategory(category) },
                        size = CategoryButtonSize,
                        selected = category == state.selectedCategory
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            PillButton(
                text = if (state.cartPrice > 0) "Купить · ${state.cartPrice}" else "Купить",
                onClick = { confirming = true },
                enabled = state.canBuyCart
            )
        }
    }

    // The window goes away by itself if the cart stops being payable while it is open — there would
    // be nothing left to confirm.
    if (confirming && state.canBuyCart) {
        PurchaseConfirmDialog(
            lines = cartLines,
            total = state.cartPrice,
            balance = state.balance,
            onConfirm = {
                confirming = false
                onBuy()
            },
            onDismiss = { confirming = false }
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
 * @param onConfirm called when the player pays for the cart.
 * @param onDismiss called when the window should be closed without buying anything.
 */
@Composable
private fun PurchaseConfirmDialog(
    lines: List<CartLine>,
    total: Int,
    balance: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // The window is sized by what is in it, not by the platform's dialog width, so the card keeps
    // the proportions of the rest of the game's windows.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            PurchaseConfirmBlock(
                lines = lines,
                total = total,
                balance = balance,
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }
    }
}

/**
 * The block the purchase window is made of: what the cart holds, what it costs, what the player is
 * left with, the button that pays for it and the cross that closes it.
 *
 * The cross sits on the top-right corner and hangs half-way over the edge of the card, the way the
 * inventory's item window wears it.
 *
 * @param lines what the cart holds.
 * @param total what the whole cart costs.
 * @param balance the coins the player has.
 * @param onConfirm called when the player pays for the cart.
 * @param onDismiss called when the cross is pressed.
 * @param modifier modifier applied to the block root.
 */
@Composable
private fun PurchaseConfirmBlock(
    lines: List<CartLine>,
    total: Int,
    balance: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Half of the cross hangs outside the card, so the card keeps that much room around itself.
    val overhang = GameDimens.buttonSize(ConfirmCloseButtonSize) / 2

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .padding(top = overhang, end = overhang)
                .widthIn(max = ConfirmMaxWidth),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GameColors.cardStroke),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Покупка",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

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

                PillButton(text = "Купить", onClick = onConfirm)
            }
        }

        SpriteButton(
            assetPath = Sprites.CLOSE,
            contentDescription = "Отменить покупку",
            onClick = onDismiss,
            size = ConfirmCloseButtonSize,
            modifier = Modifier.align(Alignment.TopEnd)
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
 * A named sum in [PurchaseConfirmDialog]: the label on the left, the coins on the right.
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
            modifier = Modifier.size(ConfirmCoinSize)
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
 * Name of a shop section as the player reads it.
 *
 * @return The Russian title of the category.
 */
private fun ItemCategory.title(): String = when (this) {
    ItemCategory.FOOD -> "Еда"
    ItemCategory.TOYS -> "Игрушки"
    ItemCategory.CLOTHES -> "Одежда"
    ItemCategory.DECOR -> "Декор"
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
 * item's name and price, a variant picker for items offered in several variants, and a purchase
 * control that depends on [mode].
 *
 * The card is built out of slots of a fixed height rather than out of whatever its item happens to
 * need, so the cards of one shelf line up with each other instead of ending at three different
 * heights: an item with one variant keeps the room the variant picker takes (as long as its
 * neighbours have one, see [reserveVariantRow]), and the purchase control sits in a slot as tall as
 * the tallest of them.
 *
 * On a screen with no height to spare ([horizontalLayout]) the card is laid out sideways instead of
 * stacked: the sprite on the left, the name, price, variant picker and purchase control in a column
 * to its right. A vertical card is as tall as the grid is wide, and on a phone held sideways that is
 * taller than the screen; turned on its side, the card fits into the height the screen has left once
 * the bottom bar with the cart and the "Купить" button is given its share.
 *
 * @param item the item to show.
 * @param pickedVariantId variant the item is shown and would be bought in.
 * @param quantity how many of this item are in the cart; shown by [ShopItemMode.COUNTER].
 * @param mode which purchase control to show; see [ShopItemMode].
 * @param reserveVariantRow whether the card keeps room for a variant picker even when its item has
 *   but one variant; true when any item on the same shelf has several of them.
 * @param horizontalLayout whether to lay the card out sideways, sprite beside the details rather
 *   than above them; true on a short screen, see [GameDimens.isShortScreen].
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
    horizontalLayout: Boolean,
    onPickVariant: (String) -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: "in goals" star state is local UI state; move it to the goals logic/data layer once the team defines where goals actually live.
    var inGoals by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GameColors.cardStroke),
        shadowElevation = 2.dp
    ) {
        if (horizontalLayout) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ShortScreenRailGap)
            ) {
                ItemSprite(
                    item = item,
                    pickedVariantId = pickedVariantId,
                    inGoals = inGoals,
                    onToggleGoals = { inGoals = !inGoals },
                    starButtonSize = ShortScreenStarButtonSize,
                    modifier = Modifier.size(ShortScreenCardSpriteSize)
                )

                Column(modifier = Modifier.weight(1f)) {
                    ItemNameText(name = item.name)

                    Spacer(modifier = Modifier.height(4.dp))

                    ItemPriceRow(price = item.price, iconSize = ShortScreenPriceIconSize)

                    ItemVariantRow(
                        item = item,
                        pickedVariantId = pickedVariantId,
                        reserveVariantRow = reserveVariantRow,
                        onPickVariant = onPickVariant,
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PurchaseControl(
                        mode = mode,
                        quantity = quantity,
                        onIncrease = onIncrease,
                        onDecrease = onDecrease
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ItemSprite(
                    item = item,
                    pickedVariantId = pickedVariantId,
                    inGoals = inGoals,
                    onToggleGoals = { inGoals = !inGoals },
                    starButtonSize = StarButtonSize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                ItemNameText(name = item.name)

                Spacer(modifier = Modifier.height(4.dp))

                ItemPriceRow(price = item.price, iconSize = PriceIconSize)

                ItemVariantRow(
                    item = item,
                    pickedVariantId = pickedVariantId,
                    reserveVariantRow = reserveVariantRow,
                    onPickVariant = onPickVariant,
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                PurchaseControl(
                    mode = mode,
                    quantity = quantity,
                    onIncrease = onIncrease,
                    onDecrease = onDecrease
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
        style = MaterialTheme.typography.bodyLarge,
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
 * @param onToggleGoals called when the star is pressed.
 * @param starButtonSize size of the star toggle.
 * @param modifier modifier applied to the sprite box; this is where the caller sizes it.
 */
@Composable
private fun ItemSprite(
    item: Item,
    pickedVariantId: String,
    inGoals: Boolean,
    onToggleGoals: () -> Unit,
    starButtonSize: Dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Sprite(
            assetPath = item.getIconPath(pickedVariantId),
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize()
        )
        SpriteButton(
            assetPath = if (inGoals) Sprites.STAR_ON else Sprites.STAR_OFF,
            contentDescription = if (inGoals) "Убрать из целей" else "Добавить в цели",
            onClick = onToggleGoals,
            size = starButtonSize,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

/**
 * An item's price: the coin sprite and the number of coins, set larger than the item's name so
 * the price is the first thing read on a card. Shared between [ShopItemCard]'s two layouts, which
 * only differ in how big the coin sprite is drawn.
 *
 * @param price the item's price, in coins.
 * @param iconSize size of the coin sprite.
 * @param modifier modifier applied to the row.
 */
@Composable
private fun ItemPriceRow(
    price: Int,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Sprite(
            assetPath = Sprites.COIN,
            contentDescription = null,
            modifier = Modifier.size(iconSize)
        )
        Text(
            text = price.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
 *   under a vertical card, left-aligned next to the sprite of a horizontal one.
 * @param modifier modifier applied to the row's box.
 */
@Composable
private fun ItemVariantRow(
    item: Item,
    pickedVariantId: String,
    reserveVariantRow: Boolean,
    onPickVariant: (String) -> Unit,
    contentAlignment: Alignment,
    modifier: Modifier = Modifier
) {
    if (!reserveVariantRow) return

    Box(
        modifier = modifier.height(spriteButtonHeight(VariantButtonSize)),
        contentAlignment = contentAlignment
    ) {
        if (item.hasSeveralVariants) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item.variantIds.forEach { variantId ->
                    SpriteButton(
                        assetPath = item.getIconPath(variantId),
                        contentDescription = "Вариант «$variantId»",
                        onClick = { onPickVariant(variantId) },
                        size = VariantButtonSize,
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
 * Sits in a slot as tall as the counter, its tallest form, so a card with a button ends at the same
 * height as one with a counter.
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
        modifier = modifier.heightIn(min = spriteButtonHeight(CounterButtonSize)),
        contentAlignment = Alignment.Center
    ) {
        when (mode) {
            ShopItemMode.COUNTER -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpriteButton(
                    assetPath = Sprites.MINUS,
                    contentDescription = "Уменьшить количество",
                    onClick = { if (quantity > 0) onDecrease() },
                    size = CounterButtonSize
                )
                Text(
                    text = quantity.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(28.dp)
                )
                SpriteButton(
                    assetPath = Sprites.PLUS,
                    contentDescription = "Увеличить количество",
                    onClick = onIncrease,
                    size = CounterButtonSize
                )
            }

            ShopItemMode.ADDABLE -> PillButton(
                text = if (quantity > 0) "Убрать" else "Добавить",
                onClick = { if (quantity > 0) onDecrease() else onIncrease() }
            )

            ShopItemMode.PURCHASED -> Surface(
                shape = RoundedCornerShape(50),
                color = GameColors.disabledContainer
            ) {
                Text(
                    text = "Куплено",
                    style = MaterialTheme.typography.labelLarge,
                    color = GameColors.disabledContent,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}

/** Food the previews go shopping with, standing in for what the catalog reads from the assets. */
private val PreviewItems = listOf(
    Item(
        id = "apple",
        name = "Яблоко",
        price = 15,
        category = ItemCategory.FOOD,
        variantIds = listOf("default")
    ),
    Item(
        id = "fish",
        name = "Рыбка",
        price = 25,
        category = ItemCategory.FOOD,
        variantIds = listOf("default")
    ),
    Item(
        id = "cake",
        name = "Пирожное",
        price = 40,
        category = ItemCategory.FOOD,
        variantIds = listOf("default")
    )
)

/**
 * Clothes the previews go shopping with: the category where a thing really does come in several
 * sorts, so this is what the variant picker on a card is previewed on.
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
                onBuy = {},
                onClose = {}
            )
        }
    }
}

/**
 * Preview of [ShopScreen] in the light theme, landscape orientation: the case the cards have to
 * survive without a card's height running past the screen and under the bottom bar.
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
                onBuy = {},
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
                onBuy = {},
                onClose = {}
            )
        }
    }
}

/** Preview of the window the player confirms a purchase in. */
@Preview(name = "Shop — Purchase confirmation", showBackground = true)
@Composable
private fun PurchaseConfirmDialogPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PurchaseConfirmBlock(
                lines = PreviewCartLines,
                total = PreviewCartLines.sumOf { line -> line.price },
                balance = 300,
                onConfirm = {},
                onDismiss = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
