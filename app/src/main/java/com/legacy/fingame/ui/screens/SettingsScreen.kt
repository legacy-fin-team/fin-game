package com.legacy.fingame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.legacy.fingame.game.settings.GameSettings
import com.legacy.fingame.game.settings.ThemeMode
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors

private val ScreenPadding = 16.dp
private val CloseButtonSize = 64.dp

/**
 * Экран настроек.
 *
 * Макет:
 * - Кнопка закрытия (X) в правом верхнем углу.
 * - Чекбоксы «Звуки» и «Музыка».
 * - Выбор темы: Светлая / Тёмная / Авто.
 * - Кнопка «Режим взрослого» → переход на отдельный экран (заглушка).
 * - Кнопка «Сбросить прогресс» → модальное окно с подтверждением.
 *
 * @param settings текущие настройки игры.
 * @param onSettingsChanged вызывается при изменении любого параметра.
 * @param onOpenAdultMode открыть экран «Режим взрослого».
 * @param onResetProgress вызывается при подтверждении сброса прогресса.
 * @param onBack закрыть экран настроек.
 * @param modifier модификатор для корневого контейнера.
 */
@Composable
fun SettingsScreen(
    settings: GameSettings,
    onSettingsChanged: (GameSettings) -> Unit,
    onOpenAdultMode: () -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Верхняя панель с заголовком и кнопкой закрытия (X)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            SpriteButton(
                assetPath = Sprites.CLOSE,
                contentDescription = "Закрыть настройки",
                onClick = onBack,
                size = CloseButtonSize
            )
        }

        // --- Звуки ---
        SettingCheckbox(
            label = "Звуки",
            checked = settings.soundEnabled,
            onCheckedChange = { onSettingsChanged(settings.copy(soundEnabled = it)) }
        )

        // --- Музыка ---
        SettingCheckbox(
            label = "Музыка",
            checked = settings.musicEnabled,
            onCheckedChange = { onSettingsChanged(settings.copy(musicEnabled = it)) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // --- Выбор темы ---
        Text(
            text = "Тема",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.LIGHT -> "Светлая"
                    ThemeMode.DARK -> "Тёмная"
                    ThemeMode.AUTO -> "Авто"
                }
                ThemeChip(
                    label = label,
                    selected = settings.themeMode == mode,
                    onClick = { onSettingsChanged(settings.copy(themeMode = mode)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Режим взрослого ---
        SettingsButton(
            text = "Режим взрослого",
            onClick = onOpenAdultMode
        )

        // --- Сбросить прогресс ---
        SettingsButton(
            text = "Сбросить прогресс",
            onClick = { showResetDialog = true }
        )
    }

    // Модальное окно подтверждения сброса прогресса
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(text = "Сброс прогресса")
            },
            text = {
                Text(text = "Вы уверены, что хотите сбросить весь прогресс? Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onResetProgress()
                }) {
                    Text("Сбросить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

/**
 * Строка настройки с чекбоксом.
 */
@Composable
private fun SettingCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(12.dp))
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

/**
 * Чип выбора темы в виде pill-кнопки.
 */
@Composable
private fun ThemeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Большая кнопка настроек (как на макете: «Режим взрослого», «Сбросить прогресс»).
 */
@Composable
private fun SettingsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GameColors.cardStroke),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Preview of [SettingsScreen] in the light theme. */
@Preview(name = "Settings — Light", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun SettingsScreenLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(
                settings = GameSettings(),
                onSettingsChanged = {},
                onOpenAdultMode = {},
                onResetProgress = {},
                onBack = {}
            )
        }
    }
}

/** Preview of [SettingsScreen] in the dark theme. */
@Preview(name = "Settings — Dark", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun SettingsScreenDarkPreview() {
    FinGameTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(
                settings = GameSettings(soundEnabled = false, themeMode = ThemeMode.DARK),
                onSettingsChanged = {},
                onOpenAdultMode = {},
                onResetProgress = {},
                onBack = {}
            )
        }
    }
}
