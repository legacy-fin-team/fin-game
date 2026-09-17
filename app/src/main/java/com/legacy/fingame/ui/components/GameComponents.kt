package com.legacy.fingame.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.legacy.fingame.ui.theme.FinGameTheme
import com.legacy.fingame.ui.theme.GameColors
import com.legacy.fingame.utils.SpriteLoader
import kotlin.math.roundToInt

/**
 * Draws a sprite from assets/textures/ via SpriteLoader + Coil.
 * Missing assets are expected to render error.webp for now — do not paper over this with fallbacks.
 */
@Composable
fun Sprite(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loader = remember(context) { SpriteLoader(context) }
    val request = remember(assetPath, loader) { loader.getSprite(assetPath) }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

/** Round tappable sprite. [selected] draws a primary-colored highlight ring (e.g. shop category tabs). */
@Composable
fun SpriteButton(
    assetPath: String,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 56.dp,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val ringColor = if (selected) MaterialTheme.colorScheme.primary else GameColors.cardStroke
    val ringWidth = if (selected) 2.dp else 1.dp

    Surface(
        modifier = modifier
            .size(size)
            .shadow(elevation = 4.dp, shape = CircleShape, clip = false)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = size / 2),
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(ringWidth, ringColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Sprite(
                assetPath = assetPath,
                contentDescription = null,
                modifier = Modifier
                    .size(size * 0.6f)
                    .padding(4.dp)
            )
        }
    }
}

/** Player balance pill: coin sprite + externally supplied text. No number formatting happens here. */
@Composable
fun BalanceChip(
    text: String = "Баланс",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.wrapContentSize(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, GameColors.cardStroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Sprite(
                assetPath = Sprites.COIN,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/** Current-goal card. [progress] is supplied by the caller; the only computation here is clamping and % formatting. */
@Composable
fun GoalCard(
    title: String = "текущая цель",
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val percentText = "${(clampedProgress * 100).roundToInt()}%"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, GameColors.cardStroke),
        shadowElevation = 2.dp
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.wrapContentSize()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { clampedProgress },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 0.dp),
                        color = GameColors.goalProgress,
                        trackColor = GameColors.goalProgressTrack
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = percentText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/** Pill-shaped button; muted appearance when [enabled] is false. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val containerColor = if (enabled) MaterialTheme.colorScheme.primary else GameColors.disabledContainer
    val contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else GameColors.disabledContent
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .wrapContentSize()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

// ---------- Previews ----------

@Preview(name = "Components — Light", showBackground = true)
@Composable
private fun GameComponentsLightPreview() {
    FinGameTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PreviewContent()
        }
    }
}

@Preview(name = "Components — Dark", showBackground = true)
@Composable
private fun GameComponentsDarkPreview() {
    FinGameTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PreviewContent()
        }
    }
}

@Composable
private fun PreviewContent() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Sprite(assetPath = Sprites.pet("cat"), contentDescription = "Питомец", modifier = Modifier.size(56.dp))
            SpriteButton(
                assetPath = Sprites.SHOP,
                contentDescription = "Открыть магазин",
                onClick = {}
            )
            SpriteButton(
                assetPath = Sprites.LOCATIONS,
                contentDescription = "Локации",
                onClick = {},
                selected = true
            )
            BalanceChip(text = "12 400 ₽")
        }
        GoalCard(title = "Велосипед", progress = 0.64f)
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            PillButton(text = "Купить", onClick = {})
            PillButton(text = "Недоступно", onClick = {}, enabled = false)
        }
    }
}
