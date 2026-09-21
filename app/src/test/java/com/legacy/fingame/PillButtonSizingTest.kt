package com.legacy.fingame

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legacy.fingame.ui.components.pillButtonAutoSizeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The `min`/`max` pair a [PillButton][com.legacy.fingame.ui.components.PillButton] label's
 * `autoSize` is given: the rule that kept a label from being cropped instead of shrunk at a large
 * system font scale (see [pillButtonAutoSizeRange]'s own KDoc for the bug this rule fixes), checked
 * without a screen to run it on.
 */
class PillButtonSizingTest {

    @Test
    fun `at the normal font scale the floor converts to the same number of sp`() {
        val density = Density(density = 2f, fontScale = 1f)

        val (min, max) = pillButtonAutoSizeRange(
            minLabelSize = 9.dp,
            styleFontSize = 14.sp,
            density = density
        )

        // 1.sp is 1.dp's worth of physical size at fontScale 1, same as it would be without autoSize.
        assertEquals(9f, min.value, 0f)
        assertEquals(14f, max.value, 0f)
    }

    @Test
    fun `a bigger font scale lets the floor shrink further in sp, not less`() {
        // This is the fix itself: a 9.dp floor is physically the same size on screen at any font
        // scale, so as the scale grows the sp value it converts to has to fall, giving autoSize more
        // room below the label's (now bigger) normal size to shrink into rather than less.
        val density = Density(density = 2f, fontScale = 1.3f)

        val (min, max) = pillButtonAutoSizeRange(
            minLabelSize = 9.dp,
            styleFontSize = 14.sp,
            density = density
        )

        assertEquals(9f / 1.3f, min.value, 0.001f)
        assertEquals(14f, max.value, 0f)
        assertTrue("min must stay a real, positive floor below max", min < max)
    }

    @Test
    fun `an unusually small font scale never inverts the pair`() {
        // At a small enough font scale, converting a fixed dp floor to sp can land above the
        // style's own font size (9.dp is 30.sp at a font scale of 0.3, well past a 13.sp style).
        // TextAutoSize.StepBased would otherwise be handed an inverted min/max pair.
        val density = Density(density = 2f, fontScale = 0.3f)

        val (min, max) = pillButtonAutoSizeRange(
            minLabelSize = 9.dp,
            styleFontSize = 13.sp,
            density = density
        )

        assertEquals(13f, min.value, 0f)
        assertEquals(13f, max.value, 0f)
        assertTrue("min must never end up above max", min <= max)
    }

    @Test
    fun `the density itself does not affect the converted floor`() {
        // Dp-to-sp conversion is a font-scale ratio, not a density one: two screens at the same font
        // scale but different pixel densities must still get the same floor in sp.
        val lowDensity = pillButtonAutoSizeRange(9.dp, 14.sp, Density(density = 1f, fontScale = 1.2f))
        val highDensity = pillButtonAutoSizeRange(9.dp, 14.sp, Density(density = 3f, fontScale = 1.2f))

        assertEquals(lowDensity.first.value, highDensity.first.value, 0f)
    }
}
