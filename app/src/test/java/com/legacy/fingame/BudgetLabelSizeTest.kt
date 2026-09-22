package com.legacy.fingame

import com.legacy.fingame.ui.screens.sharedLabelSizeOf
import org.junit.Assert.assertEquals
import org.junit.Test

/** Общий кегль подписей «Обязательные» и «Необязательные» в раскладке бюджета. */
class BudgetLabelSizeTest {

    /** «Необязательные» — самая длинная подпись группы. */
    private val longest = "Необязательные".length

    @Test
    fun `a label that fits keeps its own size`() {
        // 411 dp, шрифт 1.0: подписи остаётся 267 dp, а 15 знаков по 13 — это 195.
        assertEquals(13f, sharedLabelSizeOf(labelRoom = 267f, longestLabel = longest, styleSize = 13f, floor = 9f))
    }

    @Test
    fun `a label too long for the row shrinks just enough to fit`() {
        // 360 dp, шрифт 1.3: подписи остаётся 216 dp, обычный кегль — 16.9, и влезает только 14.4.
        val size = sharedLabelSizeOf(labelRoom = 216f, longestLabel = longest, styleSize = 16.9f, floor = 9f)
        assertEquals(14.4f, size, 0.01f)
        assert(size * (longest + 1) <= 216f)
    }

    @Test
    fun `a label never shrinks below the floor`() {
        assertEquals(9f, sharedLabelSizeOf(labelRoom = 60f, longestLabel = longest, styleSize = 16.9f, floor = 9f))
    }

    @Test
    fun `nothing to measure leaves the size as it is`() {
        assertEquals(13f, sharedLabelSizeOf(labelRoom = 0f, longestLabel = longest, styleSize = 13f, floor = 9f))
        assertEquals(13f, sharedLabelSizeOf(labelRoom = 200f, longestLabel = 0, styleSize = 13f, floor = 9f))
    }
}
