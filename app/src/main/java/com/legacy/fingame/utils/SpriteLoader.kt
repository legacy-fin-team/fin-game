package com.legacy.fingame.utils

import android.content.Context
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
 * @param context current local application context. Used to get access to /assets/ folder
 */
class SpriteLoader(private val context: Context) {

    private val tag = "SpriteLoader"

    /**
     * @param fullPath path relative to /assets/ folder
     * @return Drawable of an asset
     * @throws IOException in case of an error with decoding an image (e.g. file does not exist)
     */
    private fun getDrawable(
         fullPath: String
     ): Drawable {
         val inputStream = context.assets.open(fullPath)

         return inputStream.use { inputStream ->
             Drawable.createFromStream(inputStream, null)
                 ?: throw IOException("Failed to decode drawable: $fullPath")
         }
     }

    /**
     * @param assetPath path relative to /assets/textures/ folder
     * @return ImageRequest of an asset. If file does not exist returns the ERROR ImageRequest
     */
    fun getSprite(
        assetPath: String,
    ): ImageRequest {
        val fullPath = "textures/$assetPath"
        try {
            val drawable = getDrawable(fullPath)

            return ImageRequest.Builder(context)
                .crossfade(false)
                .data(drawable)
                .build()

        } catch (_: IOException) {
            Log.e(tag, "Unable to load sprite from: /assets/$fullPath")

            // Assume this file exists
            val errorPath = "textures/error/error.webp"
            val drawable = getDrawable(errorPath)

            return ImageRequest.Builder(context)
                .crossfade(false)
                .data(drawable)
                .build()
        }
    }
}