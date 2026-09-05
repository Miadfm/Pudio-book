package com.miadfm.podcasts.data.vault

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    // --- Podcast Favorites ---
    @Query("SELECT * FROM podcast_favorites")
    fun getAllFavorites(): Flow<List<PodcastFavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: PodcastFavoriteEntity)

    @Query("DELETE FROM podcast_favorites WHERE episodeId = :episodeId")
    suspend fun deleteFavorite(episodeId: String)

    // --- Vault Folders ---
    @Query("SELECT * FROM vault_folders ORDER BY createdAt ASC")
    fun getAllFolders(): Flow<List<VaultFolderEntity>>

    @Query("SELECT * FROM vault_folders ORDER BY createdAt ASC")
    suspend fun getAllFoldersList(): List<VaultFolderEntity>

    @Query("SELECT COUNT(*) FROM vault_folders")
    suspend fun getFolderCount(): Int

    @Query("SELECT * FROM vault_folders ORDER BY createdAt ASC LIMIT 1")
    suspend fun getFirstFolder(): VaultFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: VaultFolderEntity)

    @Query("DELETE FROM vault_folders WHERE id = :id")
    suspend fun deleteFolderById(id: String)

    @Query("UPDATE vault_items SET folderId = :folderId WHERE folderId IS NULL OR folderId = ''")
    suspend fun assignOrphanedItemsToFolder(folderId: String)

    @Query("UPDATE vault_notes SET folderId = :folderId WHERE folderId IS NULL OR folderId = ''")
    suspend fun assignOrphanedNotesToFolder(folderId: String)

    @Query("UPDATE vault_items SET folderId = :newFolderId WHERE folderId = :oldFolderId")
    suspend fun reassignFolderItems(oldFolderId: String, newFolderId: String)

    @Query("UPDATE vault_notes SET folderId = :newFolderId WHERE folderId = :oldFolderId")
    suspend fun reassignFolderNotes(oldFolderId: String, newFolderId: String)

    // --- Vault Items ---
    @Query("SELECT * FROM vault_items WHERE isTrashed = 0 ORDER BY createdAt DESC")
    fun getActiveItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE isTrashed = 0 AND type = :type ORDER BY createdAt DESC")
    fun getActiveItemsByType(type: String): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE isTrashed = 0 AND folderId = :folderId ORDER BY createdAt DESC")
    fun getActiveItemsByFolder(folderId: String): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE isTrashed = 1 ORDER BY trashedTimestamp DESC")
    fun getTrashedItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE isTrashed = 1 AND trashedTimestamp < :thresholdTimestamp")
    suspend fun getExpiredTrashedItems(thresholdTimestamp: Long): List<VaultItemEntity>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): VaultItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<VaultItemEntity>)

    @Update
    suspend fun updateItem(item: VaultItemEntity)

    @Update
    suspend fun updateItems(items: List<VaultItemEntity>)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("DELETE FROM vault_items WHERE id IN (:ids)")
    suspend fun deleteItemsByIds(ids: List<String>)

    // --- Vault Notes ---
    @Query("SELECT * FROM vault_notes WHERE isTrashed = 0 ORDER BY updatedAt DESC")
    fun getActiveNotes(): Flow<List<VaultNoteEntity>>

    @Query("SELECT * FROM vault_notes WHERE isTrashed = 0 AND folderId = :folderId ORDER BY updatedAt DESC")
    fun getActiveNotesByFolder(folderId: String): Flow<List<VaultNoteEntity>>

    @Query("SELECT * FROM vault_notes WHERE isTrashed = 1 ORDER BY trashedTimestamp DESC")
    fun getTrashedNotes(): Flow<List<VaultNoteEntity>>

    @Query("SELECT * FROM vault_notes WHERE isTrashed = 1 AND trashedTimestamp < :thresholdTimestamp")
    suspend fun getExpiredTrashedNotes(thresholdTimestamp: Long): List<VaultNoteEntity>

    @Query("SELECT * FROM vault_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): VaultNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: VaultNoteEntity)

    @Update
    suspend fun updateNote(note: VaultNoteEntity)

    @Query("DELETE FROM vault_notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)
}
