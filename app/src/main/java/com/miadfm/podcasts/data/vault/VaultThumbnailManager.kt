package com.miadfm.podcasts.data.vault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import android.util.LruCache
import com.miadfm.podcasts.data.security.ChunkedEncryption
import com.miadfm.podcasts.data.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Intelligent in-memory and on-disk thumbnail cache for Vault Images and Videos.
 * All thumbnails are cached exclusively inside context.cacheDir and are safely disposable.
 * If Android clears the cache, thumbnails are seamlessly regenerated on-demand without
 * affecting permanent Vault originals.
 */
class VaultThumbnailManager(
    private val context: Context,
    private val cryptoManager: CryptoManager,
    private val tempPlaybackCacheManager: TemporaryPlaybackCacheManager
) {

    companion object {
        private const val TAG = "VaultThumbnailManager"
        private const val THUMBNAIL_DIR_NAME = "vault_thumbnails"
        private const val MAX_DISK_CACHE_BYTES = 50 * 1024 * 1024L // 50 MB bounded cache
        private const val TARGET_THUMBNAIL_SIZE = 280 // Standard thumbnail dimension (pixels)
    }

    // In-memory LRU cache scaled to device memory (up to 24 MB)
    private val memoryCache: LruCache<String, Bitmap>

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = min(maxMemory / 8, 24 * 1024) // 24 MB or 1/8th of available RAM
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    private val thumbnailDir: File
        get() {
            val dir = File(context.cacheDir, THUMBNAIL_DIR_NAME)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Gets or asynchronously generates a lightweight thumbnail for a Vault item.
     * Returns immediately from memory or disk cache when available.
     */
    suspend fun getThumbnail(item: VaultItemEntity, vaultDir: File): Bitmap? = withContext(Dispatchers.IO) {
        // 1. Check memory cache (fastest, microsecond access)
        memoryCache.get(item.id)?.let { return@withContext it }

        // 2. Check disk cache
        val diskThumbFile = File(thumbnailDir, "${item.id}.thumb")
        if (diskThumbFile.exists() && diskThumbFile.length() > 0) {
            try {
                val bitmap = BitmapFactory.decodeFile(diskThumbFile.absolutePath)
                if (bitmap != null) {
                    memoryCache.put(item.id, bitmap)
                    return@withContext bitmap
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading cached thumbnail for ${item.id}", e)
                diskThumbFile.delete()
            }
        }

        // 3. Generate thumbnail asynchronously from encrypted original
        val encryptedFile = File(vaultDir, item.encryptedFileName)
        if (!encryptedFile.exists()) return@withContext null

        val generatedBitmap: Bitmap? = when (item.type) {
            VaultContentType.IMAGE.name -> {
                cryptoManager.decryptImageThumbnail(encryptedFile, TARGET_THUMBNAIL_SIZE)
            }
            VaultContentType.VIDEO.name -> {
                generateVideoThumbnail(encryptedFile, item)
            }
            VaultContentType.AUDIO.name -> {
                generateAudioArtworkThumbnail(encryptedFile, item)
            }
            else -> null
        }

        if (generatedBitmap != null) {
            // Save to memory cache
            memoryCache.put(item.id, generatedBitmap)

            // Save to disk cache with LRU trim check
            saveThumbnailToDisk(diskThumbFile, generatedBitmap)
        }

        generatedBitmap
    }

    /**
     * Efficiently generates a representative video thumbnail frame without full playback.
     */
    private fun generateVideoThumbnail(encryptedFile: File, item: VaultItemEntity): Bitmap? {
        var tempFile: File? = null
        val retriever = MediaMetadataRetriever()
        return try {
            if (ChunkedEncryption.isChunkedFile(encryptedFile)) {
                // For chunked files, decrypt sufficient chunks to a temp file for fast metadata retrieval
                val header = ChunkedEncryption.readHeader(encryptedFile)
                if (header != null && header.totalChunks > 0) {
                    tempFile = tempPlaybackCacheManager.createTempPlaybackFile("thumb_vid", ".tmp")
                    FileOutputStream(tempFile).use { fos ->
                        val chunksToRead = min(30, header.totalChunks)
                        for (i in 0 until chunksToRead) {
                            val chunk = ChunkedEncryption.decryptChunk(encryptedFile, i, cryptoManager.getSecretKey(), header)
                            if (chunk != null) {
                                fos.write(chunk)
                            }
                        }
                    }
                    retriever.setDataSource(tempFile.absolutePath)
                    val frame = retriever.getFrameAtTime(1000000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                    frame?.let { downsampleBitmap(it, TARGET_THUMBNAIL_SIZE) }
                } else {
                    null
                }
            } else {
                // Legacy encrypted video: decrypt to bounded temporary file
                tempFile = tempPlaybackCacheManager.createTempPlaybackFile("thumb_vid_leg", ".tmp")
                FileOutputStream(tempFile).use { fos ->
                    cryptoManager.decryptFileToStream(encryptedFile, fos)
                }
                retriever.setDataSource(tempFile.absolutePath)
                val frame = retriever.getFrameAtTime(1000000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
                frame?.let { downsampleBitmap(it, TARGET_THUMBNAIL_SIZE) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to generate video thumbnail for ${item.id}", e)
            null
        } finally {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    retriever.close()
                } else {
                    retriever.release()
                }
            } catch (_: Exception) {}
            tempFile?.let { tempPlaybackCacheManager.safelyDeleteTempFile(it) }
        }
    }

    /**
     * Efficiently extracts embedded audio artwork from ID3 metadata tags without decoding the whole file.
     */
    private fun generateAudioArtworkThumbnail(encryptedFile: File, item: VaultItemEntity): Bitmap? {
        var tempFile: File? = null
        val retriever = MediaMetadataRetriever()
        return try {
            if (ChunkedEncryption.isChunkedFile(encryptedFile)) {
                val header = ChunkedEncryption.readHeader(encryptedFile)
                if (header != null && header.totalChunks > 0) {
                    tempFile = tempPlaybackCacheManager.createTempPlaybackFile("thumb_aud", ".tmp")
                    FileOutputStream(tempFile).use { fos ->
                        // ID3 tags and embedded artwork are located in the initial chunks
                        val chunksToRead = min(4, header.totalChunks)
                        for (i in 0 until chunksToRead) {
                            val chunk = ChunkedEncryption.decryptChunk(encryptedFile, i, cryptoManager.getSecretKey(), header)
                            if (chunk != null) {
                                fos.write(chunk)
                            }
                        }
                    }
                    retriever.setDataSource(tempFile.absolutePath)
                    val artBytes = retriever.embeddedPicture
                    if (artBytes != null && artBytes.isNotEmpty()) {
                        decodeSampledBitmap(artBytes, TARGET_THUMBNAIL_SIZE)
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                tempFile = tempPlaybackCacheManager.createTempPlaybackFile("thumb_aud_leg", ".tmp")
                FileOutputStream(tempFile).use { fos ->
                    cryptoManager.decryptFileToStream(encryptedFile, fos)
                }
                retriever.setDataSource(tempFile.absolutePath)
                val artBytes = retriever.embeddedPicture
                if (artBytes != null && artBytes.isNotEmpty()) {
                    decodeSampledBitmap(artBytes, TARGET_THUMBNAIL_SIZE)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to extract audio artwork for ${item.id}", e)
            null
        } finally {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    retriever.close()
                } else {
                    retriever.release()
                }
            } catch (_: Exception) {}
            tempFile?.let { tempPlaybackCacheManager.safelyDeleteTempFile(it) }
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
            val decoded = BitmapFactory.decodeByteArray(data, 0, data.size, options)
            decoded?.let { downsampleBitmap(it, targetSize) }
        } catch (e: Exception) {
            Log.w(TAG, "Error decoding sampled artwork", e)
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

    private fun downsampleBitmap(source: Bitmap, targetSize: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= targetSize && height <= targetSize) return source

        val ratio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (width > height) {
            targetSize to max(1, (targetSize / ratio).toInt())
        } else {
            max(1, (targetSize * ratio).toInt()) to targetSize
        }

        return try {
            val scaled = Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
            if (scaled != source && !source.isRecycled) {
                source.recycle()
            }
            scaled
        } catch (_: Exception) {
            source
        }
    }

    private fun saveThumbnailToDisk(thumbFile: File, bitmap: Bitmap) {
        try {
            trimDiskCacheIfNeeded()
            FileOutputStream(thumbFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error caching thumbnail to disk", e)
        }
    }

    private fun trimDiskCacheIfNeeded(maxBytes: Long = MAX_DISK_CACHE_BYTES) {
        try {
            val dir = thumbnailDir
            val files = dir.listFiles() ?: return
            var totalSize = files.sumOf { it.length() }
            if (totalSize <= maxBytes) return

            val sortedFiles = files.sortedBy { it.lastModified() }
            for (file in sortedFiles) {
                if (totalSize <= maxBytes * 0.75) break
                val len = file.length()
                if (file.delete()) {
                    totalSize -= len
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error trimming disk thumbnail cache", e)
        }
    }

    /**
     * Clears an item's thumbnail from both memory and disk (e.g. when deleted or modified).
     */
    fun evictThumbnail(itemId: String) {
        memoryCache.remove(itemId)
        try {
            val file = File(thumbnailDir, "$itemId.thumb")
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }

    /**
     * Wipes all cached thumbnails.
     * Never affects original encrypted files in Vault storage.
     */
    fun clearCache() {
        memoryCache.evictAll()
        try {
            thumbnailDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing thumbnail cache", e)
        }
    }
}
