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
 * First-launch screen where the player picks the animal to play with.
 *
 * Every variant of every animal is a card of its own, because an animal is identified by its id
 * plus the variant it was created with. Cards show the animal at its first age stage
 * ([Animal.FIRST_AGE]) — that is how the pet starts out.
 *
 * Layout: title and caption at the top, a scrollable grid of animal cards below them, and a
 * "Выбрать" button at the bottom which stays disabled until a card is picked.
 *
 * @param animals animals the player can choose from, as read from the animal data.
 * @param onSelect called with the picked animal and variant when the player confirms the choice;
 *   the caller is the one that persists it.
 * @param modifier modifier applied to the screen root.
 */
@Composable
fun AnimalSelectScreen(
    animals: List<Animal>,
    onSelect: (AnimalSelection) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember(animals) {
        animals.flatMap { animal -> animal.variants.keys.map { variantId -> animal to variantId } }
    }
    var selection by remember { mutableStateOf<AnimalSelection?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(SelectScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Выбери питомца",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Он будет расти вместе с твоими накоплениями",
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
