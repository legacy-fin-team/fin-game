package com.legacy.fingame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.ui.components.Sprite
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.FinGameTheme

/** Screen stub for sections the customer has not specified yet. */
@Composable
fun PlaceholderScreen(
    title: String,
    iconPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        SpriteButton(
            assetPath = Sprites.CLOSE,
            contentDescription = "Закрыть",
            onClick = onBack,
            size = 48.dp,
            modifier = Modifier.align(Alignment.TopEnd)
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Sprite(
                assetPath = iconPath,
                contentDescription = title,
                modifier = Modifier.size(96.dp)
            )
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Раздел в разработке",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderPreview() {
    FinGameTheme {
        PlaceholderScreen(title = "Инвентарь", iconPath = Sprites.INVENTORY, onBack = {})
    }
}
