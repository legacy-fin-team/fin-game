package com.legacy.fingame.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.legacy.fingame.R

/**
 * The font the whole game is written in — Press Start 2P, one file and one weight.
 *
 * Every [FontWeight] below resolves to that same `press_start_2p_regular`, because the family has
 * no other file to resolve to: the weights are listed only so Compose never has to synthesize one
 * by smearing the glyphs sideways. The consequence is worth spelling out, since it is invisible at
 * the call site: **`fontWeight` on this family changes nothing on screen.** A `FontWeight.Bold`
 * written next to a [androidx.compose.ui.text.TextStyle] here is not a stronger title, it is a
 * reader of the code being told a lie. Hierarchy in this game is built out of size and color only
 * — see [PixelTypography].
 *
 * The font is monospaced as well: every glyph takes exactly one em, so a line of `n` characters at
 * a size of `s` sp comes out `n × s × fontScale` dp wide. That is what makes it possible to work
 * out in advance whether a label fits a screen instead of finding out on the device.
 */
val PixelFont = FontFamily(
    Font(
        resId = R.font.press_start_2p_regular,
        weight = FontWeight.ExtraLight
    ),
    Font(
        resId = R.font.press_start_2p_regular,
        weight = FontWeight.Thin
    ),
    Font(
        resId = R.font.press_start_2p_regular,
        weight = FontWeight.Light
    ),
    Font(
        resId = R.font.press_start_2p_regular,
        weight = FontWeight.Normal
    ),
    Font(
        resId = R.font.press_start_2p_regular,
        weight = FontWeight.Medium
    ),
    Font(
        resId = R.font.press_start_2p_regular,
        weight = FontWeight.Bold
    ),
    Font(
        resId = R.font.press_start_2p_regular,
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.press_start_2p_regular,
        weight = FontWeight.Black
    ),
    Font(
        resId = R.font.press_start_2p_regular,
        weight = FontWeight.ExtraBold
    ),
)