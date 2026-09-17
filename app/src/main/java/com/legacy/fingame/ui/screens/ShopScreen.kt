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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import com.legacy.fingame.ui.DemoContent
import com.legacy.fingame.ui.components.BalanceChip
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors

private val CloseButtonSize = 64.dp
private val CategoryButtonSize = 56.dp
private val CounterButtonSize = 48.dp
private val StarButtonSize = 40.dp
private val ItemCellMinSize = 170.dp

/** How a card exposes its purchase controls. Presentation-only, never stored in the ViewModel. */
enum class ShopItemMode { COUNTER, ADDABLE, PURCHASED }

/**
 * Shop screen. It receives plain ids and builds blocks out of them — no prices, names or
 * catalog models are known here; visible labels are the placeholders from the customer's mockup.
 */
@Composable
fun ShopScreen(
    state: GameUiState,
    onSelectCategory: (String) -> Unit,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    itemIds: List<String> = DemoContent.itemIds,
    categoryIds: List<String> = DemoContent.categoryIds
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BalanceChip()
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
            text = "Название категории",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = ItemCellMinSize),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(items = itemIds, key = { _, itemId -> itemId }) { index, itemId ->
                ShopItemCard(
                    itemId = itemId,
                    quantity = state.quantities[itemId] ?: 0,
                    mode = demoModeFor(index),
                    onIncrease = { onIncrease(itemId) },
                    onDecrease = { onDecrease(itemId) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Categories scroll horizontally so the "Купить" button always keeps its full width.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoryIds.forEach { categoryId ->
                    SpriteButton(
                        assetPath = Sprites.shopCategory(categoryId),
                        contentDescription = "Категория товаров",
                        onClick = { onSelectCategory(categoryId) },
                        size = CategoryButtonSize,
                        selected = categoryId == state.selectedCategoryId
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            PillButton(
                text = "Купить",
                // Purchase flow belongs to another team and will be wired in later.
                onClick = {}
            )
        }
    }
}

/** Demo-only layout rule: every fifth card looks purchased, every third offers add/remove. */
private fun demoModeFor(index: Int): ShopItemMode = when {
    (index + 1) % 5 == 0 -> ShopItemMode.PURCHASED
    (index + 1) % 3 == 0 -> ShopItemMode.ADDABLE
    else -> ShopItemMode.COUNTER
}

@Composable
private fun ShopItemCard(
    itemId: String,
    quantity: Int,
    mode: ShopItemMode,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Goal toggle is local UI state until the team defines where goals actually live.
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
                    assetPath = Sprites.shopItem(itemId),
                    contentDescription = "Изображение товара",
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
                text = "Название",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "стоимость",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

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

// ---------- Previews ----------

@Preview(name = "Shop — Light", showBackground = true)
@Composable
private fun ShopScreenLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(quantities = mapOf("item_01" to 2)),
                onSelectCategory = {},
                onIncrease = {},
                onDecrease = {},
                onClose = {}
            )
        }
    }
}

@Preview(name = "Shop — Dark", showBackground = true)
@Composable
private fun ShopScreenDarkPreview() {
    FinGameTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ShopScreen(
                state = GameUiState(quantities = mapOf("item_02" to 1)),
                onSelectCategory = {},
                onIncrease = {},
                onDecrease = {},
                onClose = {}
            )
        }
    }
}
