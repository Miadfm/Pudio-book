package com.miadfm.podcasts.data.vault

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PodcastFavoriteEntity::class,
        VaultItemEntity::class,
        VaultFolderEntity::class,
        VaultNoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getDatabase(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "podcasts_vault_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
