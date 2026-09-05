package com.miadfm.podcasts.data.vault

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.miadfm.podcasts.data.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

data class ImportFileResult(
    val fileName: String,
    val isSuccess: Boolean,
    val errorReason: String? = null,
    val originalDeleted: Boolean = false
)

data class ImportSummary(
    val totalProcessed: Int,
    val successCount: Int,
    val failureCount: Int,
    val results: List<ImportFileResult>
)

data class UnhideFileResult(
    val itemId: String,
    val fileName: String,
    val isSuccess: Boolean,
    val errorReason: String? = null,
    val restoredUri: Uri? = null
)

data class UnhideSummary(
    val totalProcessed: Int,
    val successCount: Int,
    val failureCount: Int,
    val results: List<UnhideFileResult>
)

data class DecryptedNote(
    val id: String,
    val title: String,
    val content: String,
    val folderId: String? = null,
    val isTrashed: Boolean = false,
    val trashedTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class VaultRepository(
    private val context: Context,
    private val vaultDao: VaultDao,
    private val cryptoManager: CryptoManager
) {
    private val vaultDir: File = File(context.filesDir, "vault_files").apply { mkdirs() }
    val cacheManager: TemporaryPlaybackCacheManager = TemporaryPlaybackCacheManager(context)
    val thumbnailManager: VaultThumbnailManager = VaultThumbnailManager(context, cryptoManager, cacheManager)

    // --- Favorites ---
    val allFavorites: Flow<List<PodcastFavoriteEntity>> = vaultDao.getAllFavorites()

    suspend fun toggleFavorite(episodeId: String, isFav: Boolean) {
        withContext(Dispatchers.IO) {
            if (isFav) {
                vaultDao.insertFavorite(PodcastFavoriteEntity(episodeId = episodeId))
            } else {
                vaultDao.deleteFavorite(episodeId)
            }
        }
    }

    // --- Folders ---
    val allFolders: Flow<List<VaultFolderEntity>> = vaultDao.getAllFolders()

    suspend fun ensureDefaultFolderExists(defaultName: String): String = withContext(Dispatchers.IO) {
        val existingFirst = vaultDao.getFirstFolder()
        if (existingFirst != null) {
            vaultDao.assignOrphanedItemsToFolder(existingFirst.id)
            vaultDao.assignOrphanedNotesToFolder(existingFirst.id)
            return@withContext existingFirst.id
        }
        val id = UUID.randomUUID().toString()
        vaultDao.insertFolder(VaultFolderEntity(id = id, name = defaultName.trim()))
        vaultDao.assignOrphanedItemsToFolder(id)
        vaultDao.assignOrphanedNotesToFolder(id)
        id
    }

    suspend fun createFolder(name: String): String {
        return withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            vaultDao.insertFolder(VaultFolderEntity(id = id, name = name.trim()))
            id
        }
    }

    suspend fun deleteFolder(id: String) {
        withContext(Dispatchers.IO) {
            val allFolders = vaultDao.getAllFoldersList()
            val remainingFolder = allFolders.firstOrNull { it.id != id }
            if (remainingFolder != null) {
                vaultDao.reassignFolderItems(id, remainingFolder.id)
                vaultDao.reassignFolderNotes(id, remainingFolder.id)
            }
            vaultDao.deleteFolderById(id)
        }
    }

    suspend fun renameFolder(id: String, newName: String) {
        withContext(Dispatchers.IO) {
            vaultDao.insertFolder(VaultFolderEntity(id = id, name = newName.trim()))
        }
    }

    // --- Vault Items Flow ---
    val activeItems: Flow<List<VaultItemEntity>> = vaultDao.getActiveItems()
    val trashedItems: Flow<List<VaultItemEntity>> = vaultDao.getTrashedItems()

    // --- Multi-File Import ---
    suspend fun importFiles(
        uris: List<Uri>,
        folderId: String? = null,
        onProgress: (current: Int, total: Int, currentFileName: String) -> Unit
    ): ImportSummary = withContext(Dispatchers.IO) {
        val total = uris.size
        if (total == 0) {
            return@withContext ImportSummary(0, 0, 0, emptyList())
        }

        val effectiveFolderId = folderId ?: vaultDao.getFirstFolder()?.id ?: ensureDefaultFolderExists("My Vault")

        // Bounded concurrency (2 simultaneous file streams) for high reliability on mid-range and all Android devices
        val concurrencySemaphore = Semaphore(2)
        val progressCounter = AtomicInteger(0)

        // Process files independently while maintaining index order
        val deferredResults = coroutineScope {
            uris.mapIndexed { index, uri ->
                async(Dispatchers.IO) {
                    concurrencySemaphore.withPermit {
                        val displayName = queryDisplayName(uri) ?: "file_${System.currentTimeMillis()}_$index"
                        val count = progressCounter.incrementAndGet()
                        onProgress(count, total, displayName)

                        // 1. Check content type
                        val mimeType = queryMimeType(uri)
                        val contentType = determineSupportedType(mimeType, displayName)

                        if (contentType == null || contentType == VaultContentType.NOTE) {
                            return@withPermit ImportFileResult(
                                fileName = displayName,
                                isSuccess = false,
                                errorReason = "Unsupported file type"
                            )
                        }

                        // 2. Storage space check
                        val estimatedSize = queryFileSize(uri)
                        val availableSpace = vaultDir.usableSpace
                        if (estimatedSize > 0 && availableSpace > 0 && availableSpace < estimatedSize + (5 * 1024 * 1024L)) {
                            return@withPermit ImportFileResult(
                                fileName = displayName,
                                isSuccess = false,
                                errorReason = "Not enough storage"
                            )
                        }

                        // 3. Open source stream safely
                        val inputStream = try {
                            safelyTakeUriPermission(uri)
                            context.contentResolver.openInputStream(uri)
                        } catch (e: SecurityException) {
                            Log.e("VaultRepository", "Security error opening stream for $uri", e)
                            null
                        } catch (e: Exception) {
                            Log.e("VaultRepository", "Error opening stream for $uri", e)
                            null
                        }

                        if (inputStream == null) {
                            return@withPermit ImportFileResult(
                                fileName = displayName,
                                isSuccess = false,
                                errorReason = "Access denied"
                            )
                        }

                        val itemId = UUID.randomUUID().toString()
                        val encryptedFileName = "vault_${itemId}.enc"
                        val tempEncryptedFile = File(vaultDir, "${encryptedFileName}.tmp_${System.currentTimeMillis()}")
                        val finalEncryptedFile = File(vaultDir, encryptedFileName)

                        var isEncryptedSuccess = false
                        var errorMsg: String? = null

                        try {
                            // 4. Stream and encrypt in background chunks directly to persistent app-private storage
                            inputStream.use { input ->
                                cryptoManager.encryptStreamToFile(input, tempEncryptedFile)
                            }

                            // 5. Verification step: verify encrypted copy was written and is readable
                            if (cryptoManager.verifyVaultFileReadable(tempEncryptedFile)) {
                                if (finalEncryptedFile.exists()) finalEncryptedFile.delete()
                                if (tempEncryptedFile.renameTo(finalEncryptedFile)) {
                                    isEncryptedSuccess = true
                                } else {
                                    tempEncryptedFile.copyTo(finalEncryptedFile, overwrite = true)
                                    tempEncryptedFile.delete()
                                    isEncryptedSuccess = true
                                }
                            } else {
                                tempEncryptedFile.delete()
                                finalEncryptedFile.delete()
                                errorMsg = "Encrypted file verification failed"
                            }
                        } catch (e: SecurityException) {
                            Log.e("VaultRepository", "Security error encrypting $uri", e)
                            tempEncryptedFile.delete()
                            finalEncryptedFile.delete()
                            errorMsg = "Access denied"
                        } catch (e: IOException) {
                            Log.e("VaultRepository", "I/O error encrypting $uri", e)
                            tempEncryptedFile.delete()
                            finalEncryptedFile.delete()
                            val msg = e.message?.lowercase() ?: ""
                            errorMsg = if (msg.contains("enospc") || msg.contains("space") || msg.contains("storage")) {
                                "Not enough storage"
                            } else {
                                "Unable to read or write file"
                            }
                        } catch (e: Exception) {
                            Log.e("VaultRepository", "Error encrypting $uri", e)
                            tempEncryptedFile.delete()
                            finalEncryptedFile.delete()
                            errorMsg = "Import interrupted: ${e.localizedMessage ?: "Unknown error"}"
                        }

                        // 6. Only after successful encryption and verification, update the database
                        if (isEncryptedSuccess && finalEncryptedFile.exists()) {
                            val finalSize = finalEncryptedFile.length()
                            val itemEntity = VaultItemEntity(
                                id = itemId,
                                type = contentType.name,
                                encryptedFileName = encryptedFileName,
                                originalDisplayName = displayName,
                                sizeBytes = finalSize,
                                folderId = effectiveFolderId,
                                isTrashed = false,
                                trashedTimestamp = null,
                                createdAt = System.currentTimeMillis() + index // Ensure stable ordering
                            )

                            try {
                                vaultDao.insertItem(itemEntity)
                            } catch (e: Exception) {
                                Log.e("VaultRepository", "Database insert failed for $displayName", e)
                                finalEncryptedFile.delete()
                                return@withPermit ImportFileResult(
                                    fileName = displayName,
                                    isSuccess = false,
                                    errorReason = "Database update failed"
                                )
                            }

                            // 7. Only after successful database confirmation, attempt source cleanup
                            val originalDeleted = safelyDeleteSourceUri(uri)

                            ImportFileResult(
                                fileName = displayName,
                                isSuccess = true,
                                originalDeleted = originalDeleted
                            )
                        } else {
                            ImportFileResult(
                                fileName = displayName,
                                isSuccess = false,
                                errorReason = errorMsg ?: "Unsupported or unreadable file"
                            )
                        }
                    }
                }
            }
        }.awaitAll()

        val successCount = deferredResults.count { it.isSuccess }
        val failureCount = deferredResults.count { !it.isSuccess }

        ImportSummary(
            totalProcessed = total,
            successCount = successCount,
            failureCount = failureCount,
            results = deferredResults
        )
    }

    // --- Unhide Flow (Restore Files to User-Selected SAF Directory) ---
    suspend fun unhideItems(
        items: List<VaultItemEntity>,
        destinationTreeUri: Uri,
        deleteFromVaultOnSuccess: Boolean = true,
        onProgress: (current: Int, total: Int, fileName: String) -> Unit
    ): UnhideSummary = withContext(Dispatchers.IO) {
        val total = items.size
        if (total == 0) {
            return@withContext UnhideSummary(0, 0, 0, emptyList())
        }

        val destFolder = try {
            DocumentFile.fromTreeUri(context, destinationTreeUri)
        } catch (e: Exception) {
            Log.e("VaultRepository", "Unable to access destination tree URI $destinationTreeUri", e)
            null
        }

        if (destFolder == null || !destFolder.exists() || !destFolder.isDirectory || !destFolder.canWrite()) {
            return@withContext UnhideSummary(
                totalProcessed = total,
                successCount = 0,
                failureCount = total,
                results = items.map {
                    UnhideFileResult(
                        itemId = it.id,
                        fileName = it.originalDisplayName,
                        isSuccess = false,
                        errorReason = if (destFolder == null || !destFolder.canWrite()) "Access denied" else "Destination unavailable"
                    )
                }
            )
        }

        val semaphore = Semaphore(2) // Bounded concurrency for optimal performance on Samsung A32 / mid-range devices
        val processedCount = AtomicInteger(0)
        val itemsToDeleteFromVault = ArrayList<VaultItemEntity>()

        val deferredResults = coroutineScope {
            items.map { item ->
                async {
                    semaphore.withPermit {
                        val currentCount = processedCount.incrementAndGet()
                        onProgress(currentCount, total, item.originalDisplayName)

                        val encryptedFile = File(vaultDir, item.encryptedFileName)
                        if (!encryptedFile.exists() || encryptedFile.length() <= 12) {
                            return@withPermit Pair(
                                UnhideFileResult(
                                    itemId = item.id,
                                    fileName = item.originalDisplayName,
                                    isSuccess = false,
                                    errorReason = "Unable to restore selected content"
                                ),
                                null
                            )
                        }

                        var createdDoc: DocumentFile? = null
                        var isSuccess = false
                        var errorReason: String? = null

                        try {
                            val mimeType = resolveMimeType(item.originalDisplayName, item.type)
                            val targetFileName = generateNonConflictingName(destFolder, item.originalDisplayName)

                            createdDoc = destFolder.createFile(mimeType, targetFileName)
                            if (createdDoc == null) {
                                errorReason = "Destination unavailable"
                            } else {
                                val outputStream = context.contentResolver.openOutputStream(createdDoc.uri, "w")
                                if (outputStream == null) {
                                    errorReason = "Access denied"
                                    createdDoc.delete()
                                    createdDoc = null
                                } else {
                                    outputStream.use { out ->
                                        cryptoManager.decryptFileToStream(encryptedFile, out)
                                    }

                                    // Verification step: verify destination document exists and is valid readable size
                                    val restoredLength = createdDoc.length()
                                    if (createdDoc.exists() && restoredLength > 0) {
                                        isSuccess = true
                                    } else {
                                        errorReason = "Restore interrupted"
                                        createdDoc.delete()
                                        createdDoc = null
                                    }
                                }
                            }
                        } catch (e: SecurityException) {
                            Log.e("VaultRepository", "Security error unhiding ${item.originalDisplayName}", e)
                            errorReason = "Access denied"
                            createdDoc?.delete()
                        } catch (e: IOException) {
                            Log.e("VaultRepository", "I/O error unhiding ${item.originalDisplayName}", e)
                            val msg = e.message?.lowercase() ?: ""
                            errorReason = if (msg.contains("enospc") || msg.contains("space") || msg.contains("storage")) {
                                "Not enough storage"
                            } else {
                                "Unable to write file"
                            }
                            createdDoc?.delete()
                        } catch (e: Exception) {
                            Log.e("VaultRepository", "Error unhiding ${item.originalDisplayName}", e)
                            errorReason = "Restore interrupted"
                            createdDoc?.delete()
                        }

                        if (isSuccess && createdDoc != null) {
                            Pair(
                                UnhideFileResult(
                                    itemId = item.id,
                                    fileName = item.originalDisplayName,
                                    isSuccess = true,
                                    restoredUri = createdDoc.uri
                                ),
                                item
                            )
                        } else {
                            Pair(
                                UnhideFileResult(
                                    itemId = item.id,
                                    fileName = item.originalDisplayName,
                                    isSuccess = false,
                                    errorReason = errorReason ?: "Unable to restore selected content"
                                ),
                                null
                            )
                        }
                    }
                }
            }
        }.awaitAll()

        val results = ArrayList<UnhideFileResult>(total)
        for (pair in deferredResults) {
            results.add(pair.first)
            if (pair.second != null && deleteFromVaultOnSuccess) {
                itemsToDeleteFromVault.add(pair.second!!)
            }
        }

        // Only after verified successful restore: delete encrypted vault file and Room DB record
        if (itemsToDeleteFromVault.isNotEmpty()) {
            for (item in itemsToDeleteFromVault) {
                val file = File(vaultDir, item.encryptedFileName)
                if (file.exists()) {
                    file.delete()
                }
                vaultDao.deleteItemById(item.id)
            }
        }

        val successCount = results.count { it.isSuccess }
        val failureCount = results.count { !it.isSuccess }

        UnhideSummary(
            totalProcessed = total,
            successCount = successCount,
            failureCount = failureCount,
            results = results
        )
    }

    private fun generateNonConflictingName(destFolder: DocumentFile, originalDisplayName: String): String {
        val existingNames = try {
            destFolder.listFiles().mapNotNull { it.name }.toSet()
        } catch (e: Exception) {
            emptySet()
        }

        if (!existingNames.contains(originalDisplayName)) {
            return originalDisplayName
        }

        val baseName = originalDisplayName.substringBeforeLast('.', originalDisplayName)
        val extension = if (originalDisplayName.contains('.')) ".${originalDisplayName.substringAfterLast('.')}" else ""

        var counter = 1
        while (counter < 1000) {
            val candidate = "$baseName ($counter)$extension"
            if (!existingNames.contains(candidate)) {
                return candidate
            }
            counter++
        }
        return "${baseName}_${System.currentTimeMillis()}$extension"
    }

    // --- Notes Management ---
    val activeNotes: Flow<List<VaultNoteEntity>> = vaultDao.getActiveNotes()
    val trashedNotes: Flow<List<VaultNoteEntity>> = vaultDao.getTrashedNotes()

    suspend fun saveNote(title: String, content: String, noteId: String? = null, folderId: String? = null): String {
        return withContext(Dispatchers.IO) {
            val id = noteId ?: UUID.randomUUID().toString()
            val encTitle = cryptoManager.encryptBytes(title.toByteArray(Charsets.UTF_8))
            val encContent = cryptoManager.encryptBytes(content.toByteArray(Charsets.UTF_8))

            val existing = vaultDao.getNoteById(id)
            val noteEntity = VaultNoteEntity(
                id = id,
                encryptedTitle = encTitle,
                encryptedContent = encContent,
                folderId = folderId ?: existing?.folderId,
                isTrashed = false,
                trashedTimestamp = null,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            vaultDao.insertNote(noteEntity)
            id
        }
    }

    suspend fun getDecryptedNote(id: String): DecryptedNote? = withContext(Dispatchers.IO) {
        val entity = vaultDao.getNoteById(id) ?: return@withContext null
        try {
            val title = String(cryptoManager.decryptBytes(entity.encryptedTitle), Charsets.UTF_8)
            val content = String(cryptoManager.decryptBytes(entity.encryptedContent), Charsets.UTF_8)
            DecryptedNote(
                id = entity.id,
                title = title,
                content = content,
                folderId = entity.folderId,
                isTrashed = entity.isTrashed,
                trashedTimestamp = entity.trashedTimestamp,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        } catch (e: Exception) {
            Log.e("VaultRepository", "Error decrypting note $id", e)
            null
        }
    }

    suspend fun decryptAllNotes(entities: List<VaultNoteEntity>): List<DecryptedNote> = withContext(Dispatchers.IO) {
        entities.mapNotNull { entity ->
            try {
                val title = String(cryptoManager.decryptBytes(entity.encryptedTitle), Charsets.UTF_8)
                val content = String(cryptoManager.decryptBytes(entity.encryptedContent), Charsets.UTF_8)
                DecryptedNote(
                    id = entity.id,
                    title = title,
                    content = content,
                    folderId = entity.folderId,
                    isTrashed = entity.isTrashed,
                    trashedTimestamp = entity.trashedTimestamp,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            } catch (e: Exception) {
                Log.e("VaultRepository", "Failed to decrypt note ${entity.id}", e)
                null
            }
        }
    }

    // --- Image & Thumbnail Decryption ---
    suspend fun decryptImageBitmap(item: VaultItemEntity, maxDimension: Int = 2048): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(vaultDir, item.encryptedFileName)
        if (!file.exists()) return@withContext null
        cryptoManager.decryptImageBitmap(file, maxDimension)
    }

    suspend fun decryptThumbnail(item: VaultItemEntity, targetSize: Int = 256): Bitmap? = withContext(Dispatchers.IO) {
        thumbnailManager.getThumbnail(item, vaultDir)
    }

    suspend fun getThumbnail(item: VaultItemEntity): Bitmap? = withContext(Dispatchers.IO) {
        thumbnailManager.getThumbnail(item, vaultDir)
    }

    // --- Media Playback Preparation (Internal Streaming and External Sharing) ---

    /**
     * Creates a high-performance DataSource.Factory for streaming encrypted media directly into ExoPlayer.
     * Starts playback in milliseconds without decrypting whole files to disk.
     */
    fun getMediaDataSourceFactory(item: VaultItemEntity): androidx.media3.datasource.DataSource.Factory {
        val encryptedFile = File(vaultDir, item.encryptedFileName)
        return EncryptedVaultDataSource.Factory(encryptedFile, cryptoManager, cacheManager)
    }

    suspend fun prepareTemporaryMediaFile(item: VaultItemEntity): File? = withContext(Dispatchers.IO) {
        val encryptedFile = File(vaultDir, item.encryptedFileName)
        if (!encryptedFile.exists()) return@withContext null

        cacheManager.cleanStalePlaybackFiles(maxAgeMs = 10 * 60 * 1000L)

        try {
            val extension = getFileExtension(item.originalDisplayName, item.type)
            val baseName = item.originalDisplayName
                .substringBeforeLast('.')
                .replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                .take(32)
            val tempFile = cacheManager.createTempPlaybackFile("play_${item.id}_${baseName}", extension)
            cacheManager.markFileActive(tempFile)
            FileOutputStream(tempFile).use { fos ->
                cryptoManager.decryptFileToStream(encryptedFile, fos)
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                tempFile
            } else {
                cacheManager.safelyDeleteTempFile(tempFile)
                null
            }
        } catch (e: Exception) {
            Log.e("VaultRepository", "Failed to decrypt temporary media for player ${item.id}", e)
            null
        }
    }

    fun deleteTemporaryFile(file: File?) {
        cacheManager.safelyDeleteTempFile(file)
    }

    // --- External Playback Sharing (Open With...) ---
    suspend fun prepareTemporaryMediaUri(item: VaultItemEntity): Pair<Uri, String>? = withContext(Dispatchers.IO) {
        val tempFile = prepareTemporaryMediaFile(item) ?: return@withContext null
        try {
            val mimeType = resolveMimeType(item.originalDisplayName, item.type)
            val uri = FileProvider.getUriForFile(
                context,
                "com.miadfm.podcasts.fileprovider",
                tempFile
            )
            Pair(uri, mimeType)
        } catch (e: Exception) {
            Log.e("VaultRepository", "Failed to prepare temp media URI for ${item.id}", e)
            cacheManager.safelyDeleteTempFile(tempFile)
            null
        }
    }

    private fun getFileExtension(displayName: String, type: String): String {
        val lastDot = displayName.lastIndexOf('.')
        if (lastDot > 0 && lastDot < displayName.length - 1) {
            return displayName.substring(lastDot)
        }
        return when (type) {
            VaultContentType.AUDIO.name -> ".mp3"
            VaultContentType.VIDEO.name -> ".mp4"
            VaultContentType.IMAGE.name -> ".jpg"
            else -> ".bin"
        }
    }

    fun resolveMimeType(displayName: String, type: String): String {
        val ext = displayName.substringAfterLast('.', "").lowercase()
        val mimeFromMap = if (ext.isNotEmpty()) {
            android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        } else null

        return mimeFromMap ?: when (ext) {
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg", "oga" -> "audio/ogg"
            "flac" -> "audio/flac"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "3gp" -> "video/3gpp"
            "mov" -> "video/quicktime"
            else -> when (type) {
                VaultContentType.AUDIO.name -> "audio/*"
                VaultContentType.VIDEO.name -> "video/*"
                VaultContentType.IMAGE.name -> "image/*"
                else -> "*/*"
            }
        }
    }

    fun cleanStaleTemporaryPlaybackFiles(maxAgeMs: Long = 10 * 60 * 1000L) {
        cacheManager.cleanStalePlaybackFiles(maxAgeMs)
    }

    fun cleanTemporaryPlaybackFiles() {
        cacheManager.cleanAllInactivePlaybackFiles()
    }

    /**
     * Clears disposable temporary playback and thumbnail cache files.
     * Guaranteed to NEVER touch original permanent Vault files, database, PIN, or user data.
     */
    suspend fun clearTemporaryCache() = withContext(Dispatchers.IO) {
        cacheManager.cleanAllInactivePlaybackFiles()
        thumbnailManager.clearCache()
        // Also clean any leftover temporary export or unhide files in cacheDir
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("unhide_") || file.name.startsWith("temp_") || file.name.startsWith("export_")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    // --- Trash & Deletion ---
    suspend fun moveItemToTrash(item: VaultItemEntity) = withContext(Dispatchers.IO) {
        vaultDao.updateItem(item.copy(isTrashed = true, trashedTimestamp = System.currentTimeMillis()))
    }

    suspend fun restoreItem(item: VaultItemEntity) = withContext(Dispatchers.IO) {
        vaultDao.updateItem(item.copy(isTrashed = false, trashedTimestamp = null))
    }

    suspend fun deleteItemPermanently(item: VaultItemEntity) = withContext(Dispatchers.IO) {
        thumbnailManager.evictThumbnail(item.id)
        val file = File(vaultDir, item.encryptedFileName)
        if (file.exists()) file.delete()
        vaultDao.deleteItemById(item.id)
    }

    suspend fun moveNoteToTrash(noteId: String) = withContext(Dispatchers.IO) {
        val note = vaultDao.getNoteById(noteId) ?: return@withContext
        vaultDao.updateNote(note.copy(isTrashed = true, trashedTimestamp = System.currentTimeMillis()))
    }

    suspend fun restoreNote(noteId: String) = withContext(Dispatchers.IO) {
        val note = vaultDao.getNoteById(noteId) ?: return@withContext
        vaultDao.updateNote(note.copy(isTrashed = false, trashedTimestamp = null))
    }

    suspend fun deleteNotePermanently(noteId: String) = withContext(Dispatchers.IO) {
        vaultDao.deleteNoteById(noteId)
    }

    suspend fun setItemFolder(itemId: String, folderId: String?) = withContext(Dispatchers.IO) {
        val item = vaultDao.getItemById(itemId) ?: return@withContext
        vaultDao.updateItem(item.copy(folderId = folderId))
    }

    suspend fun setNoteFolder(noteId: String, folderId: String?) = withContext(Dispatchers.IO) {
        val note = vaultDao.getNoteById(noteId) ?: return@withContext
        vaultDao.updateNote(note.copy(folderId = folderId))
    }

    // --- Automatic 30-Day Trash Cleanup ---
    suspend fun cleanupExpiredTrash() = withContext(Dispatchers.IO) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)

        val expiredItems = vaultDao.getExpiredTrashedItems(thirtyDaysAgo)
        for (item in expiredItems) {
            val file = File(vaultDir, item.encryptedFileName)
            if (file.exists()) file.delete()
            vaultDao.deleteItemById(item.id)
        }

        val expiredNotes = vaultDao.getExpiredTrashedNotes(thirtyDaysAgo)
        for (note in expiredNotes) {
            vaultDao.deleteNoteById(note.id)
        }
    }

    // --- Helper Utilities ---
    private fun queryDisplayName(uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) name = cursor.getString(index)
                }
            }
        } catch (e: Exception) {
            Log.e("VaultRepository", "Error querying display name", e)
        }
        return name ?: uri.lastPathSegment
    }

    private fun queryFileSize(uri: Uri): Long {
        var size: Long = 0
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index != -1) size = cursor.getLong(index)
                }
            }
        } catch (e: Exception) {
            Log.e("VaultRepository", "Error querying file size", e)
        }
        return size
    }

    private fun queryMimeType(uri: Uri): String? {
        return try {
            context.contentResolver.getType(uri)
        } catch (e: Exception) {
            null
        }
    }

    private fun safelyTakeUriPermission(uri: Uri) {
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {
            // Expected for Photo Picker or providers that don't support persistable permissions
        }
    }

    private fun safelyDeleteSourceUri(uri: Uri): Boolean {
        return try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } else {
                val rows = context.contentResolver.delete(uri, null, null)
                rows > 0
            }
        } catch (e: Exception) {
            Log.w("VaultRepository", "Could not delete original source URI: $uri", e)
            false
        }
    }

    private fun determineSupportedType(mimeType: String?, fileName: String): VaultContentType? {
        val lowerMime = mimeType?.lowercase() ?: ""
        val lowerName = fileName.lowercase()

        // 1. Check MIME type first
        if (lowerMime.startsWith("image/")) return VaultContentType.IMAGE
        if (lowerMime.startsWith("video/")) return VaultContentType.VIDEO
        if (lowerMime.startsWith("audio/") || lowerMime == "application/ogg" || lowerMime == "application/x-flac") return VaultContentType.AUDIO

        // 2. Check file extensions
        val ext = lowerName.substringAfterLast('.', "")
        val imageExts = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "svg", "ico", "tiff", "tif", "avif", "raw", "dng")
        val videoExts = setOf("mp4", "mkv", "webm", "3gp", "mov", "avi", "m4v", "flv", "ts", "wmv", "mpg", "mpeg", "ogv", "m2ts")
        val audioExts = setOf("mp3", "m4a", "wav", "aac", "ogg", "flac", "opus", "wma", "mid", "midi", "amr", "m4b", "oga", "aiff")

        if (imageExts.contains(ext)) return VaultContentType.IMAGE
        if (videoExts.contains(ext)) return VaultContentType.VIDEO
        if (audioExts.contains(ext)) return VaultContentType.AUDIO

        return null
    }

    fun getEncryptedFile(item: VaultItemEntity): File {
        return File(vaultDir, item.encryptedFileName)
    }
}
