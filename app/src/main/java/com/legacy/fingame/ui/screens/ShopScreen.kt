package com.legacy.fingame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legacy.fingame.domain.ItemCategory
import com.legacy.fingame.domain.PurchaseKind
import com.legacy.fingame.domain.ShopCatalog
import com.legacy.fingame.game.GameUiState
import com.legacy.fingame.game.ShopItemUi
import com.legacy.fingame.ui.components.BalanceChip
import com.legacy.fingame.ui.components.GameIcon
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.RoundIconButton
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val priceFormat = DecimalFormat(
    "#,###",
    DecimalFormatSymbols(Locale.forLanguageTag("ru")).apply { groupingSeparator = ' ' }
)

private fun formatPrice(value: Int): String = "${priceFormat.format(value)} ₽"

/**
 * Экран магазина.
 *
 * Раскладка по эскизу заказчика:
 * - верх: баланс слева, крестик закрытия справа, под ними — название текущей категории;
 * - центр: адаптивная сетка товаров с вертикальной прокруткой;
 * - низ (закреплён): слева кнопки категорий, справа сумма корзины и кнопка «Купить».
 *
 * Toast ([GameUiState.toast]) здесь НЕ обрабатывается — его показывает вызывающий экран,
 * который также владеет `consumeToast()`.
 */
@Composable
fun ShopScreen(
    state: GameUiState,
    onSelectCategory: (ItemCategory) -> Unit,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onToggleUnique: (String) -> Unit,
    onToggleGoal: (String) -> Unit,
    onCheckout: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        // ---------- Шапка ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BalanceChip(balance = state.balance)
            Spacer(modifier = Modifier.weight(1f))
            CloseButton(onClick = onClose)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = state.selectedCategory.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // ---------- Сетка товаров ----------
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = state.shopItems, key = { it.item.id }) { ui ->
                ShopItemCard(
                    ui = ui,
                    onIncrease = onIncrease,
                    onDecrease = onDecrease,
                    onToggleUnique = onToggleUnique,
                    onToggleGoal = onToggleGoal
                )
            }
        }

        // ---------- Нижняя закреплённая панель ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemCategory.entries.forEach { category ->
                    val selected = category == state.selectedCategory
                    RoundIconButton(
                        emoji = category.emoji,
                        contentDescription = if (selected) {
                            "Категория ${category.title}, выбрана"
                        } else {
                            "Категория ${category.title}"
                        },
                        onClick = { onSelectCategory(category) },
                        size = 48.dp,
                        modifier = Modifier.border(
                            width = if (selected) 3.dp else 0.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End) {
                if (state.cartTotal > 0) {
                    Text(
                        text = formatPrice(state.cartTotal),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .semantics { contentDescription = "Сумма корзины ${formatPrice(state.cartTotal)}" }
                    )
                }
                PillButton(
                    text = "Купить",
                    onClick = onCheckout,
                    enabled = state.canCheckout,
                    modifier = Modifier.semantics { contentDescription = "Купить товары из корзины" }
                )
            }
        }
    }
}

/** Круглая кнопка закрытия магазина (крестик). */
@Composable
private fun CloseButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 22.dp),
                onClick = onClick
            )
            .semantics { contentDescription = "Закрыть магазин" },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "✕",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Карточка одного товара в сетке. */
@Composable
private fun ShopItemCard(
    ui: ShopItemUi,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onToggleUnique: (String) -> Unit,
    onToggleGoal: (String) -> Unit
) {
    val item = ui.item
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, GameColors.cardStroke),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Квадратная область с иконкой + звёздочка «цель» в правом верхнем углу.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                GameIcon(
                    emoji = item.emoji,
                    spritePath = item.spritePath,
                    size = 72.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
                GoalStarButton(
                    isGoal = ui.isGoal,
                    onClick = { onToggleGoal(item.id) },
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatPrice(item.price),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            when {
                item.kind == PurchaseKind.CONSUMABLE -> QuantityRow(
                    title = item.title,
                    quantity = ui.quantity,
                    onIncrease = { onIncrease(item.id) },
                    onDecrease = { onDecrease(item.id) }
                )

                ui.owned -> OwnedBadge()

                else -> PillButton(
                    text = if (ui.quantity > 0) "Убрать" else "Добавить",
                    onClick = { onToggleUnique(item.id) },
                    modifier = Modifier.semantics {
                        contentDescription = if (ui.quantity > 0) {
                            "Убрать ${item.title} из корзины"
                        } else {
                            "Добавить ${item.title} в корзину"
                        }
                    }
                )
            }
        }
    }
}

/**
 * Маленькая кнопка-звёздочка «сделать целью».
 * Иконки StarBorder нет в базовом material-icons-core, поэтому рисуем глифы ★/☆.
 */
@Composable
private fun GoalStarButton(
    isGoal: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 14.dp),
                onClick = onClick
            )
            .semantics {
                contentDescription = if (isGoal) "Убрать из целей" else "Сделать целью"
            },
        shape = CircleShape,
        color = if (isGoal) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (isGoal) "★" else "☆",
                fontSize = 15.sp,
                color = if (isGoal) GameColors.coin else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Строка «− количество +» для расходуемых товаров. */
@Composable
private fun QuantityRow(
    title: String,
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        StepperButton(
            glyph = "−",
            contentDescription = "Убрать одну единицу: $title",
            enabled = quantity > 0,
            onClick = onDecrease
        )
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(36.dp)
                .semantics { contentDescription = "Количество: $quantity" }
        )
        StepperButton(
            glyph = "+",
            contentDescription = "Добавить одну единицу: $title",
            enabled = true,
            onClick = onIncrease
        )
    }
}

/** Квадратная кнопка шага количества. */
@Composable
private fun StepperButton(
    glyph: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val container = if (enabled) MaterialTheme.colorScheme.primaryContainer else GameColors.disabledContainer
    val content = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else GameColors.disabledContent
    val description = contentDescription
    Surface(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 16.dp),
                enabled = enabled,
                onClick = onClick
            )
            .semantics { this.contentDescription = description },
        shape = CircleShape,
        color = container,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = glyph,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = content
            )
        }
    }
}

/** Неактивная плашка «Куплено» для уже приобретённых уникальных товаров. */
@Composable
private fun OwnedBadge() {
    Surface(
        shape = RoundedCornerShape(50),
        color = GameColors.disabledContainer,
        modifier = Modifier.semantics { contentDescription = "Товар уже куплен" }
    ) {
        Text(
            text = "Куплено",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = GameColors.disabledContent,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

// ---------- Previews ----------

private fun previewState(category: ItemCategory = ItemCategory.FOOD): GameUiState {
    val items = ShopCatalog.items.filter { it.category == category }
    return GameUiState(
        balance = 1200,
        selectedCategory = category,
        shopItems = items.mapIndexed { index, item ->
            ShopItemUi(
                item = item,
                quantity = if (index == 0) 2 else 0,
                owned = index == 1 && item.kind == PurchaseKind.UNIQUE,
                isGoal = index == 2
            )
        },
        cartTotal = 1240,
        canCheckout = false
    )
}

@Preview(name = "Магазин — светлая", showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun ShopScreenLightPreview() {
    FinGameTheme(darkTheme = false) {
        ShopScreen(
            state = previewState(),
            onSelectCategory = {},
            onIncrease = {},
            onDecrease = {},
            onToggleUnique = {},
            onToggleGoal = {},
            onCheckout = {},
            onClose = {}
        )
    }
}

@Preview(name = "Магазин — тёмная", showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun ShopScreenDarkPreview() {
    FinGameTheme(darkTheme = true) {
        ShopScreen(
            state = previewState(ItemCategory.CLOTHES).copy(canCheckout = true),
            onSelectCategory = {},
            onIncrease = {},
            onDecrease = {},
            onToggleUnique = {},
            onToggleGoal = {},
            onCheckout = {},
            onClose = {}
        )
    }
}
