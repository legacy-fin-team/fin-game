package com.legacy.fingame

import android.content.Context
import android.graphics.drawable.Drawable
import coil3.request.ImageRequest
import coil3.request.crossfade
import java.io.IOException

class SpriteLoader(private val context: Context) {

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
     * @param assetPath path relative to assets/textures/ folder
     * @return ImageRequest of an asset. If file does not exist returns the ERROR ImageRequest.
     */
    fun getSprite(
        assetPath: String,
    ): ImageRequest {
        try {
            val fullPath = "textures/$assetPath"
            val drawable = getDrawable(fullPath)

            return ImageRequest.Builder(context)
                .crossfade(false)
                .data(drawable)
                .build()

        } catch (e: IOException) {
            e.printStackTrace()

            val errorPath = "textures/error/error.webp"
            val drawable = getDrawable(errorPath)

            return ImageRequest.Builder(context)
                .crossfade(false)
                .data(drawable)
                .build()
        }
    }
}