package com.legacy.fingame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
 * First-launch screen where the player picks the animal to play with.
 *
 * Every variant of every animal is a card of its own, because an animal is identified by its id
 * plus the variant it was created with. Cards show the animal at its first age stage
 * ([Animal.FIRST_AGE]) — that is how the pet starts out.
 *
 * Layout: title and caption at the top, a scrollable grid of animal cards below them, and a
 * "Выбрать" button at the bottom which stays disabled until a card is picked. When [animals] holds
 * no animal at all — the animal data is broken or empty — there is nothing to pick, so the grid and
 * the button are replaced by a message saying exactly that instead of an empty screen with a
 * button that can never be pressed.
 *
 * @param animals animals the player can choose from, as read from the animal data.
 * @param onSelect called with the picked animal and variant when the player confirms the choice;
 *   the caller is the one that persists it.
 * @param modifier modifier applied to the screen root.
 * @param previousPetLost whether the player already had a pet that is gone from the animal data,
 *   i.e. this is not the first launch and the earlier choice can no longer be played; the screen
 *   then explains that instead of greeting the player as a newcomer.
 */
@Composable
fun AnimalSelectScreen(
    animals: List<Animal>,
    onSelect: (AnimalSelection) -> Unit,
    modifier: Modifier = Modifier,
    previousPetLost: Boolean = false
) {
    val options = remember(animals) {
        animals.flatMap { animal -> animal.variants.keys.map { variantId -> animal to variantId } }
    }

    if (options.isEmpty()) {
        NoAnimalsMessage(modifier = modifier)
    } else {
        AnimalPicker(
            options = options,
            onSelect = onSelect,
            previousPetLost = previousPetLost,
            modifier = modifier
        )
    }
}

/**
 * The picker itself: the grid of animal cards plus the button that confirms the choice.
 *
 * The picked card is kept in [rememberSaveable], so rotating the phone or having the process
 * recreated doesn't drop the choice and disable the button again.
 *
 * @param options animal variants to show, as pairs of an animal and one of its variant ids.
 * @param onSelect called with the picked animal and variant when the player confirms the choice.
 * @param previousPetLost whether the player is here because the pet they had is gone from the
 *   animal data; only changes the title and the caption.
 * @param modifier modifier applied to the root column.
 */
@Composable
private fun AnimalPicker(
    options: List<Pair<Animal, String>>,
    onSelect: (AnimalSelection) -> Unit,
    previousPetLost: Boolean,
    modifier: Modifier = Modifier
) {
    var selection by rememberSaveable(stateSaver = AnimalSelectionSaver) {
        mutableStateOf<AnimalSelection?>(null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(SelectScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (previousPetLost) "Выбери нового питомца" else "Выбери питомца",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (previousPetLost) {
                "Питомца, с которым ты играл раньше, больше нет в игре. " +
                    "Новый будет расти вместе с твоими накоплениями"
            } else {
                "Он будет расти вместе с твоими накоплениями"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = AnimalCellMinSize),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = options,
                key = { (animal, variantId) -> "${animal.id}/$variantId" }
            ) { (animal, variantId) ->
                val option = AnimalSelection(animalId = animal.id, variantId = variantId)
                AnimalCard(
                    animal = animal,
                    variantId = variantId,
                    selected = option == selection,
                    onClick = { selection = option }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PillButton(
            text = "Выбрать",
            onClick = { selection?.let(onSelect) },
            enabled = selection != null
        )
    }
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
            style = MaterialTheme.typography.headlineMedium,
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
 * Card of a single animal variant: its sprite at the first age stage and its name.
 *
 * @param animal animal the card shows.
 * @param variantId variant of that animal the card shows.
 * @param selected whether this card is the one the player currently picked; a selected card gets
 *   a thicker, accented border.
 * @param onClick called when the card is tapped.
 * @param modifier modifier applied to the card surface.
 */
@Composable
private fun AnimalCard(
    animal: Animal,
    variantId: String,
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
                assetPath = animal.getIdleSpritePath(variantId, Animal.FIRST_AGE),
                contentDescription = animal.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = animal.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Animals used by the previews of [AnimalSelectScreen]. */
private val PreviewAnimals = listOf(
    Animal(
        id = "cat",
        title = "Кот",
        ageCount = 3,
        variants = mapOf("orange" to "animals/cat/orange", "white" to "animals/cat/white")
    ),
    Animal(
        id = "dog",
        title = "Пёс",
        ageCount = 2,
        variants = mapOf("brown" to "animals/dog/brown")
    )
)

/** Preview of [AnimalSelectScreen] in the light theme. */
@Preview(name = "AnimalSelectScreen — Light", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AnimalSelectScreenLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AnimalSelectScreen(animals = PreviewAnimals, onSelect = {})
        }
    }
}

/** Preview of [AnimalSelectScreen] in the dark theme. */
@Preview(name = "AnimalSelectScreen — Dark", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AnimalSelectScreenDarkPreview() {
    FinGameTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AnimalSelectScreen(animals = PreviewAnimals, onSelect = {})
        }
    }
}

/** Preview of [AnimalSelectScreen] shown to a player whose pet is gone from the animal data. */
@Preview(name = "AnimalSelectScreen — Pet lost", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AnimalSelectScreenPetLostPreview() {
    FinGameTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AnimalSelectScreen(animals = PreviewAnimals, onSelect = {}, previousPetLost = true)
        }
    }
}

/** Preview of [AnimalSelectScreen] with no animals in the data at all. */
@Preview(name = "AnimalSelectScreen — Empty", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun AnimalSelectScreenEmptyPreview() {
    FinGameTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AnimalSelectScreen(animals = emptyList(), onSelect = {})
        }
    }
}
