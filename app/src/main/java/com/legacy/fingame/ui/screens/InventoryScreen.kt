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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.legacy.fingame.game.items.InventoryEntry
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.ItemSelection
import com.legacy.fingame.game.items.ItemUse
import com.legacy.fingame.game.stats.StatKind
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.components.title
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors

private val CloseButtonSize = 64.dp
private val ItemCellMinSize = 140.dp
private val DialogSpriteSize = 120.dp

/**
 * Inventory screen: everything the player bought, in a grid, and a modal window with what can be
 * done with the item that was tapped.
 *
 * Layout:
 * - Top: the screen title and a close button.
 * - Below that: a scrollable grid of item cards ([InventoryCell]), one per owned item and variant,
 *   each showing how many of it there are and whether it is on the pet right now. An empty inventory
 *   says so instead of showing an empty grid.
 * - Over everything: [ItemActionDialog] for the tapped item, until the player closes it.
 *
 * The dialog offers exactly what the item allows (see [ItemUse]): food is eaten and is gone, a toy is
 * played with and stays, and clothes and decorations are put on and taken off. The window stays open
 * while the item is still there, so the player can feed the pet several apples in a row; it closes
 * itself once the last one is eaten, since there is nothing left to act on.
 *
 * @param entries the owned items to show, as [com.legacy.fingame.game.items.Inventory] built them.
 * @param onUseItem called with the item the pet should eat or play with.
 * @param onToggleWorn called with the item that should be put on or taken off.
 * @param onClose called when the close button is pressed.
 * @param modifier modifier applied to the screen root.
 */
@Composable
fun InventoryScreen(
    entries: List<InventoryEntry>,
    onUseItem: (ItemSelection) -> Unit,
    onToggleWorn: (ItemSelection) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var picked by remember { mutableStateOf<ItemSelection?>(null) }
    // The item is looked up again on every change of the inventory, so the window always shows what
    // the player actually has left; an item that ran out is simply not found any more.
    val pickedEntry = entries.find { it.selection == picked }

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
            Text(
                text = "Инвентарь",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            SpriteButton(
                assetPath = Sprites.CLOSE,
                contentDescription = "Закрыть инвентарь",
                onClick = onClose,
                size = CloseButtonSize
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (entries.isEmpty()) {
                Text(
                    text = "Инвентарь пуст — купите что-нибудь в магазине",
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
                    items(
                        items = entries,
                        key = { entry -> "${entry.item.id}:${entry.variantId}" }
                    ) { entry ->
                        InventoryCell(entry = entry, onClick = { picked = entry.selection })
                    }
                }
            }
        }
    }

    if (pickedEntry != null) {
        ItemActionDialog(
            entry = pickedEntry,
            onUse = { onUseItem(pickedEntry.selection) },
            onToggleWorn = { onToggleWorn(pickedEntry.selection) },
            onDismiss = { picked = null }
        )
    }
}

/**
 * Single inventory cell: the sprite of the owned variant, how many of it there are, its name and a
 * note about it being on the pet.
 *
 * @param entry the owned item this cell stands for.
 * @param onClick called when the cell is tapped, which is what opens [ItemActionDialog].
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
        border = BorderStroke(
            width = if (entry.worn) 2.dp else 1.dp,
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
                    assetPath = entry.spritePath,
                    contentDescription = entry.item.title,
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
                text = entry.item.title,
                style = MaterialTheme.typography.bodyLarge,
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
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Text(
            text = "×$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * Modal window with what can be done with one inventory item: its sprite and name, what it does to
 * the pet's stats, the action the item allows, and a way out.
 *
 * @param entry the item the window was opened for.
 * @param onUse called when the pet should eat the item or play with it; only offered for items that
 *   are used that way (see [ItemUse.CONSUMED] and [ItemUse.REUSABLE]).
 * @param onToggleWorn called when the item should be put on or taken off; only offered for items the
 *   pet can wear.
 * @param onDismiss called when the window should be closed, by the button or by tapping outside it.
 */
@Composable
private fun ItemActionDialog(
    entry: InventoryEntry,
    onUse: () -> Unit,
    onToggleWorn: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GameColors.cardStroke),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Sprite(
                    assetPath = entry.spritePath,
                    contentDescription = entry.item.title,
                    modifier = Modifier.size(DialogSpriteSize)
                )

                Text(
                    text = entry.item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

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

                if (entry.item.isWearable) {
                    PillButton(
                        text = if (entry.worn) "Убрать" else "Надеть",
                        onClick = onToggleWorn
                    )
                } else {
                    PillButton(text = entry.item.useActionTitle(), onClick = onUse)
                }

                PillButton(text = "Закрыть", onClick = onDismiss)
            }
        }
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

/**
 * One of an item's effects, as a chip saying which bar it moves and by how much.
 *
 * @param stat the stat the effect is on.
 * @param value how much the effect adds to it; a negative value is shown with its minus sign.
 * @param modifier modifier applied to the chip surface.
 */
@Composable
private fun EffectChip(
    stat: StatKind,
    value: Int,
    modifier: Modifier = Modifier
) {
    val sign = if (value > 0) "+" else ""
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Text(
            text = "${stat.title()} $sign$value",
            style = MaterialTheme.typography.labelMedium,
            color = if (value < 0) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 1
        )
    }
}

/** Inventory the previews show, standing in for what the player's state holds. */
private val PreviewEntries = listOf(
    InventoryEntry(
        item = Item(
            id = "apple",
            title = "Яблоко",
            price = 15,
            category = ItemCategory.FOOD,
            variants = mapOf("red" to "items/apple/red"),
            effects = mapOf(StatKind.HUNGER to 20, StatKind.HEALTH to 5)
        ),
        variantId = "red",
        count = 3,
        worn = false
    ),
    InventoryEntry(
        item = Item(
            id = "ball",
            title = "Мячик",
            price = 60,
            category = ItemCategory.TOYS,
            variants = mapOf("red" to "items/ball/red"),
            effects = mapOf(StatKind.PLEASURE to 20, StatKind.HUNGER to -5)
        ),
        variantId = "red",
        count = 1,
        worn = false
    ),
    InventoryEntry(
        item = Item(
            id = "hat",
            title = "Шляпа",
            price = 100,
            category = ItemCategory.CLOTHES,
            variants = mapOf("black" to "items/hat/black"),
            effects = mapOf(StatKind.PLEASURE to 10)
        ),
        variantId = "black",
        count = 1,
        worn = true
    )
)

/** Preview of [InventoryScreen] in the light theme. */
@Preview(name = "Inventory — Light", showBackground = true)
@Composable
private fun InventoryScreenLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            InventoryScreen(
                entries = PreviewEntries,
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
                onUseItem = {},
                onToggleWorn = {},
                onClose = {}
            )
        }
    }
}

/** Preview of the modal window that opens on an item the pet can wear. */
@Preview(name = "Inventory — Item actions", showBackground = true)
@Composable
private fun ItemActionDialogPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ItemActionDialog(
                entry = PreviewEntries.last(),
                onUse = {},
                onToggleWorn = {},
                onDismiss = {}
            )
        }
    }
}
