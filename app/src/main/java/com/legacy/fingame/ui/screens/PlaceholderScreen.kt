package com.legacy.fingame.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.ui.components.GameIcon
import com.legacy.fingame.ui.theme.FinGameTheme

/**
 * Экран-заглушка для разделов, требования по которым заказчик ещё не описал
 * (инвентарь, квесты, локации, опции). Сохраняет навигацию рабочей.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    emoji: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = "✕",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable(onClick = onBack)
                .semantics { contentDescription = "Закрыть" }
                .padding(8.dp)
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GameIcon(emoji = emoji, size = 96.dp)
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Раздел в разработке",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderPreview() {
    FinGameTheme { PlaceholderScreen(title = "Инвентарь", emoji = "🎒", onBack = {}) }
}
