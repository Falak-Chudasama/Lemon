package com.coconut.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object AppIconCache {
    // 150 items cached at ~20KB each = ~3MB memory footprint
    private val maxCacheSize = 150
    private val cache = LruCache<String, ImageBitmap>(maxCacheSize)
    private val inFlight = ConcurrentHashMap<String, Boolean>()
    private val lock = Mutex()

    fun get(packageName: String): ImageBitmap? {
        return cache.get(packageName)
    }

    suspend fun loadIcon(context: Context, packageName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        cache.get(packageName)?.let { return@withContext it }

        if (inFlight.putIfAbsent(packageName, true) != null) {
            // Already being loaded by another coroutine
            return@withContext cache.get(packageName)
        }

        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val drawable = appInfo.loadIcon(pm)
            val bitmap = drawableToBitmap(drawable)
            val imageBitmap = bitmap.asImageBitmap()

            lock.withLock {
                cache.put(packageName, imageBitmap)
            }
            imageBitmap
        } catch (_: Exception) {
            null
        } finally {
            inFlight.remove(packageName)
        }
    }

    fun clear() {
        cache.evictAll()
        inFlight.clear()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            val b = drawable.bitmap
            if (b.width > 0 && b.height > 0) {
                // Resize to standard 72x72 to minimize memory and maximize scrolling frame rate
                return Bitmap.createScaledBitmap(b, 72, 72, true)
            }
        }
        val bitmap = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, 72, 72)
        drawable.draw(canvas)
        return bitmap
    }
}
