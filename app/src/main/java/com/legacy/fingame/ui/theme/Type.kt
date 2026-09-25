package com.legacy.fingame.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * FinGame game typography: titles are larger and bolder than standard Material3
 * to make the interface read as a friendly casual game. Uses system font by default
 * (no external Google Fonts).
 */
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * The scale the game is written in, in the pixel font.
 *
 * Five roles and no more, so a screen reads as one piece of paper rather than as a pile of them:
 * a screen title (`headlineSmall`, 20 sp), a card title (`titleMedium`, 16 sp), the text itself
 * (`bodyMedium`, 13 sp), a caption under it (`bodySmall`, 11 sp) and a number (`labelLarge`,
 * 14 sp), with `titleSmall` (12 sp) for the sub-heading of a group inside a card. A title is
 * always bigger than what stands under it — the one rule the old scale broke, where a card title
 * (14 sp) came out smaller than its own body text (15 sp).
 *
 * Sizes stop at 24 sp: the font is monospaced, so a line is exactly `characters × size` wide, and
 * anything above that no longer fits the narrowest screen the game is laid out for (360 dp) at the
 * largest font scale it is read at (1.3). `displayLarge`, `displayMedium`, `headlineLarge`,
 * `headlineMedium`, `titleLarge` and `bodyLarge` are kept only so the theme covers every Material
 * slot; nothing in the game is meant to ask for them — `headlineMedium` is the tablet's screen
 * title and the largest of them anything should ever use.
 *
 * Every slot is [FontWeight.Normal] on purpose: the family resolves every weight to the same
 * `press_start_2p_regular` file (see [PixelFont]), so asking for a bold one changes nothing and
 * only makes the code claim a hierarchy the screen does not have. Hierarchy here is size and color.
 */
val PixelTypography = Typography(
    displayLarge = pixelStyle(fontSize = 44, lineHeight = 50),
    displayMedium = pixelStyle(fontSize = 36, lineHeight = 42),
    headlineLarge = pixelStyle(fontSize = 30, lineHeight = 36),
    // Screen title on a tablet, where the same 20 sp would read as small print from arm's length.
    headlineMedium = pixelStyle(fontSize = 24, lineHeight = 32),
    // Screen title: "Бюджет", "Журнал", "Инвентарь".
    headlineSmall = pixelStyle(fontSize = 20, lineHeight = 28),
    titleLarge = pixelStyle(fontSize = 22, lineHeight = 28),
    // Card title, and the title of a window.
    titleMedium = pixelStyle(fontSize = 16, lineHeight = 22, letterSpacing = 0.1f),
    // Sub-heading of a group inside a card; always drawn in `onSurfaceVariant`.
    titleSmall = pixelStyle(fontSize = 12, lineHeight = 16, letterSpacing = 0.2f),
    bodyLarge = pixelStyle(fontSize = 17, lineHeight = 24, letterSpacing = 0.3f),
    // The text of the game, the label of a button included.
    bodyMedium = pixelStyle(fontSize = 13, lineHeight = 20, letterSpacing = 0.2f),
    // Caption and explanation under the text.
    bodySmall = pixelStyle(fontSize = 11, lineHeight = 16, letterSpacing = 0.4f),
    // Numbers: a sum, a price, how full a stat is.
    labelLarge = pixelStyle(fontSize = 14, lineHeight = 20, letterSpacing = 0.2f),
    labelMedium = pixelStyle(fontSize = 13, lineHeight = 18, letterSpacing = 0.3f),
    // Smallest caption there is: the word under an icon.
    labelSmall = pixelStyle(fontSize = 11, lineHeight = 16, letterSpacing = 0.5f)
)

/**
 * One style of [PixelTypography], so a slot states its sizes and nothing else.
 *
 * @param fontSize size of the text, in sp.
 * @param lineHeight height of one line of it, in sp.
 * @param letterSpacing room between the letters, in sp.
 * @return The style, in the game's pixel font.
 */
private fun pixelStyle(
    fontSize: Int,
    lineHeight: Int,
    letterSpacing: Float = 0f
): TextStyle = TextStyle(
    fontFamily = PixelFont,
    fontWeight = FontWeight.Normal,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp
)
