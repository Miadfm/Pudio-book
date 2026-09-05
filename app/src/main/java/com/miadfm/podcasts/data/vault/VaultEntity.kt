package com.miadfm.podcasts.data.vault

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VaultContentType {
    IMAGE,
    VIDEO,
    AUDIO,
    NOTE
}

@Entity(tableName = "podcast_favorites")
data class PodcastFavoriteEntity(
    @PrimaryKey val episodeId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vault_items",
    indices = [
        androidx.room.Index("isTrashed"),
        androidx.room.Index("type"),
        androidx.room.Index("folderId"),
        androidx.room.Index("createdAt"),
        androidx.room.Index(value = ["isTrashed", "type"])
    ]
)
data class VaultItemEntity(
    @PrimaryKey val id: String,
    val type: String, // "IMAGE", "VIDEO", "AUDIO"
    val encryptedFileName: String,
    val originalDisplayName: String,
    val sizeBytes: Long,
    val folderId: String? = null,
    val isTrashed: Boolean = false,
    val trashedTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vault_folders",
    indices = [
        androidx.room.Index("createdAt")
    ]
)
data class VaultFolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vault_notes",
    indices = [
        androidx.room.Index("isTrashed"),
        androidx.room.Index("folderId"),
        androidx.room.Index("updatedAt")
    ]
)
data class VaultNoteEntity(
    @PrimaryKey val id: String,
    val encryptedTitle: ByteArray,
    val encryptedContent: ByteArray,
    val folderId: String? = null,
    val isTrashed: Boolean = false,
    val trashedTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VaultNoteEntity

        if (id != other.id) return false
        if (!encryptedTitle.contentEquals(other.encryptedTitle)) return false
        if (!encryptedContent.contentEquals(other.encryptedContent)) return false
        if (folderId != other.folderId) return false
        if (isTrashed != other.isTrashed) return false
        if (trashedTimestamp != other.trashedTimestamp) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + encryptedTitle.contentHashCode()
        result = 31 * result + encryptedContent.contentHashCode()
        result = 31 * result + (folderId?.hashCode() ?: 0)
        result = 31 * result + isTrashed.hashCode()
        result = 31 * result + (trashedTimestamp?.hashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
