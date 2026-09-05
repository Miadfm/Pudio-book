package com.miadfm.podcasts.data.vault

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages temporary decrypted media and playback caches in context.cacheDir.
 * Enforces bounded cache limits, LRU cleanup, active player protection,
 * and resilient recovery from Android OS "Clear cache" events.
 */
class TemporaryPlaybackCacheManager(private val context: Context) {

    companion object {
        private const val TAG = "TempPlaybackCache"
        private const val PLAYBACK_CACHE_DIR_NAME = "temp_playback"
        const val MAX_CACHE_BYTES = 200 * 1024 * 1024L // 200 MB maximum bounded cache
        const val STALE_FILE_MAX_AGE_MS = 10 * 60 * 1000L // 10 minutes
    }

    // Set of active temporary file absolute paths currently being read by players
    private val activePlaybackFiles = ConcurrentHashMap.newKeySet<String>()

    val cacheDir: File
        get() {
            val dir = File(context.cacheDir, PLAYBACK_CACHE_DIR_NAME)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Marks a temporary file as actively in use by a player.
     */
    fun markFileActive(file: File) {
        activePlaybackFiles.add(file.absolutePath)
        try {
            file.setLastModified(System.currentTimeMillis())
        } catch (_: Exception) {}
    }

    /**
     * Unmarks an active playback file when the player finishes or closes.
     */
    fun unmarkFileActive(file: File) {
        activePlaybackFiles.remove(file.absolutePath)
    }

    /**
     * Creates a new temporary playback file handle in the cache directory with LRU trimming.
     */
    fun createTempPlaybackFile(prefix: String, suffix: String): File {
        trimCacheIfNeeded()
        val dir = cacheDir
        return File(dir, "${prefix}_${System.currentTimeMillis()}_${(1000..9999).random()}$suffix")
    }

    /**
     * Safely deletes a specific temporary file if it is not actively being read by another player.
     */
    fun safelyDeleteTempFile(file: File?) {
        if (file == null || !file.exists()) return
        try {
            activePlaybackFiles.remove(file.absolutePath)
            file.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Could not delete temporary file ${file.name}", e)
        }
    }

    /**
     * Trims the cache size if it exceeds MAX_CACHE_BYTES, deleting the oldest inactive files first (LRU).
     */
    fun trimCacheIfNeeded(maxBytes: Long = MAX_CACHE_BYTES) {
        try {
            val dir = cacheDir
            val files = dir.listFiles() ?: return
            var totalSize = files.sumOf { it.length() }
            if (totalSize <= maxBytes) return

            // Sort files by last modified ascending (oldest first)
            val sortedFiles = files.sortedBy { it.lastModified() }
            for (file in sortedFiles) {
                if (totalSize <= maxBytes * 0.75) break // Trim down to 75% of limit
                if (!activePlaybackFiles.contains(file.absolutePath)) {
                    val len = file.length()
                    if (file.delete()) {
                        totalSize -= len
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error trimming playback cache", e)
        }
    }

    /**
     * Cleans up stale abandoned playback files (e.g. from previous app sessions or crashed players).
     */
    fun cleanStalePlaybackFiles(maxAgeMs: Long = STALE_FILE_MAX_AGE_MS) {
        try {
            val dir = cacheDir
            val now = System.currentTimeMillis()
            dir.listFiles()?.forEach { file ->
                val age = now - file.lastModified()
                if (age > maxAgeMs && !activePlaybackFiles.contains(file.absolutePath)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning stale playback files", e)
        }
    }

    /**
     * Wipes all inactive temporary playback files.
     */
    fun cleanAllInactivePlaybackFiles() {
        try {
            val dir = cacheDir
            dir.listFiles()?.forEach { file ->
                if (!activePlaybackFiles.contains(file.absolutePath)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning all playback files", e)
        }
    }
}
