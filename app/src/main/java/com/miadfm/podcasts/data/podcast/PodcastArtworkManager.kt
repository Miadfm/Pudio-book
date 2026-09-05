package com.miadfm.podcasts.data.podcast

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Collections
import java.util.LinkedHashSet

/**
 * Manages thread-safe extraction, caching, and fallback handling for podcast/audio artwork.
 *
 * Requirements satisfied:
 * 1. Safely reads embedded MP3/audio artwork when available.
 * 2. Calling getEmbeddedPicture() is never treated as a fatal error.
 * 3. Handles null results and extraction failures safely with try/catch.
 * 4. Pre-checks ID3 tags and deduplicates extraction so getEmbeddedPicture() is not repeatedly called.
 * 5. Memory cache (artworkCache + noArtworkCache) prevents redundant processing.
 * 6. Returns null gracefully so clean built-in default artwork is displayed.
 * 7. Silences repeated Logcat errors for expected missing artwork.
 * 8. Releases MediaMetadataRetriever and file descriptors properly in finally blocks.
 * 9. Audio playback is completely decoupled and never impacted by artwork extraction.
 */
object PodcastArtworkManager {

    private const val TARGET_THUMBNAIL_SIZE = 512

    // Memory cache for decoded artwork (up to 10MB)
    private val artworkCache = object : LruCache<String, Bitmap>(10 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    // Fast-lookup set of keys verified to have no embedded artwork (or where extraction failed)
    private val noArtworkCache = Collections.synchronizedSet(LinkedHashSet<String>())

    // Serializes extraction so multiple composables do not concurrently trigger extraction for the same asset
    private val extractionMutex = Mutex()

    /**
     * Asynchronously loads embedded artwork for a podcast episode.
     * Returns null if no embedded artwork is found, allowing the UI to render the clean default cover.
     * Guaranteed not to block the main/UI thread.
     */
    suspend fun loadArtwork(
        context: Context,
        episode: Episode,
        targetSize: Int = TARGET_THUMBNAIL_SIZE
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = episode.assetPath.ifBlank { episode.id }
        if (cacheKey.isBlank()) return@withContext null

        // 1. Fast path: check memory cache for positive result
        artworkCache.get(cacheKey)?.let { return@withContext it }

        // 2. Fast path: check if already verified to have no embedded artwork
        if (noArtworkCache.contains(cacheKey)) {
            return@withContext null
        }

        // 3. Thread-safe extraction with mutex
        extractionMutex.withLock {
            // Double-check cache after acquiring lock
            artworkCache.get(cacheKey)?.let { return@withContext it }
            if (noArtworkCache.contains(cacheKey)) {
                return@withContext null
            }

            val bitmap = extractEmbeddedArtwork(context, episode.assetPath, targetSize)
            if (bitmap != null) {
                artworkCache.put(cacheKey, bitmap)
            } else {
                noArtworkCache.add(cacheKey)
            }
            bitmap
        }
    }

    /**
     * Synchronously checks memory cache.
     */
    fun getCachedArtwork(episode: Episode): Bitmap? {
        val cacheKey = episode.assetPath.ifBlank { episode.id }
        return artworkCache.get(cacheKey)
    }

    /**
     * Checks if this episode is known to have no embedded artwork.
     */
    fun hasNoEmbeddedArtwork(episode: Episode): Boolean {
        val cacheKey = episode.assetPath.ifBlank { episode.id }
        return noArtworkCache.contains(cacheKey)
    }

    /**
     * Clears both positive and negative artwork caches.
     */
    fun clearCache() {
        artworkCache.evictAll()
        noArtworkCache.clear()
    }

    /**
     * Extracts embedded artwork from an asset or storage path without blocking main thread.
     * Safely handles all errors, avoids repeated JNI calls, and releases all resources.
     */
    fun extractEmbeddedArtwork(
        context: Context,
        assetPath: String,
        targetSize: Int = TARGET_THUMBNAIL_SIZE
    ): Bitmap? {
        if (assetPath.isBlank()) return null
        val cleanPath = assetPath.trimStart('/')

        // Step 1: Pre-check if audio tag can contain artwork.
        // If an ID3v2 tag is present and has NO APIC/PIC frame, getEmbeddedPicture() will unconditionally
        // fail in native JNI and log "getEmbeddedPicture: Call to getEmbeddedPicture failed".
        // By pre-checking, we eliminate those repeated error logs while still extracting artwork when present.
        if (!mayContainEmbeddedPicture(context, cleanPath)) {
            return null
        }

        val retriever = MediaMetadataRetriever()
        var tempFile: File? = null
        var afd: AssetFileDescriptor? = null

        return try {
            val file = File(assetPath)
            if (file.exists() && file.isFile) {
                retriever.setDataSource(file.absolutePath)
            } else {
                try {
                    afd = context.assets.openFd(cleanPath)
                    retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                } catch (_: Exception) {
                    // If openFd fails (e.g. compressed asset stream), copy asset to temp file
                    val tempDir = File(context.cacheDir, "podcast_art_tmp").apply { mkdirs() }
                    val tmp = File(tempDir, "art_${cleanPath.replace('/', '_')}.tmp")
                    context.assets.open(cleanPath).use { input ->
                        FileOutputStream(tmp).use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile = tmp
                    retriever.setDataSource(tmp.absolutePath)
                }
            }

            // Step 2: Safe invocation of getEmbeddedPicture with try/catch
            val artBytes: ByteArray? = try {
                retriever.embeddedPicture
            } catch (_: Throwable) {
                null
            }

            if (artBytes != null && artBytes.isNotEmpty()) {
                decodeSampledBitmap(artBytes, targetSize)
            } else {
                null
            }
        } catch (_: Throwable) {
            // Never treat extraction failure as fatal; return null so clean default artwork is rendered
            null
        } finally {
            try {
                afd?.close()
            } catch (_: Throwable) {}
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    retriever.close()
                } else {
                    retriever.release()
                }
            } catch (_: Throwable) {}
            try {
                tempFile?.delete()
            } catch (_: Throwable) {}
        }
    }

    /**
     * Inspects the beginning of an audio stream to verify if it contains ID3v2 APIC/PIC tags.
     * If an MP3 has an ID3v2 tag that does NOT contain APIC or PIC frames, calling
     * MediaMetadataRetriever.getEmbeddedPicture() will unconditionally fail in native C++
     * and print 'MediaMetadataRetrieverJNI: getEmbeddedPicture: Call to getEmbeddedPicture failed'
     * to logcat.
     * By pre-checking the ID3v2 tag, we avoid calling getEmbeddedPicture on files known to have
     * no artwork, eliminating repeated error logs while still allowing embedded pictures to be extracted.
     */
    private fun mayContainEmbeddedPicture(context: Context, path: String): Boolean {
        if (path.isBlank()) return false
        val cleanPath = path.trimStart('/')
        var stream: InputStream? = null
        return try {
            val file = File(path)
            stream = if (file.exists() && file.isFile) {
                FileInputStream(file)
            } else {
                context.assets.open(cleanPath)
            }

            val header = ByteArray(10)
            var bytesRead = 0
            while (bytesRead < 10) {
                val r = stream.read(header, bytesRead, 10 - bytesRead)
                if (r <= 0) break
                bytesRead += r
            }
            if (bytesRead < 10) return false

            // Check if file starts with ID3v2 magic 'I', 'D', '3' (0x49, 0x44, 0x33)
            if (header[0] != 0x49.toByte() || header[1] != 0x44.toByte() || header[2] != 0x33.toByte()) {
                // Not an ID3v2 MP3 file (e.g. M4A/MP4/FLAC/OGG, or ID3v1 only).
                // In this case, allow MediaMetadataRetriever to attempt extraction safely.
                return true
            }

            val majorVersion = header[3].toInt() and 0xFF
            // Synchsafe integer (7 bits per byte): bytes 6, 7, 8, 9
            val tagSize = ((header[6].toInt() and 0x7F) shl 21) or
                    ((header[7].toInt() and 0x7F) shl 14) or
                    ((header[8].toInt() and 0x7F) shl 7) or
                    (header[9].toInt() and 0x7F)

            if (tagSize <= 0) return false

            // Scan inside the ID3v2 tag for the presence of "APIC" (v2.3/v2.4) or "PIC" (v2.2)
            val maxScan = minOf(tagSize, 4 * 1024 * 1024)
            val buffer = ByteArray(8192)
            var totalScanned = 0
            var prev1 = 0
            var prev2 = 0
            var prev3 = 0

            val apic0 = 'A'.code
            val apic1 = 'P'.code
            val apic2 = 'I'.code
            val apic3 = 'C'.code

            val pic0 = 'P'.code
            val pic1 = 'I'.code
            val pic2 = 'C'.code

            while (totalScanned < maxScan) {
                val toRead = minOf(buffer.size, maxScan - totalScanned)
                val n = stream.read(buffer, 0, toRead)
                if (n <= 0) break

                for (i in 0 until n) {
                    val b = buffer[i].toInt() and 0xFF
                    // Check for "APIC"
                    if (prev3 == apic0 && prev2 == apic1 && prev1 == apic2 && b == apic3) {
                        return true
                    }
                    // Check for "PIC" if ID3v2.2
                    if (majorVersion == 2 && prev2 == pic0 && prev1 == pic1 && b == pic2) {
                        return true
                    }
                    prev3 = prev2
                    prev2 = prev1
                    prev1 = b
                }
                totalScanned += n
            }

            // Completed scan of the ID3v2 tag and no picture frame was found.
            false
        } catch (_: Exception) {
            // In case of any I/O error during pre-check, return true to let retriever attempt safely
            true
        } finally {
            try {
                stream?.close()
            } catch (_: Exception) {}
        }
    }

    private fun decodeSampledBitmap(data: ByteArray, targetSize: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)

            options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Memory-efficient for thumbnails

            BitmapFactory.decodeByteArray(data, 0, data.size, options)
        } catch (_: Throwable) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
