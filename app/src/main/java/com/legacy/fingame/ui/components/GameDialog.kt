package com.legacy.fingame.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.ui.theme.GameDimens

/** Sizes shared by the windows of the game; see [GameDialogBlock]. */
private val DialogMaxWidth = 320.dp
private val DialogCloseButtonSize = 44.dp

/** Room between the buttons of a window, and between them and what stands above them. */
private val DialogActionsGap = 8.dp

/**
 * The frame every window of the game is hung in: a window sized by what is in it rather than by
 * the platform's dialog width, so the card keeps the proportions of the rest of the game's windows,
 * and centred in the screen with a margin of its own.
 *
 * A tap outside it and the system back gesture close it, which is [onDismiss]'s job either way.
 *
 * @param onDismiss called when the window should be closed without anything happening.
 * @param content the block the window shows; see [GameDialogBlock].
 */
@Composable
fun GameDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * The card every window of the game is built out of, so all of them look like one game and not
 * like several: the same rounded surface, the same title over the same column of content, and the
 * cross that closes it.
 *
 * The cross sits on the top-right corner and hangs half-way over the edge of the card, the way the
 * inventory's item window wears it.
 *
 * The title wraps onto a second line rather than being cut short: a window's title is written to be
 * read whole, and the font scale it is read at is the player's to choose.
 *
 * @param title what the window is about, in one short line.
 * @param closeDescription what the cross does, announced to screen readers.
 * @param onDismiss called when the cross is pressed.
 * @param modifier modifier applied to the block root.
 * @param content what the window says under its title, laid out in the card's column: evenly spaced
 *   and centred.
 * @param actions the buttons the window ends with, stacked one under another and each as wide as
 *   the card: the action first, as a [PillStyle.Primary] pill, and under it the way out —
 *   "Отмена" — as a [PillStyle.Text] one. A column and not a row on purpose, and not only when the
 *   two do not fit beside each other: the card is 280 dp wide inside its padding, and "Подтвердить"
 *   next to "Отмена" wants 335 of them at the largest font scale the game is read at. Trying the row
 *   first and falling back would mean the same window looking like two different windows depending
 *   on the player's font setting, which is worse than a column that is always a column. `null` —
 *   the default — for a window that only tells the player something: no buttons, and no room kept
 *   under the text for the ones that are not there.
 */
@Composable
fun GameDialogBlock(
    title: String,
    closeDescription: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // Half of the cross hangs outside the card, so the card keeps that much room around itself.
    val overhang = GameDimens.buttonSize(DialogCloseButtonSize) / 2

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .padding(top = overhang, end = overhang)
                .widthIn(max = DialogMaxWidth),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GameColors.cardStroke),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                content()
                if (actions != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(DialogActionsGap),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = actions
                    )
                }
            }
        }

        SpriteButton(
            assetPath = Sprites.CLOSE,
            contentDescription = closeDescription,
            onClick = onDismiss,
            size = DialogCloseButtonSize,
            // The cross belongs to no group of tabs, so nothing is kept under it for an underline
            // it would never draw — which is what used to push it off the corner it hangs on.
            showIndicator = false,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}
