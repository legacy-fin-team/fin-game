package com.legacy.fingame.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import coil3.request.ImageRequest
import coil3.request.crossfade
import java.io.IOException

/**
 * Handles sprite loading from /assets/textures/ folder.
 *
 * In case of [IOException] provides the special ERROR texture to indicate the error.
 *
 * Some sprites are animated WebP files (the goldfish swimming, the ERROR texture blinking): the
 * platform decodes them into an [Animatable] drawable that plays its own frames once Coil starts
 * it, with no frame counter of ours anywhere. With the animations turned off in the settings
 * ([getSprite] with `animated = false`) such a sprite is decoded as its first frame alone, a plain
 * bitmap, so it stands still and is drawn the same way as every static sprite.
 *
 * @param context current local application context. Used to get access to /assets/ folder
 */
class SpriteLoader(private val context: Context) {

    companion object {
        /**
         * The sprite drawn in place of one whose file is not in the assets, as a path relative to
         * /assets/textures/ folder.
         */
        private const val MISSING_SPRITE = "error/error.webp"

        /**
         * Whether a decoded sprite is handed on as it is or replaced with its first frame.
         *
         * @param drawable the sprite as the platform decoded it; an animated one is [Animatable].
         * @param animated whether the animations are on (the "Анимации" setting).
         * @return `true` when the drawable is kept: the animations are on, or it has no frames to
         *   play anyway. `false` for an animated sprite while the animations are off.
         */
        internal fun keepsFrames(drawable: Drawable, animated: Boolean): Boolean =
            animated || drawable !is Animatable
    }

    private val tag = "SpriteLoader"

    /**
     * @param fullPath path relative to /assets/ folder
     * @param animated whether an animated sprite should play its frames; when `false`, it is
     *   decoded as its first frame only (see [firstFrameOf]). A static sprite is the same either way.
     * @return Drawable of an asset
     * @throws IOException in case of an error with decoding an image (e.g. file does not exist)
     */
    private fun getDrawable(
         fullPath: String,
         animated: Boolean = true
     ): Drawable {
         val inputStream = context.assets.open(fullPath)

         val drawable = inputStream.use { inputStream ->
             Drawable.createFromStream(inputStream, null)
                 ?: throw IOException("Failed to decode drawable: $fullPath")
         }
         return if (keepsFrames(drawable, animated)) drawable else firstFrameOf(fullPath)
     }

    /**
     * Decodes only the first frame of a sprite: [BitmapFactory] reads an animated WebP as the
     * picture it opens with, which is exactly what the sprite shows before it starts moving.
     *
     * @param fullPath path relative to /assets/ folder
     * @return The first frame as a plain bitmap drawable.
     * @throws IOException in case of an error with decoding the image
     */
    private fun firstFrameOf(fullPath: String): Drawable {
        val bitmap = context.assets.open(fullPath).use { BitmapFactory.decodeStream(it) }
            ?: throw IOException("Failed to decode the first frame: $fullPath")
        return BitmapDrawable(context.resources, bitmap)
    }

    /**
     * @param assetPath path relative to /assets/textures/ folder
     * @return Full path of the asset, relative to /assets/ folder
     */
    private fun fullPathOf(assetPath: String): String = "textures/$assetPath"

    /**
     * Tells whether a sprite is in the assets at all, i.e. whether [getSprite] would hand back the
     * sprite itself rather than [MISSING_SPRITE].
     *
     * @param assetPath path relative to /assets/textures/ folder
     * @return True when the file is there and can be opened
     */
    fun hasSprite(assetPath: String): Boolean = try {
        context.assets.open(fullPathOf(assetPath)).close()
        true
    } catch (_: IOException) {
        false
    }

    /**
     * @param assetPath path relative to /assets/textures/ folder
     * @param animated whether an animated sprite plays its frames, i.e. the "Анимации" setting;
     *   when `false`, it shows its first frame and stands still. Static sprites do not care.
     * @return ImageRequest of an asset. If file does not exist returns the ERROR ImageRequest
     */
    fun getSprite(
        assetPath: String,
        animated: Boolean = true,
    ): ImageRequest {
        val fullPath = fullPathOf(assetPath)
        try {
            val drawable = getDrawable(fullPath, animated)

            return ImageRequest.Builder(context)
                .crossfade(false)
                .data(drawable)
                .build()

        } catch (_: IOException) {
            Log.e(tag, "Unable to load sprite from: /assets/$fullPath")

            // Assume this file exists
            val drawable = getDrawable(fullPathOf(MISSING_SPRITE), animated)

            return ImageRequest.Builder(context)
                .crossfade(false)
                .data(drawable)
                .build()
        }
    }
}