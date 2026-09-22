package com.legacy.fingame.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.animals.AnimalSelection
import com.legacy.fingame.ui.components.PillButton
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors

private val SelectScreenPadding = 16.dp
private val AnimalCellMinSize = 150.dp
private val NameFieldMaxWidth = 360.dp

/** How long a pet's name may be, so it still fits into the badge above the pet on the main screen. */
private const val MaxPetNameLength = 20

/**
 * Stores the picked [AnimalSelection] across configuration changes and process death.
 *
 * [AnimalSelection] is a plain data class of the game layer, so it is not something a Bundle can
 * hold on its own; saving it as the pair of ids it consists of keeps the game layer free of any
 * knowledge about Compose or Android state saving.
 *
 * A selection that wasn't made yet is saved as an empty list, and restoring an empty list gives
 * back no selection at all.
 */
internal val AnimalSelectionSaver: Saver<AnimalSelection?, Any> = listSaver(
    save = { selection ->
        if (selection == null) emptyList() else listOf(selection.animalId, selection.variantId)
    },
    restore = { ids ->
        if (ids.size == 2) AnimalSelection(animalId = ids[0], variantId = ids[1]) else null
    }
)

/**
 * First-launch screen where the player takes in a pet, in two steps.
 *
 * The first step ([SpeciesStep]) is about *who* the pet is: one card per animal, shown at its first
 * age stage ([Animal.FIRST_AGE]) — that is how the pet starts out. Tapping a card opens the second
 * step ([VariantStep]), which is about *what it looks like* and *what it is called*: the variants of
 * that one animal and a field for its name. Splitting the choice this way keeps the first screen
 * down to a handful of animals instead of every variant of every one of them at once, and leaves
 * room for the name where it belongs — next to the pet it is being given to.
 *
 * Going back from the second step returns to the first one rather than leaving the game, so a
 * player who picked the wrong animal is never stuck with it.
 *
 * When [animals] holds no animal that can be played at all — the animal data is broken, empty, or
 * leaves an animal without a single variant — there is nothing to pick, so both steps are replaced
 * by a message saying exactly that instead of an empty screen with a button that can never be
 * pressed.
 *
 * @param animals animals the player can choose from, as read from the animal data.
 * @param onSelect called with the picked animal, its variant and the name the player gave it when
 *   the choice is confirmed; the caller is the one that persists it.
 * @param modifier modifier applied to the screen root.
 * @param previousPetMissingFromData whether the pet the player picked earlier is no longer described
 *   by the animal data, so its sprites cannot be drawn and the saved choice cannot be played on.
 *   Nothing the player did leads here — a pet is never lost by playing badly; this only happens when
 *   the animal or the variant is dropped from the game's own data between versions. The screen then
 *   says so instead of greeting the player as a newcomer.
 */
@Composable
fun AnimalSelectScreen(
    animals: List<Animal>,
    onSelect: (AnimalSelection, String) -> Unit,
    modifier: Modifier = Modifier,
    previousPetMissingFromData: Boolean = false
) {
    val species = remember(animals) { animals.filter { it.variants.isNotEmpty() } }

    if (species.isEmpty()) {
        NoAnimalsMessage(modifier = modifier)
        return
    }

    var pickedAnimalId by rememberSaveable { mutableStateOf<String?>(null) }
    // The animal the id points at may be gone from the data by now, which simply puts the player
    // back on the first step instead of into a second step about nothing.
    val pickedAnimal = species.find { it.id == pickedAnimalId }

    if (pickedAnimal == null) {
        SpeciesStep(
            species = species,
            onPick = { animal -> pickedAnimalId = animal.id },
            previousPetMissingFromData = previousPetMissingFromData,
            modifier = modifier
        )
    } else {
        BackHandler { pickedAnimalId = null }

        VariantStep(
            animal = pickedAnimal,
            onBack = { pickedAnimalId = null },
            onConfirm = { variantId, name ->
                onSelect(
                    AnimalSelection(animalId = pickedAnimal.id, variantId = variantId),
                    name
                )
            },
            modifier = modifier
        )
    }
}

/**
 * First step: which animal the pet is going to be.
 *
 * Each animal is a single card here no matter how many variants it has — the variant is asked for on
 * the next step — and the card shows the animal in the first variant its data declares.
 *
 * @param species animals to choose from, each of them with at least one variant.
 * @param onPick called with the animal whose card was tapped; the caller moves on to the next step.
 * @param previousPetMissingFromData whether the player is here because the animal data no longer
 *   describes the pet they picked earlier; only changes the heading and the caption.
 * @param modifier modifier applied to the root column.
 */
@Composable
private fun SpeciesStep(
    species: List<Animal>,
    onPick: (Animal) -> Unit,
    previousPetMissingFromData: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(SelectScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (previousPetMissingFromData) "Выбери питомца заново" else "Выбери питомца",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (previousPetMissingFromData) {
                "Твой питомец никуда не делся — просто обновилась игра, " +
                    "и такого питомца в ней больше нет. Выбери другого: " +
                    "он будет расти вместе с твоими накоплениями"
            } else {
                "Он будет расти вместе с твоими накоплениями"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimalGrid(modifier = Modifier.weight(1f)) {
            items(items = species, key = { animal -> animal.id }) { animal ->
                AnimalCard(
                    spritePath = animal.getIdleSpritePath(
                        animal.variants.keys.first(),
                        Animal.FIRST_AGE
                    ),
                    description = animal.name,
                    title = animal.name,
                    selected = false,
                    onClick = { onPick(animal) }
                )
            }
        }
    }
}

/**
 * Second step: what the picked animal looks like and what it is called.
 *
 * The first variant is picked from the start, so the player who is happy with it only has to name
 * the pet and confirm. The name starts out as the animal's own name and is the player's to change;
 * a name wiped out completely falls back to that name rather than leaving the pet nameless.
 *
 * @param animal the animal picked on the first step.
 * @param onBack called when the player wants to pick a different animal after all.
 * @param onConfirm called with the picked variant and the name when the choice is confirmed.
 * @param modifier modifier applied to the root column.
 */
@Composable
private fun VariantStep(
    animal: Animal,
    onBack: () -> Unit,
    onConfirm: (variantId: String, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val variantIds = remember(animal) { animal.variants.keys.toList() }
    var selection by rememberSaveable(animal.id, stateSaver = AnimalSelectionSaver) {
        mutableStateOf<AnimalSelection?>(
            AnimalSelection(animalId = animal.id, variantId = variantIds.first())
        )
    }
    var name by rememberSaveable(animal.id) { mutableStateOf(animal.name) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(SelectScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = animal.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Выбери вид и придумай имя",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimalGrid(modifier = Modifier.weight(1f)) {
            items(items = variantIds, key = { variantId -> variantId }) { variantId ->
                AnimalCard(
                    spritePath = animal.getIdleSpritePath(variantId, Animal.FIRST_AGE),
                    description = animal.name,
                    title = null,
                    selected = variantId == selection?.variantId,
                    onClick = {
                        selection = AnimalSelection(
                            animalId = animal.id,
                            variantId = variantId
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { typed -> name = typed.take(MaxPetNameLength) },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = NameFieldMaxWidth),
            label = { Text(text = "Имя") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(text = "Назад", onClick = onBack)
            PillButton(
                text = "Выбрать",
                onClick = {
                    selection?.let { picked ->
                        onConfirm(picked.variantId, name.trim().ifBlank { animal.name })
                    }
                },
                enabled = selection != null
            )
        }
    }
}

/**
 * The grid both steps lay their cards out in, so an animal and a variant are always the same size.
 *
 * @param modifier modifier applied to the grid.
 * @param content the cards of the step, added the way [LazyVerticalGrid] takes them.
 */
@Composable
private fun AnimalGrid(
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = AnimalCellMinSize),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

/**
 * What the player gets instead of the picker when the animal data holds no animal at all.
 *
 * This is a dead end by nature: without animals there is no pet, and the game cannot be started at
 * all. The screen says so plainly rather than showing an empty grid with a button that never
 * becomes pressable — and it doesn't pretend that something is still loading, because nothing is.
 *
 * @param modifier modifier applied to the root column.
 */
@Composable
private fun NoAnimalsMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(SelectScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Выбирать не из кого",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Данные о животных не прочитались: в игре не осталось ни одного питомца, " +
                "и начать её сейчас нельзя. Перезапуск и переустановка тут не помогут — " +
                "нужна исправленная версия игры.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Card of a single choice: a sprite and, on the first step, the name of the animal under it. The
 * variants of one animal are told apart by the sprite alone, so their cards carry no caption —
 * a variant id is something the data files say, not something the player reads.
 *
 * @param spritePath path to the sprite the card shows, relative to `assets/textures/`.
 * @param description what the sprite is, for screen readers.
 * @param title caption under the sprite, or null for a card that shows the sprite alone.
 * @param selected whether this card is the one the player currently picked; a selected card gets
 *   a thicker, accented border.
 * @param onClick called when the card is tapped.
 * @param modifier modifier applied to the card surface.
 */
@Composable
private fun AnimalCard(
    spritePath: String,
    description: String,
    title: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, GameColors.cardStroke)
        },
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Sprite(
                assetPath = spritePath,
                contentDescription = description,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            if (title != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Animals used by the previews of [AnimalSelectScreen]. */
private val PreviewAnimals = listOf(
    Animal(
        id = "cat",
        name = "Кот",
        ageCount = 3,
        variants = mapOf("orange" to "animals/cat/orange", "white" to "animals/cat/white")
    ),
    Animal(
        id = "dog",
        name = "Пёс",
        ageCount = 2,
        variants = mapOf("brown" to "animals/dog/brown")
    )
)

/** Preview of the first step of [AnimalSelectScreen] in the light theme. */
@Preview(name = "AnimalSelectScreen — Light", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AnimalSelectScreenLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AnimalSelectScreen(animals = PreviewAnimals, onSelect = { _, _ -> })
        }
    }
}

/** Preview of the first step of [AnimalSelectScreen] in the dark theme. */
@Preview(name = "AnimalSelectScreen — Dark", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AnimalSelectScreenDarkPreview() {
    FinGameTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AnimalSelectScreen(animals = PreviewAnimals, onSelect = { _, _ -> })
        }
    }
}

/** Preview of the second step: the variants of one animal and the field for its name. */
@Preview(name = "AnimalSelectScreen — Variants", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AnimalSelectScreenVariantsPreview() {
    FinGameTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            VariantStep(
                animal = PreviewAnimals.first(),
                onBack = {},
                onConfirm = { _, _ -> }
            )
        }
    }
}

/** Preview of [AnimalSelectScreen] shown to a player whose pet is gone from the animal data. */
@Preview(
    name = "AnimalSelectScreen — Pet missing from data",
    showBackground = true,
    widthDp = 411,
    heightDp = 891
)
@Composable
private fun AnimalSelectScreenPetMissingPreview() {
    FinGameTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AnimalSelectScreen(
                animals = PreviewAnimals,
                onSelect = { _, _ -> },
                previousPetMissingFromData = true
            )
        }
    }
}

/** Preview of [AnimalSelectScreen] with no animals in the data at all. */
@Preview(name = "AnimalSelectScreen — Empty", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AnimalSelectScreenEmptyPreview() {
    FinGameTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AnimalSelectScreen(animals = emptyList(), onSelect = { _, _ -> })
        }
    }
}
