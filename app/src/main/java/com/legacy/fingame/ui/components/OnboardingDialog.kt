package com.legacy.fingame.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors

/** Same card width every window of the game shares; see [GameDialogBlock]. */
private val CardMaxWidth = 320.dp

/** How much of the screen's own height the card may take at most, so it never runs off it. */
private const val CardMaxHeightFraction = 0.86f

/** Padding of the card and the gap between its blocks. */
private val CardPadding = 20.dp
private val BlockGap = 16.dp

/** Room between a block's icon and its text, and between the block's title and its body. */
private val BlockIconGap = 12.dp
private val BlockTextGap = 4.dp

/**
 * One small block of the onboarding window: a decorative icon or emoji, a short title and a line of
 * child-friendly text under it.
 *
 * @property icon emoji or short icon shown beside the block; purely decorative, so it is hidden
 * from TalkBack rather than read out by whatever Unicode calls it.
 * @property title what the block is about, in one or two words.
 * @property text the block's own short explanation.
 */
data class OnboardingBlock(
    val icon: String,
    val title: String,
    val text: String
)

/**
 * The three things a new player is told about, in the language of an 8-to-12-year-old: what the
 * game is for, what money has to go on, and that not all of it has to be spent right away.
 *
 * The categories named here are the real ones the shop and the budget screen use — food and toys
 * as what the pet needs ([com.legacy.fingame.game.economy.SpendKind.MUST]), clothes and decorations
 * as what is nice to have ([com.legacy.fingame.game.economy.SpendKind.WANT]) — rather than invented
 * ones, so the window teaches words the rest of the game actually uses.
 */
val OnboardingBlocks: List<OnboardingBlock> = listOf(
    OnboardingBlock(
        icon = "🐾",
        title = "Твой питомец",
        text = "Корми его, играй с ним и заботься о нём — тогда он будет расти большим и весёлым!"
    ),
    OnboardingBlock(
        icon = "🍎",
        title = "Нужное",
        text = "Еда и игрушки — то, без чего питомцу не обойтись. Их покупай в первую очередь."
    ),
    OnboardingBlock(
        icon = "⭐",
        title = "Можно и накопить",
        text = "Одежду и украшения бери, когда захочется. А монеты можно откладывать: копить на " +
            "цель или положить на вклад — банк потом добавит ещё немного сверху!"
    )
)

/**
 * The window shown once, the very first time the app is ever launched: it explains what the game is
 * about — looking after the pet — and the three things the player's money can go on. See
 * [com.legacy.fingame.game.OnboardingGate] for when it is shown.
 *
 * Unlike [GameDialog], this window is not dismissed by tapping outside it or by the system back
 * gesture: [onDismiss] fires only from the "Понятно!" button, so a player cannot lose the
 * explanation without at least being asked to confirm they read it.
 *
 * The card never grows past [CardMaxHeightFraction] of the screen's own height, and its content
 * scrolls inside that limit — so the window still fits, and is still fully readable, in landscape
 * on a short phone screen and at the largest system font the game supports.
 *
 * @param onDismiss called when the player taps "Понятно!".
 * @param modifier modifier applied to the outer centering box.
 */
@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        // Nothing this window can be dismissed by counts as "outside" it: the system back press and
        // a tap off the card are both turned off below, so this is never actually invoked — only the
        // button inside the card calls onDismiss.
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        val maxCardHeight = LocalConfiguration.current.screenHeightDp.dp * CardMaxHeightFraction

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = CardMaxWidth)
                    .heightIn(max = maxCardHeight),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, GameColors.cardStroke),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(CardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Привет!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    OnboardingBlocks.forEach { block ->
                        Box(modifier = Modifier.padding(top = BlockGap)) {
                            OnboardingBlockRow(block)
                        }
                    }

                    Box(modifier = Modifier.padding(top = BlockGap)) {
                        PillButton(
                            text = "Понятно!",
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            style = PillStyle.Primary,
                            compact = true
                        )
                    }
                }
            }
        }
    }
}

/**
 * One row of [OnboardingDialog]: the block's icon beside a column with its title over its text.
 *
 * @param block what the row shows.
 * @param modifier modifier applied to the row.
 */
@Composable
private fun OnboardingBlockRow(block: OnboardingBlock, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = block.icon,
            // Decorative: the title and the text right next to it already say what the block is
            // about, so a screen reader is spared whatever name it has for the glyph.
            modifier = Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.headlineSmall
        )
        Box(modifier = Modifier.padding(start = BlockIconGap)) {
            Column {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(modifier = Modifier.padding(top = BlockTextGap)) {
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Onboarding window on the narrowest screen the game is laid out for. */
@Preview(name = "Onboarding — 360dp", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingDialogPreview() {
    FinGameTheme(darkTheme = false) {
        OnboardingDialog(onDismiss = {})
    }
}

/** Onboarding window at the largest system font the game supports. */
@Preview(
    name = "Onboarding — 360dp, font 1.3",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.3f
)
@Composable
private fun OnboardingDialogLargeFontPreview() {
    FinGameTheme(darkTheme = false) {
        OnboardingDialog(onDismiss = {})
    }
}

/** Onboarding window in landscape on a short phone screen: the card scrolls instead of overflowing. */
@Preview(name = "Onboarding — Landscape", showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun OnboardingDialogLandscapePreview() {
    FinGameTheme(darkTheme = false) {
        OnboardingDialog(onDismiss = {})
    }
}
