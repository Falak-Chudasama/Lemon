package com.coconut.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppIconCache {
    private val cache = LruCache<String, ImageBitmap>(300)

    fun get(packageName: String): ImageBitmap? {
        return cache.get(packageName)
    }

    fun put(packageName: String, bitmap: ImageBitmap) {
        cache.put(packageName, bitmap)
    }

    suspend fun loadIcon(context: Context, packageName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        cache.get(packageName)?.let { return@withContext it }
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val drawable = appInfo.loadIcon(pm)
            val bitmap = drawableToBitmap(drawable)
            val imageBitmap = bitmap.asImageBitmap()
            cache.put(packageName, imageBitmap)
            imageBitmap
        } catch (_: Exception) {
            null
        }
    }

    fun getOrDecode(packageName: String, drawable: Drawable?): ImageBitmap? {
        if (drawable == null) return null
        cache.get(packageName)?.let { return it }
        return try {
            val bitmap = drawableToBitmap(drawable)
            val imageBitmap = bitmap.asImageBitmap()
            cache.put(packageName, imageBitmap)
            imageBitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            val b = drawable.bitmap
            if (b.width > 0 && b.height > 0) {
                return Bitmap.createScaledBitmap(b, 96, 96, true)
            }
        }
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, 96, 96)
        drawable.draw(canvas)
        return bitmap
    }
}

