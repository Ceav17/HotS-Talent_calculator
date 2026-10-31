package com.github.ceav17.hots_talent_calculator

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.FileNotFoundException
import java.io.IOException
import org.json.JSONObject

// Share decoded artwork between hero pages and build screens without retaining every hero in RAM.
private val assetImageCache = object : LruCache<Pair<AssetManager, String>, Bitmap>(8 * 1024 * 1024) {
    override fun sizeOf(key: Pair<AssetManager, String>, value: Bitmap): Int = value.allocationByteCount
}
private data class AssetImageIndex(val assets: AssetManager, val aliases: Map<String, String>)

@Volatile private var imageIndex: AssetImageIndex? = null
private var imageCacheGeneration = 0L

private fun resolveAssetImagePath(assets: AssetManager, path: String): String {
    val index = imageIndex?.takeIf { it.assets === assets } ?: run {
        // Publish an immutable index; file reads must never hold the UI's cache-eviction lock.
        val aliases = try {
            assets.open("data/image_aliases.json").bufferedReader().use { reader ->
                val data = JSONObject(reader.readText())
                data.keys().asSequence().associateWith { data.getString(it) }
            }
        } catch (_: FileNotFoundException) {
            emptyMap()
        }
        AssetImageIndex(assets, aliases).also { imageIndex = it }
    }
    // JSON keeps the original logical paths; the packer maps them to unique lossless images.
    return index.aliases[path] ?: path
}

internal fun loadAssetImage(assets: AssetManager, path: String): ImageBitmap {
    val actualPath = resolveAssetImagePath(assets, path)
    val key = assets to actualPath
    val generation = synchronized(assetImageCache) {
        assetImageCache.get(key)?.let { return it.asImageBitmap() }
        imageCacheGeneration
    }
    val bitmap = assets.open(actualPath).use { stream ->
        BitmapFactory.decodeStream(stream) ?: throw IOException("Invalid image: $actualPath")
    }
    bitmap.prepareToDraw()
    return synchronized(assetImageCache) {
        // An in-flight decode must not refill a cache just cleared for memory pressure.
        if (generation != imageCacheGeneration) bitmap else {
            assetImageCache.get(key) ?: bitmap.also { assetImageCache.put(key, it) }
        }
    }.asImageBitmap()
}

internal fun clearAssetImageCache() = synchronized(assetImageCache) {
    imageCacheGeneration++
    // Views can still reference evicted bitmaps; let GC release them instead of calling recycle().
    assetImageCache.evictAll()
}
