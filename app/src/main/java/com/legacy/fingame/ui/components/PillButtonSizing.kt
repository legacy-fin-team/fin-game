package com.legacy.fingame.ui.components

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * Smallest a [PillButton] label is ever allowed to shrink to, in [Dp] rather than
 * [androidx.compose.ui.unit.TextUnit.Sp].
 *
 * A size in `sp` already scales with the system's font size setting — `10.sp` is a real 13.sp at a
 * font scale of 1.3, not a floor at all — so a label whose own normal size grows with that setting
 * could run out of room to shrink into before it fit, and [PillButton] would fall back to cropping
 * it mid-word exactly like the wrap this whole mechanism exists to prevent. Keeping the floor in
 * `dp` and converting it with [pillButtonAutoSizeRange] keeps it the same physical size — and so the
 * same worst-case label width — no matter how the font scale is set.
 *
 * The value is a floor of readability, not of geometry: this is a pixel font, which goes unreadable
 * far earlier than an ordinary one does, and `11.dp` is where a child still reads a word on a phone
 * held at arm's length. A label that does not fit even at this size is a label to be shortened, not
 * to be shrunk further.
 */
val PillButtonMinLabelSize: Dp = 11.dp

/**
 * The `min`/`max` pair a [PillButton] label's `autoSize` shrinks within.
 *
 * @param minLabelSize the label's physical floor; see [PillButtonMinLabelSize] for why this is a
 *   [Dp] and not a font size directly.
 * @param styleFontSize the label's normal size, i.e. the `fontSize` of the [PillButton]'s
 *   [androidx.compose.ui.text.TextStyle]; the ceiling `autoSize` never grows past, since a button is
 *   never made to look bigger than its usual self, only smaller when it must.
 * @param density density (and font scale) the label is drawn at, needed to convert [minLabelSize]
 *   into the same unit as [styleFontSize].
 * @return [minLabelSize] converted to a font size, paired with [styleFontSize] — except at an
 *   unusually small font scale, where that conversion can come out *above* [styleFontSize] (an
 *   11.dp floor is 36.sp at a font scale of 0.3, well past a 13.sp style). The floor is capped to
 *   [styleFontSize] in that case, so the pair handed to `autoSize` is never inverted.
 */
fun pillButtonAutoSizeRange(
    minLabelSize: Dp,
    styleFontSize: TextUnit,
    density: Density
): Pair<TextUnit, TextUnit> {
    val floor = with(density) { minLabelSize.toSp() }
    val cappedFloor = if (floor > styleFontSize) styleFontSize else floor
    return cappedFloor to styleFontSize
}
