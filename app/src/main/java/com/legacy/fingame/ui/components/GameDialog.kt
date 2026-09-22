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
 *   and centred, ending with the button that acts on it.
 */
@Composable
fun GameDialogBlock(
    title: String,
    closeDescription: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
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
            }
        }

        SpriteButton(
            assetPath = Sprites.CLOSE,
            contentDescription = closeDescription,
            onClick = onDismiss,
            size = DialogCloseButtonSize,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}
