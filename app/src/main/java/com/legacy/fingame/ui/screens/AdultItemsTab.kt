package com.legacy.fingame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.items.CustomItemDraft
import com.legacy.fingame.game.items.CustomItemIcons
import com.legacy.fingame.game.items.CustomItems
import com.legacy.fingame.game.items.Item
import com.legacy.fingame.game.items.ItemCategory
import com.legacy.fingame.game.items.customItemOf
import com.legacy.fingame.game.stats.StatKind
import com.legacy.fingame.ui.components.GameDialog
import com.legacy.fingame.ui.components.GameDialogBlock
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.PillStyle
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens

/** Иконка своего предмета в списке. */
private val ListIconSize = 40.dp

/** Кнопка иконки в сетке формы. */
private val IconChoiceSize = 52.dp

/** Иконка стата в строке эффекта. */
private val EffectIconSize = 24.dp

/** Поле, в котором видна набранная цена. */
private val PriceBoxHeight = 52.dp

/** Как в форме называются статы: так взрослому понятнее, что прибавляет «+». */
private fun StatKind.formTitle(): String = when (this) {
    StatKind.HEALTH -> "Здоровье"
    StatKind.HUNGER -> "Сытость"
    StatKind.PLEASURE -> "Настроение"
}

/** Со знаком: «+10», «−5», «0». */
private fun signed(value: Int): String = when {
    value > 0 -> "+$value"
    value < 0 -> "−${-value}"
    else -> "0"
}

/**
 * Вкладка «Предметы» хаба взрослого: свои предметы магазина списком и кнопка «Добавить», которая
 * открывает форму прямо здесь, вместо списка.
 *
 * @param items свои предметы.
 * @param onAdd сохранить предмет из формы; false — не сохранён.
 * @param onRemove убрать предмет из магазина.
 */
@Composable
internal fun CustomItemsTab(
    items: List<Item>,
    onAdd: (CustomItemDraft) -> Boolean,
    onRemove: (String) -> Boolean,
    initialFormOpen: Boolean = false
) {
    var formOpen by rememberSaveable { mutableStateOf(initialFormOpen) }
    var removingId by rememberSaveable { mutableStateOf<String?>(null) }

    if (formOpen) {
        CustomItemForm(
            onSave = { draft -> onAdd(draft).also { saved -> if (saved) formOpen = false } },
            onCancel = { formOpen = false },
            existingCount = items.size
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (items.isEmpty()) {
                EmptyText("Своих предметов пока нет.\nДобавь — и они появятся в магазине.")
            } else {
                CardGrid(items = items, key = { it.id }) { item ->
                    CustomItemCard(item = item, onRemove = { removingId = item.id })
                }
            }
        }
        PillButton(
            text = "Добавить",
            onClick = { formOpen = true },
            style = PillStyle.Primary,
            enabled = items.size < CustomItems.MAX,
            modifier = Modifier.fillMaxWidth()
        )
    }

    val removing = items.find { it.id == removingId }
    if (removing != null) {
        GameDialog(onDismiss = { removingId = null }) {
            GameDialogBlock(
                title = "Убрать «${removing.name}»?",
                closeDescription = "Не убирать",
                onDismiss = { removingId = null },
                actions = {
                    PillButton(
                        text = "Убрать",
                        onClick = {
                            onRemove(removing.id)
                            removingId = null
                        },
                        style = PillStyle.Primary,
                        compact = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PillButton(
                        text = "Отмена",
                        onClick = { removingId = null },
                        style = PillStyle.Text,
                        compact = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            ) {
                Text(
                    text = "Предмет пропадёт из магазина и из инвентаря ребёнка.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Свой предмет в списке: иконка, название, цена с разделом и кнопка «Убрать». */
@Composable
private fun CustomItemCard(item: Item, onRemove: () -> Unit) {
    AdultCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Sprite(
                assetPath = item.getIconPath(item.defaultVariantId),
                contentDescription = null,
                modifier = Modifier.size(ListIconSize)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.category.title(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Sprite(
                        assetPath = Sprites.COIN,
                        contentDescription = "Цена",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = item.price.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                if (item.effects.isNotEmpty()) {
                    Text(
                        text = item.effects.entries.joinToString(", ") { (stat, value) ->
                            "${stat.formTitle()} ${signed(value)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            PillButton(
                text = "Убрать",
                onClick = onRemove,
                style = PillStyle.Outlined,
                compact = true
            )
        }
    }
}

/**
 * Форма «Новый предмет»: название, цена с цифровой клавиатуры, раздел, эффекты (у еды и игрушек),
 * иконка из набора игры. «Сохранить» доступна, когда всё верно; что не так — написано над ней.
 * Всё прокручивается, и клавиатура телефона не закрывает поля. Лёжа — две колонки.
 *
 * @param onSave сохранить; true — сохранено, форма закрывается.
 * @param onCancel закрыть без сохранения.
 * @param existingCount сколько своих предметов уже есть — для предела.
 */
@Composable
internal fun CustomItemForm(
    onSave: (CustomItemDraft) -> Boolean,
    onCancel: () -> Unit,
    existingCount: Int,
    initialDraft: CustomItemDraft = CustomItemDraft()
) {
    var name by rememberSaveable { mutableStateOf(initialDraft.name) }
    var priceText by rememberSaveable {
        mutableStateOf(if (initialDraft.price > 0) initialDraft.price.toString() else "")
    }
    var category by rememberSaveable { mutableStateOf(initialDraft.category) }
    var health by rememberSaveable { mutableIntStateOf(initialDraft.effects[StatKind.HEALTH] ?: 0) }
    var hunger by rememberSaveable { mutableIntStateOf(initialDraft.effects[StatKind.HUNGER] ?: 0) }
    var pleasure by rememberSaveable { mutableIntStateOf(initialDraft.effects[StatKind.PLEASURE] ?: 0) }
    var iconPath by rememberSaveable { mutableStateOf(initialDraft.iconPath) }

    val draft = CustomItemDraft(
        name = name,
        price = priceText.toIntOrNull() ?: 0,
        category = category,
        effects = mapOf(
            StatKind.HEALTH to health,
            StatKind.HUNGER to hunger,
            StatKind.PLEASURE to pleasure
        ),
        iconPath = iconPath
    )
    val errors = draft.validate(existingCount)
    // Пока взрослый ничего не ввёл, ругаться не на что: ошибки видны, когда он начал заполнять.
    val touched = name.isNotEmpty() || priceText.isNotEmpty()
    val focusManager = LocalFocusManager.current
    val short = GameDimens.isShortScreen

    val nameBlock: @Composable ColumnScope.() -> Unit = {
        FormLabel("Название")
        OutlinedTextField(
            value = name,
            onValueChange = { typed ->
                name = typed.filterNot { it == '\n' || it.code < 0x20 }.take(CustomItems.NAME_MAX)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Например, Пазл") },
            supportingText = { Text("${name.length}/${CustomItems.NAME_MAX}") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
    val categoryBlock: @Composable ColumnScope.() -> Unit = {
        FormLabel("Раздел магазина")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomItems.CATEGORIES.forEach { entry ->
                PillButton(
                    text = entry.title(),
                    onClick = { category = entry },
                    selected = entry == category,
                    compact = true,
                    autoShrink = false
                )
            }
        }
    }
    val effectsBlock: @Composable ColumnScope.() -> Unit = {
        if (CustomItems.hasEffects(category)) {
            FormLabel("Что делает с питомцем")
            EffectRow(StatKind.HEALTH, health) { health = it }
            EffectRow(StatKind.HUNGER, hunger) { hunger = it }
            EffectRow(StatKind.PLEASURE, pleasure) { pleasure = it }
        }
    }
    val iconBlock: @Composable ColumnScope.() -> Unit = {
        FormLabel("Иконка")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomItemIcons.ALL.forEachIndexed { index, path ->
                SpriteButton(
                    assetPath = path,
                    contentDescription = "Иконка ${index + 1}",
                    onClick = { iconPath = path },
                    size = IconChoiceSize,
                    selected = path == iconPath
                )
            }
        }
    }
    val priceBlock: @Composable ColumnScope.() -> Unit = {
        FormLabel("Цена, монет (${CustomItems.PRICE_MIN}–${CustomItems.PRICE_MAX})")
        PriceBox(priceText)
        Keypad(
            onDigit = { digit ->
                if (priceText.length < CustomItems.PRICE_MAX.toString().length) {
                    priceText = (priceText + digit).trimStart('0')
                }
            },
            onErase = { priceText = priceText.dropLast(1) }
        )
    }
    val saveBlock: @Composable ColumnScope.() -> Unit = {
        if (touched && errors.isNotEmpty()) {
            Text(
                text = errors.joinToString("\n"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        PillButton(
            text = "Сохранить",
            onClick = {
                focusManager.clearFocus()
                onSave(draft)
            },
            style = PillStyle.Primary,
            enabled = errors.isEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Новый предмет",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            PillButton(text = "Отмена", onClick = onCancel, style = PillStyle.Text, compact = true)
        }
        if (short) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    nameBlock()
                    categoryBlock()
                    effectsBlock()
                    iconBlock()
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    priceBlock()
                    saveBlock()
                }
            }
        } else {
            nameBlock()
            categoryBlock()
            priceBlock()
            effectsBlock()
            iconBlock()
            saveBlock()
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Набранная цена с монетой; пока ничего не набрано — «?». */
@Composable
private fun PriceBox(priceText: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(PriceBoxHeight),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = priceText.ifEmpty { "?" },
                style = MaterialTheme.typography.headlineSmall,
                color = if (priceText.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                softWrap = false
            )
            Sprite(assetPath = Sprites.COIN, contentDescription = null, modifier = Modifier.size(28.dp))
        }
    }
}

/** Строка эффекта: иконка и имя стата, «−», значение со знаком, «+». Шаг 5, от −50 до +50. */
@Composable
private fun EffectRow(stat: StatKind, value: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Sprite(
            assetPath = Sprites.stat(stat.xmlName),
            contentDescription = null,
            modifier = Modifier.size(EffectIconSize)
        )
        // На узком экране с крупным шрифтом «Настроение» ужимается целиком, а не режется.
        ShrinkText(
            text = stat.formTitle(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            minSize = 10.dp
        )
        SpriteButton(
            assetPath = Sprites.MINUS,
            contentDescription = "${stat.formTitle()} меньше",
            onClick = { onChange((value - CustomItems.EFFECT_STEP).coerceAtLeast(CustomItems.EFFECT_MIN)) },
            size = 40.dp,
            enabled = value > CustomItems.EFFECT_MIN,
            showIndicator = false
        )
        Text(
            text = signed(value),
            modifier = Modifier.widthIn(min = 60.dp),
            style = MaterialTheme.typography.titleMedium,
            color = when {
                value > 0 -> GameColors.success
                value < 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            softWrap = false,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        SpriteButton(
            assetPath = Sprites.PLUS,
            contentDescription = "${stat.formTitle()} больше",
            onClick = { onChange((value + CustomItems.EFFECT_STEP).coerceAtMost(CustomItems.EFFECT_MAX)) },
            size = 40.dp,
            enabled = value < CustomItems.EFFECT_MAX,
            showIndicator = false
        )
    }
}

// --- Превью ---

private val PreviewCustomItems = listOfNotNull(
    customItemOf(
        CustomItemDraft("Пазл", 40, ItemCategory.OTHER, iconPath = CustomItemIcons.ALL.first()),
        "custom-1"
    ),
    customItemOf(
        CustomItemDraft(
            "Печенье",
            12,
            ItemCategory.FOOD,
            mapOf(StatKind.HUNGER to 15, StatKind.PLEASURE to 5),
            "shop/categories/food.webp"
        ),
        "custom-2"
    )
)

private val PreviewDraft = CustomItemDraft(
    name = "Печенье",
    price = 12,
    category = ItemCategory.FOOD,
    effects = mapOf(StatKind.HUNGER to 15),
    iconPath = "shop/categories/food.webp"
)

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Preview(name = "Items — Light", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun CustomItemsLightPreview() {
    FinGameTheme(darkTheme = false) {
        PreviewSurface { CustomItemsTab(PreviewCustomItems, { true }, { true }) }
    }
}

@Preview(name = "Items — Dark", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun CustomItemsDarkPreview() {
    FinGameTheme(darkTheme = true) {
        PreviewSurface { CustomItemsTab(PreviewCustomItems, { true }, { true }) }
    }
}

@Preview(name = "Item form — Light", showBackground = true, widthDp = 411, heightDp = 1400)
@Composable
private fun CustomItemFormPreview() {
    FinGameTheme(darkTheme = false) {
        PreviewSurface { CustomItemForm({ true }, {}, 0, PreviewDraft) }
    }
}

@Preview(
    name = "Item form — 360dp, font 1.3",
    showBackground = true,
    widthDp = 360,
    heightDp = 1600,
    fontScale = 1.3f
)
@Composable
private fun CustomItemFormNarrowPreview() {
    FinGameTheme(darkTheme = false) {
        PreviewSurface { CustomItemForm({ true }, {}, 0, PreviewDraft) }
    }
}

@Preview(name = "Item form — Landscape", showBackground = true, widthDp = 891, heightDp = 411)
@Composable
private fun CustomItemFormLandscapePreview() {
    FinGameTheme(darkTheme = true) {
        PreviewSurface { CustomItemForm({ true }, {}, 0, PreviewDraft) }
    }
}
