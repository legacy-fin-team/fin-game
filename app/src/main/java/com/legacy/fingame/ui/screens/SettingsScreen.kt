package com.legacy.fingame.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.legacy.fingame.game.settings.GameSettings
import com.legacy.fingame.game.settings.ThemeMode
import com.legacy.fingame.ui.components.SpriteButton
import com.legacy.fingame.ui.components.Sprites
import com.legacy.fingame.ui.theme.DialogWindowMotion
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors

private val ScreenPadding = 16.dp
private val CloseButtonSize = 64.dp

/**
 * Экран настроек.
 *
 * Макет:
 * - Кнопка закрытия (X) в правом верхнем углу.
 * - Слайдеры громкости «Звуки» и «Музыка».
 * - Переключатель «Анимации».
 * - Выбор темы: Светлая / Тёмная / Авто.
 * - Кнопка «Режим взрослого» → переход на отдельный экран (заглушка).
 * - Кнопка «Помощь» → список игровых терминов с объяснением.
 * - Кнопка «Сбросить прогресс» → модальное окно с подтверждением.
 *
 * @param settings текущие настройки игры.
 * @param onSettingsChanged вызывается при изменении любого параметра.
 * @param onOpenAdultMode открыть экран «Режим взрослого».
 * @param onOpenHelp открыть экран «Помощь» со списком терминов.
 * @param onResetProgress вызывается при подтверждении сброса прогресса.
 * @param onBack закрыть экран настроек.
 * @param modifier модификатор для корневого контейнера.
 */
@Composable
fun SettingsScreen(
    settings: GameSettings,
    onSettingsChanged: (GameSettings) -> Unit,
    onOpenAdultMode: () -> Unit,
    onOpenHelp: () -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // В альбомной ориентации задаем дополнительные отступы от боковых краев (выреза вырезов/камеры) и снизу (от свайп-панели навигации)
    val horizontalPadding = if (isLandscape) 64.dp else ScreenPadding
    val bottomPadding = if (isLandscape) 40.dp else ScreenPadding

    Column(
        modifier = modifier
            .fillMaxSize()
            .displayCutoutPadding()
            .systemBarsPadding()
            .verticalScroll(scrollState)
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = ScreenPadding,
                bottom = bottomPadding
            ),
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
        SettingSlider(
            label = "Звуки",
            value = settings.soundVolume,
            onValueChange = { onSettingsChanged(settings.copy(soundVolume = it)) }
        )

        // --- Музыка ---
        SettingSlider(
            label = "Музыка",
            value = settings.musicVolume,
            onValueChange = { onSettingsChanged(settings.copy(musicVolume = it)) }
        )

        // --- Анимации ---
        SettingSwitch(
            label = "Анимации",
            hint = "Движение питомца, сердечек и окон",
            checked = settings.animationsEnabled,
            onCheckedChange = { onSettingsChanged(settings.copy(animationsEnabled = it)) }
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

        // --- Помощь ---
        SettingsButton(
            text = "Помощь",
            onClick = onOpenHelp
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
                DialogWindowMotion()
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
 * Элемент настройки со слайдером уровня громкости в процентах (0..100).
 */
@Composable
private fun SettingSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$value%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(0, 100)) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

/**
 * Настройка «включено / выключено»: подпись, короткое пояснение под ней и переключатель справа.
 *
 * Нажимается вся строка, а не только переключатель, так что попасть в неё легко; программа чтения
 * с экрана слышит её как один переключатель с подписью и состоянием.
 *
 * @param label название настройки.
 * @param hint что именно она включает, простыми словами.
 * @param checked включена ли настройка.
 * @param onCheckedChange вызывается с новым состоянием, когда строку нажали.
 */
@Composable
private fun SettingSwitch(
    label: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            // Нажатие ловит вся строка (см. toggleable выше).
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
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
                onOpenHelp = {},
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
                settings = GameSettings(
                    soundVolume = 0,
                    themeMode = ThemeMode.DARK,
                    animationsEnabled = false
                ),
                onSettingsChanged = {},
                onOpenAdultMode = {},
                onOpenHelp = {},
                onResetProgress = {},
                onBack = {}
            )
        }
    }
}

/** Preview of [SettingsScreen] in landscape orientation. */
@Preview(name = "Settings — Landscape", showBackground = true, widthDp = 891, heightDp = 411)
@Composable
private fun SettingsScreenLandscapePreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(
                settings = GameSettings(),
                onSettingsChanged = {},
                onOpenAdultMode = {},
                onOpenHelp = {},
                onResetProgress = {},
                onBack = {}
            )
        }
    }
}
