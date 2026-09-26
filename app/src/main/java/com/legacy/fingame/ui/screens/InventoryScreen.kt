package com.legacy.fingame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.legacy.fingame.game.items.InventoryEntry
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.items.ItemUse
import com.legacy.fingame.game.stats.PetStats
import com.legacy.fingame.game.stats.StatKind
import com.legacy.fingame.ui.components.EffectChip
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.components.StatChip
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens

private val CloseButtonSize = 40.dp
private val InventoryCellMinSize = 140.dp
private val PopupMaxWidth = 260.dp
private val PopupCloseButtonSize = 44.dp

/** Gap between the tapped cell and the item window that pops up next to it. */
private val PopupGap = 8.dp

/** How close to the edge of the screen the item window is allowed to come. */
private val PopupScreenMargin = 12.dp

/**
 * Inventory screen: everything the player bought, in a grid, and a window with what can be done with
 * the item that was tapped.
 *
 * Layout:
 * - Top: the screen title and a close button.
 * - Below that, pinned above the grid so scrolling it never carries the row away: [InventoryStatsRow],
 *   the same per-stat chips the main screen shows next to the pet, in one line that never wraps or
 *   scrolls off a normal phone width and only turns scrollable itself if it ever has to.
 * - Below that: a scrollable grid of item cards ([InventoryCell]), one per owned item and variant,
 *   each showing how many of it there are and whether it is on the pet right now. An empty inventory
 *   says so instead of showing an empty grid.
 * - Over the grid: [ItemActionPopup] for the tapped item, right next to the cell it was opened from,
 *   until the player closes it.
 *
 * The window offers exactly what the item allows (see [ItemUse]): food is eaten and is gone, a toy is
 * played with and stays, and clothes and decorations are put on and taken off. The window stays open
 * while the item is still there, so the player can feed the pet several apples in a row; it closes
 * itself once the last one is eaten, since there is nothing left to act on.
 *
 * @param entries the owned items to show, as [com.legacy.fingame.game.items.Inventory] built them.
 * @param stats the pet's current stats, read into [InventoryStatsRow]; the same
 *   [com.legacy.fingame.game.GameUiState.stats] the main screen reads its own stat chips from, so
 *   eating an item here moves both in step.
 * @param onUseItem called with the item the pet should eat or play with.
 * @param onToggleWorn called with the item that should be put on or taken off.
 * @param onClose called when the close button is pressed.
 * @param modifier modifier applied to the screen root.
 * @param readOnly инвентарь глазами взрослого: заголовок «Инвентарь ребёнка», окно предмета
 *   показывает только имя и эффекты, без кнопок. Такой инвентарь живёт внутри хаба взрослого, у
 *   которого свой крестик и свои поля, поэтому своего крестика и полей экрана у него нет.
 */
@Composable
fun InventoryScreen(
    entries: List<InventoryEntry>,
    stats: PetStats,
    onUseItem: (ItemSelection) -> Unit,
    onToggleWorn: (ItemSelection) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    var picked by remember { mutableStateOf<ItemSelection?>(null) }
    // The item is looked up again on every change of the inventory, so the window always shows what
    // the player actually has left; an item that ran out is simply not found any more.
    val pickedEntry = entries.find { it.selection == picked }

    Column(
        modifier = if (readOnly) {
            modifier.fillMaxSize()
        } else {
            modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (readOnly) "Инвентарь ребёнка" else "Инвентарь",
                // Внутри хаба взрослого заголовок экрана уже есть — этот тише, как у журнала там же.
                style = if (readOnly) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.headlineSmall
                },
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!readOnly) {
                SpriteButton(
                    assetPath = Sprites.CLOSE,
                    contentDescription = "Закрыть инвентарь",
                    onClick = onClose,
                    size = CloseButtonSize,
                    showIndicator = false
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        InventoryStatsRow(
            stats = stats,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (entries.isEmpty()) {
                Text(
                    text = "Инвентарь пуст${DashSeparator}купите что-нибудь в магазине",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = InventoryCellMinSize),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = entries,
                        key = { entry -> "${entry.item.id}:${entry.variantId}" }
                    ) { entry ->
                        // The window is opened from inside the cell so that it knows where that cell
                        // is and can pop up right next to it.
                        Box {
                            InventoryCell(entry = entry, onClick = { picked = entry.selection })
                            if (pickedEntry != null && pickedEntry.selection == entry.selection) {
                                ItemActionPopup(
                                    entry = pickedEntry,
                                    onUse = { onUseItem(pickedEntry.selection) },
                                    onToggleWorn = { onToggleWorn(pickedEntry.selection) },
                                    onDismiss = { picked = null },
                                    readOnly = readOnly
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The pet's stats in one row at the top of the inventory: the same [StatChip] the main screen shows
 * next to the pet, one per [StatKind], reading [stats] instead of a bar so a stat added to that enum
 * shows up here without a change to this screen.
 *
 * Never wraps to a second line and never crops a chip: as long as every chip fits in the width it is
 * given the row simply lays them out, same as the main screen does; on a screen too narrow for that —
 * narrower than this game promises a phone gets — the row scrolls sideways instead, the same way the
 * shop's category row and an item's variant picker fall back to scrolling.
 *
 * @param stats the pet's current stats.
 * @param modifier modifier applied to the row.
 */
@Composable
private fun InventoryStatsRow(
    stats: PetStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatKind.entries.forEach { stat -> StatChip(stat = stat, stats = stats) }
    }
}

/**
 * Single inventory cell: the sprite of the owned variant, how many of it there are, its name and a
 * note about it being on the pet.
 *
 * @param entry the owned item this cell stands for.
 * @param onClick called when the cell is tapped, which is what opens [ItemActionPopup].
 * @param modifier modifier applied to the card surface.
 */
@Composable
private fun InventoryCell(
    entry: InventoryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        // Надетое отличается цветом рамки, а не её толщиной: разницу между 1 и 2 dp на глаз не
        // видно, а карточки от неё стоят на волосок по-разному.
        border = BorderStroke(
            width = 1.dp,
            color = if (entry.worn) MaterialTheme.colorScheme.primary else GameColors.cardStroke
        ),
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
                    assetPath = entry.iconPath,
                    contentDescription = entry.item.name,
                    modifier = Modifier.fillMaxSize()
                )
                if (entry.count > 1) {
                    CountBadge(
                        count = entry.count,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = entry.item.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (entry.worn) {
                Text(
                    text = "Надето",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Badge with how many of an item the player has, shown over its sprite.
 *
 * @param count number of items owned.
 * @param modifier modifier applied to the badge surface.
 */
@Composable
private fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        // Без рамки: бордюр внутри бордюра карточки читается как лишняя линия, а заливки
        // secondaryContainer поверх спрайта и так хватает, чтобы число отделилось от картинки.
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = "×$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Window with what can be done with one inventory item, floating next to the cell it was opened
 * from — see [ItemActionBlock] for what it holds and [CellAnchoredPositionProvider] for where it
 * ends up.
 *
 * It takes the taps of the screen while it is open, so a tap anywhere outside it — as well as the
 * system back gesture — closes it and nothing else happens.
 *
 * @param entry the item the window was opened for.
 * @param onUse called when the pet should eat the item or play with it; only offered for items that
 *   are used that way (see [ItemUse.CONSUMED] and [ItemUse.REUSABLE]).
 * @param onToggleWorn called when the item should be put on or taken off; only offered for items the
 *   pet can wear.
 * @param onDismiss called when the window should be closed, by the cross or by tapping outside it.
 */
@Composable
private fun ItemActionPopup(
    entry: InventoryEntry,
    onUse: () -> Unit,
    onToggleWorn: () -> Unit,
    onDismiss: () -> Unit,
    readOnly: Boolean = false
) {
    val density = LocalDensity.current
    val gapPx = with(density) { PopupGap.roundToPx() }
    val marginPx = with(density) { PopupScreenMargin.roundToPx() }
    val positionProvider = remember(gapPx, marginPx) {
        CellAnchoredPositionProvider(gapPx = gapPx, marginPx = marginPx)
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        ItemActionBlock(
            entry = entry,
            onUse = onUse,
            onToggleWorn = onToggleWorn,
            onDismiss = onDismiss,
            readOnly = readOnly
        )
    }
}

/**
 * The block the item window is made of: what the item does to the pet's stats, the action the item
 * allows, and the cross that closes it.
 *
 * The item is neither pictured nor named here — the player has just tapped its cell and knows what
 * it is, so the window only says what will happen and lets it happen.
 *
 * An item the pet wears does nothing to its stats at all (see [Item.effects]), so its window holds
 * the action alone: the row of effects is not drawn rather than drawn empty, and no gap is left
 * where it would have been.
 *
 * The cross sits on the top-right corner and hangs half-way over the edge of the card, so it reads
 * as a way out of the block rather than as one more action inside it.
 *
 * @param entry the item the window was opened for.
 * @param onUse called when the pet should eat the item or play with it.
 * @param onToggleWorn called when the item should be put on or taken off.
 * @param onDismiss called when the cross is pressed.
 * @param modifier modifier applied to the block root.
 * @param readOnly окно для взрослого: вместо действия — имя предмета и надет ли он.
 */
@Composable
private fun ItemActionBlock(
    entry: InventoryEntry,
    onUse: () -> Unit,
    onToggleWorn: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    // Half of the cross hangs outside the card, so the card keeps that much room around itself.
    val overhang = GameDimens.buttonSize(PopupCloseButtonSize) / 2

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .padding(top = overhang, end = overhang)
                .widthIn(max = PopupMaxWidth),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            // Окно всплывает над затемнением и отделено тенью; рамка поверх этого была бы третьим
            // обведённым скруглением подряд — карточка, окно и кнопка в нём.
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (readOnly) {
                    Text(
                        text = entry.item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
                if (entry.item.effects.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        entry.item.effects.forEach { (stat, value) ->
                            EffectChip(stat = stat, value = value)
                        }
                    }
                }

                if (readOnly) {
                    if (entry.item.isWearable) {
                        Text(
                            text = if (entry.worn) "Сейчас надето" else "Сейчас не надето",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (entry.item.isWearable) {
                    PillButton(
                        text = if (entry.worn) "Убрать" else "Надеть",
                        onClick = onToggleWorn
                    )
                } else {
                    PillButton(text = entry.item.useActionTitle(), onClick = onUse)
                }
            }
        }

        SpriteButton(
            assetPath = Sprites.CLOSE,
            contentDescription = "Закрыть окно предмета",
            onClick = onDismiss,
            size = PopupCloseButtonSize,
            showIndicator = false,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

/**
 * Puts the item window next to the cell it was opened from: under the cell if there is room for it
 * there, above the cell otherwise, and horizontally centred on the cell.
 *
 * The window is always kept inside the screen with [marginPx] to spare, so a cell at the very edge
 * of the grid still gets a window the player can read and reach in full.
 *
 * @param gapPx distance between the cell and the window, in pixels.
 * @param marginPx how close to the edge of the screen the window may come, in pixels.
 */
private class CellAnchoredPositionProvider(
    private val gapPx: Int,
    private val marginPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(0)
        val x = (anchorBounds.center.x - popupContentSize.width / 2)
            .coerceIn(minOf(marginPx, maxX), maxX)

        val below = anchorBounds.bottom + gapPx
        val above = anchorBounds.top - gapPx - popupContentSize.height
        val maxY = (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(0)
        val y = when {
            below <= maxY -> below
            above >= marginPx -> above
            else -> maxY
        }

        return IntOffset(x, y)
    }
}

/**
 * What the button that uses an item says.
 *
 * @return The Russian label of the action: food is eaten, anything else that is used is played with.
 */
private fun Item.useActionTitle(): String = when (category.use) {
    ItemUse.CONSUMED -> "Съесть"
    else -> "Поиграть"
}

/** Inventory the previews show, standing in for what the player's state holds. */
private val PreviewEntries = listOf(
    InventoryEntry(
        item = Item(
            id = "apple",
            name = "Яблоко",
            price = 15,
            category = ItemCategory.FOOD,
            variantIds = listOf("default"),
            declaredEffects = mapOf(StatKind.HUNGER to 20, StatKind.HEALTH to 5)
        ),
        variantId = "default",
        count = 3,
        worn = false
    ),
    InventoryEntry(
        item = Item(
            id = "ball",
            name = "Мячик",
            price = 60,
            category = ItemCategory.TOYS,
            variantIds = listOf("red"),
            declaredEffects = mapOf(StatKind.PLEASURE to 20, StatKind.HUNGER to -5)
        ),
        variantId = "red",
        count = 1,
        worn = false
    ),
    // Clothes the player's own data may still declare an effect on: worn to no effect all the same,
    // which is what the window opened on it has to look right without.
    InventoryEntry(
        item = Item(
            id = "hat",
            name = "Шляпа",
            price = 100,
            category = ItemCategory.CLOTHES,
            variantIds = listOf("black"),
            declaredEffects = mapOf(StatKind.PLEASURE to 10)
        ),
        variantId = "black",
        count = 1,
        worn = true
    )
)

/** Stats the previews show in [InventoryStatsRow]: a pet a little hungry, otherwise doing fine. */
private val PreviewStats = PetStats(
    mapOf(
        StatKind.HEALTH to 90,
        StatKind.HUNGER to 35,
        StatKind.PLEASURE to 70
    )
)

/** Preview of [InventoryScreen] in the light theme, stats row included. */
@Preview(name = "Inventory — Light", showBackground = true)
@Composable
private fun InventoryScreenLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            InventoryScreen(
                entries = PreviewEntries,
                stats = PreviewStats,
                onUseItem = {},
                onToggleWorn = {},
                onClose = {}
            )
        }
    }
}

/** Preview of [InventoryScreen] with nothing bought yet. */
@Preview(name = "Inventory — Empty", showBackground = true)
@Composable
private fun InventoryScreenEmptyPreview() {
    FinGameTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            InventoryScreen(
                entries = emptyList(),
                stats = PreviewStats,
                onUseItem = {},
                onToggleWorn = {},
                onClose = {}
            )
        }
    }
}

/**
 * Preview of [InventoryScreen] at 360.dp: the stats row's promise that three chips fit one line
 * without scrolling on an ordinary phone width.
 */
@Preview(name = "Inventory — Narrow 360dp", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun InventoryScreenNarrowPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            InventoryScreen(
                entries = PreviewEntries,
                stats = PreviewStats,
                onUseItem = {},
                onToggleWorn = {},
                onClose = {}
            )
        }
    }
}

/**
 * Preview of the same narrow phone with the system text turned up as far as the game is laid out
 * for: the screen title, the stats row and the item names all have to stay on their own lines.
 */
@Preview(
    name = "Inventory — Narrow 360dp, large text",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    fontScale = 1.3f
)
@Composable
private fun InventoryScreenLargeTextPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            InventoryScreen(
                entries = PreviewEntries,
                stats = PreviewStats,
                onUseItem = {},
                onToggleWorn = {},
                onClose = {}
            )
        }
    }
}

/**
 * Preview of the window that opens on an item the pet can wear: the case with nothing to say about
 * the stats, which has to come out as the action alone rather than as an action with an empty row
 * over it.
 */
@Preview(name = "Inventory — Item actions, worn item", showBackground = true)
@Composable
private fun ItemActionBlockPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ItemActionBlock(
                entry = PreviewEntries.last(),
                onUse = {},
                onToggleWorn = {},
                onDismiss = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/** Preview of the window that opens on an item the pet is given, effects and all. */
@Preview(name = "Inventory — Item actions, food", showBackground = true)
@Composable
private fun ItemActionBlockWithEffectsPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ItemActionBlock(
                entry = PreviewEntries.first(),
                onUse = {},
                onToggleWorn = {},
                onDismiss = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
