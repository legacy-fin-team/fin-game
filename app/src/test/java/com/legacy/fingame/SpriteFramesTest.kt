package com.legacy.fingame

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import com.legacy.fingame.utils.SpriteLoader
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Какой кадр спрайта показывается: анимированный спрайт играет свои кадры только при включённых
 * анимациях, а при выключенных заменяется первым кадром.
 *
 * Сами кадры листает платформа (анимированный WebP декодируется в [Animatable]), поэтому здесь
 * проверяется решение [SpriteLoader], а не декодирование.
 */
class SpriteFramesTest {

    private open class StillDrawable : Drawable() {
        override fun draw(canvas: Canvas) = Unit
        override fun setAlpha(alpha: Int) = Unit
        override fun setColorFilter(colorFilter: ColorFilter?) = Unit
        @Deprecated("Deprecated in Java")
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }

    private class MovingDrawable : StillDrawable(), Animatable {
        override fun start() = Unit
        override fun stop() = Unit
        override fun isRunning() = false
    }

    @Test
    fun `an animated sprite plays its frames while the animations are on`() {
        assertTrue(SpriteLoader.keepsFrames(MovingDrawable(), animated = true))
    }

    @Test
    fun `an animated sprite is replaced by its first frame while the animations are off`() {
        assertFalse(SpriteLoader.keepsFrames(MovingDrawable(), animated = false))
    }

    @Test
    fun `a static sprite is drawn as it is either way`() {
        assertTrue(SpriteLoader.keepsFrames(StillDrawable(), animated = true))
        assertTrue(SpriteLoader.keepsFrames(StillDrawable(), animated = false))
    }
}
