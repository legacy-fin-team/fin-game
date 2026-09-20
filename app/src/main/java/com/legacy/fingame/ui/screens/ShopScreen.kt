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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.items.ItemUse
import com.legacy.fingame.ui.components.BalanceChip
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors

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

/** How a card exposes its purchase controls. Presentation-only, never stored in the ViewModel. */
enum class ShopItemMode { COUNTER, ADDABLE, PURCHASED }

/**
 * Shop screen: the registered items of one category at a time, and the balance they are paid from.
 *
 * Layout:
 * - Top: the player's balance and a close button.
 * - Below that: the title of the current category, then a scrollable grid of item cards
 *   ([ShopItemCard]), one per item of [items]; a category with nothing on its shelves says so
 *   instead of showing an empty grid.
 * - Bottom: one button per category in a row that scrolls horizontally (so the row can hold any
 *   number of categories) next to the "Купить" button, which shows what the cart costs and stays
 *   disabled while the cart is empty or the player cannot afford it.
 *
 * @param state current game state: the balance to show, which category is selected, what is in the
 *   cart with what it costs, and which items the player already owns.
 * @param items the items of [GameUiState.selectedCategory], as the catalog registered them.
 * @param onSelectCategory called with the category whose button was pressed.
 * @param onPickVariant called with an item id and the id of the variant picked for it.
 * @param onIncrease called with the id of the item to put one more of into the cart.
 * @param onDecrease called with the id of the item to take one of out of the cart.
 * @param onBuy called when the player pays for the cart.
 * @param onClose called when the close button is pressed.
 * @param modifier modifier applied to the screen root.
 * @param categories categories the shop is split into; every [ItemCategory] by default, so a
 *   section is there even before its items are.
 */
@Composable
fun ShopScreen(
    state: GameUiState,
    items: List<Item>,
    onSelectCategory: (ItemCategory) -> Unit,
    onPickVariant: (String, String) -> Unit,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onBuy: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    categories: List<ItemCategory> = ItemCategory.entries
) {
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
                    columns = GridCells.Adaptive(minSize = ItemCellMinSize),
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
                onClick = onBuy,
                enabled = state.canBuyCart
            )
        }
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
 * Single shop item card: the sprite of the picked variant with an "add to goals" star toggle, the
 * item's name and price, a variant picker for items offered in several variants, and a purchase
 * control that depends on [mode].
 *
 * @param item the item to show.
 * @param pickedVariantId variant the item is shown and would be bought in.
 * @param quantity how many of this item are in the cart; shown by [ShopItemMode.COUNTER].
 * @param mode which purchase control to show; see [ShopItemMode].
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
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                Sprite(
                    assetPath = item.getSpritePath(pickedVariantId),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize()
                )
                SpriteButton(
                    assetPath = if (inGoals) Sprites.STAR_ON else Sprites.STAR_OFF,
                    contentDescription = if (inGoals) "Убрать из целей" else "Добавить в цели",
                    onClick = { inGoals = !inGoals },
                    size = StarButtonSize,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Sprite(
                    assetPath = Sprites.COIN,
                    contentDescription = null,
                    modifier = Modifier.size(PriceIconSize)
                )
                Text(
                    text = item.price.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.hasSeveralVariants) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.variantIds.forEach { variantId ->
                        SpriteButton(
                            assetPath = item.getSpritePath(variantId),
                            contentDescription = "Вариант «$variantId»",
                            onClick = { onPickVariant(variantId) },
                            size = VariantButtonSize,
                            selected = variantId == pickedVariantId
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
}

/** Items the previews go shopping with, standing in for what the catalog reads from the assets. */
private val PreviewItems = listOf(
    Item(
        id = "apple",
        title = "Яблоко",
        price = 15,
        category = ItemCategory.FOOD,
        variants = mapOf("red" to "items/apple/red", "green" to "items/apple/green")
    ),
    Item(
        id = "fish",
        title = "Рыбка",
        price = 25,
        category = ItemCategory.FOOD,
        variants = mapOf("default" to "items/fish/default")
    ),
    Item(
        id = "cake",
        title = "Пирожное",
        price = 40,
        category = ItemCategory.FOOD,
        variants = mapOf("default" to "items/cake/default")
    )
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
                    pickedVariants = mapOf("apple" to "green"),
                    cartPrice = 30
                ),
                items = PreviewItems,
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

/** Preview of [ShopScreen] in the dark theme, with an item the player already owns. */
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
                items = emptyList(),
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
