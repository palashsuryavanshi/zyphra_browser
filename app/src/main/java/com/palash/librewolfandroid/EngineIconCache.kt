package com.palash.librewolfandroid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import java.io.File
import java.net.URL

/**
 * Downloads and caches search-engine favicons so the toolbar badge can show
 * the engine's own icon instead of a single letter.
 *
 * Works for built-in engines and for engines the user adds: the favicon URL
 * is derived from the host of the engine's query URL, so any engine that
 * exposes a favicon at the conventional /favicon.ico path gets an icon.
 * Failures (network errors, 404s, undecodable images) leave the letter badge
 * in place rather than showing a broken image.
 */
object EngineIconCache {

    private val memoryCache = mutableMapOf<String, Bitmap>()

    /** Derives the favicon URL from the engine's query URL. */
    fun faviconUrl(engine: LibreWolfDefaults.SearchEngine): String? {
        return try {
            val host = java.net.URL(engine.queryUrl.replace("%s", "x")).host
            if (host.isNullOrBlank()) null
            // Brave serves its favicon from the root domain, not search.brave.com.
            else if (host == "search.brave.com") "https://brave.com/favicon.ico"
            else "https://$host/favicon.ico"
        } catch (_: Exception) {
            null
        }
    }

    /** Returns a cached bitmap, or null if not yet downloaded. */
    fun get(context: Context, engine: LibreWolfDefaults.SearchEngine): Bitmap? {
        val key = engine.name
        memoryCache[key]?.let { return it }
        val file = cacheFile(context, engine)
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                memoryCache[key] = bitmap
                return bitmap
            }
            // Corrupt file -- remove so we retry.
            file.delete()
        }
        return null
    }

    /** Downloads the favicon on a background thread and calls [onResult]. */
    fun loadAsync(
        context: Context,
        engine: LibreWolfDefaults.SearchEngine,
        onResult: (Bitmap?) -> Unit,
    ) {
        Thread {
            val bitmap = download(context, engine)
            if (bitmap != null) {
                memoryCache[engine.name] = bitmap
            }
            onResult(bitmap)
        }.start()
    }

    private fun download(context: Context, engine: LibreWolfDefaults.SearchEngine): Bitmap? {
        val url = faviconUrl(engine) ?: return null
        val file = cacheFile(context, engine)
        if (file.exists()) {
            val cached = BitmapFactory.decodeFile(file.absolutePath)
            if (cached != null) return cached
            file.delete()
        }
        return try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.inputStream.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    file.parentFile?.mkdirs()
                    file.outputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
                bitmap
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun cacheFile(context: Context, engine: LibreWolfDefaults.SearchEngine): File {
        val dir = File(context.cacheDir, "engine_icons")
        return File(dir, "${engine.name.hashCode()}.png")
    }

    /** Scales and circularly crops a bitmap to the given pixel size. */
    fun circularBitmap(bitmap: Bitmap, sizePx: Int): Bitmap {
        val scaled = Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true }
        val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        canvas.drawOval(rect, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, null, rect, paint)
        return output
    }
}