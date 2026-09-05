package com.miadfm.podcasts

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.miadfm.podcasts.data.security.CryptoManager
import com.miadfm.podcasts.data.vault.VaultDatabase
import com.miadfm.podcasts.data.vault.VaultRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VaultRepositoryRobolectricTest {

    private lateinit var database: VaultDatabase
    private lateinit var cryptoManager: CryptoManager
    private lateinit var repository: VaultRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, VaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cryptoManager = CryptoManager(context)
        repository = VaultRepository(context, database.vaultDao(), cryptoManager)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `toggle favorite persistence in Room database`() = runBlocking {
        var favorites = repository.allFavorites.first()
        assertTrue(favorites.isEmpty())

        repository.toggleFavorite("ep_1", true)
        favorites = repository.allFavorites.first()
        assertEquals(1, favorites.size)
        assertEquals("ep_1", favorites[0].episodeId)

        repository.toggleFavorite("ep_1", false)
        favorites = repository.allFavorites.first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun `save, retrieve, and decrypt note with AES-256-GCM`() = runBlocking {
        val noteId = repository.saveNote(
            title = "Encrypted Confidential Note",
            content = "This content is protected by AES-256-GCM authenticated encryption."
        )

        val decryptedNote = repository.getDecryptedNote(noteId)
        assertNotNull(decryptedNote)
        assertEquals("Encrypted Confidential Note", decryptedNote?.title)
        assertEquals("This content is protected by AES-256-GCM authenticated encryption.", decryptedNote?.content)
        assertFalse(decryptedNote!!.isTrashed)
    }

    @Test
    fun `trash note and restore note`() = runBlocking {
        val noteId = repository.saveNote("Trash Test", "Note body")
        repository.moveNoteToTrash(noteId)

        val trashed = repository.trashedNotes.first()
        assertEquals(1, trashed.size)
        assertTrue(trashed[0].isTrashed)

        val active = repository.activeNotes.first()
        assertTrue(active.isEmpty())

        repository.restoreNote(noteId)
        val trashedAfter = repository.trashedNotes.first()
        assertTrue(trashedAfter.isEmpty())

        val activeAfter = repository.activeNotes.first()
        assertEquals(1, activeAfter.size)
    }

    @Test
    fun `create and delete folders`() = runBlocking {
        val folderId = repository.createFolder("Personal Documents")
        val folders = repository.allFolders.first()
        assertEquals(1, folders.size)
        assertEquals("Personal Documents", folders[0].name)

        repository.deleteFolder(folderId)
        val foldersAfter = repository.allFolders.first()
        assertTrue(foldersAfter.isEmpty())
    }

    @Test
    fun `import files with valid images and verify Room persistence and chunked encryption`() = runBlocking {
        // Create sample temp image file
        val tempImage = java.io.File(context.cacheDir, "sample_photo.jpg").apply {
            writeBytes("JPEG_TEST_IMAGE_DATA_12345678".toByteArray())
        }
        val uri = android.net.Uri.fromFile(tempImage)

        val progressUpdates = mutableListOf<String>()
        val summary = repository.importFiles(listOf(uri)) { current, total, name ->
            progressUpdates.add("$current/$total: $name")
        }

        assertEquals(1, summary.totalProcessed)
        assertEquals(1, summary.successCount)
        assertEquals(0, summary.failureCount)
        assertTrue(summary.results[0].isSuccess)
        assertEquals("sample_photo.jpg", summary.results[0].fileName)

        val activeItems = database.vaultDao().getActiveItems().first()
        assertEquals(1, activeItems.size)
        assertEquals("IMAGE", activeItems[0].type)
        assertEquals("sample_photo.jpg", activeItems[0].originalDisplayName)

        // Verify encrypted file exists in vault
        val vaultFile = java.io.File(context.filesDir, "vault_files/${activeItems[0].encryptedFileName}")
        assertTrue(vaultFile.exists())
        assertTrue(cryptoManager.verifyVaultFileReadable(vaultFile))
    }

    @Test
    fun `import multiple files handles unsupported formats gracefully without failing the entire batch`() = runBlocking {
        val validAudio = java.io.File(context.cacheDir, "recording.mp3").apply {
            writeBytes("MP3_AUDIO_HEADER_DATA_87654321".toByteArray())
        }
        val unsupportedDoc = java.io.File(context.cacheDir, "report.pdf").apply {
            writeBytes("PDF_DOCUMENT_CONTENT".toByteArray())
        }

        val summary = repository.importFiles(
            listOf(android.net.Uri.fromFile(validAudio), android.net.Uri.fromFile(unsupportedDoc))
        ) { _, _, _ -> }

        assertEquals(2, summary.totalProcessed)
        assertEquals(1, summary.successCount)
        assertEquals(1, summary.failureCount)

        val successItem = summary.results.find { it.fileName == "recording.mp3" }
        val failedItem = summary.results.find { it.fileName == "report.pdf" }

        assertNotNull(successItem)
        assertTrue(successItem!!.isSuccess)

        assertNotNull(failedItem)
        assertFalse(failedItem!!.isSuccess)
        assertEquals("Unsupported file type", failedItem.errorReason)

        val activeItems = database.vaultDao().getActiveItems().first()
        assertEquals(1, activeItems.size)
        assertEquals("AUDIO", activeItems[0].type)
    }

    @Test
    fun `clear temporary cache does not delete permanent vault files`() = runBlocking {
        val tempImage = java.io.File(context.cacheDir, "vault_test_img.png").apply {
            writeBytes("PNG_HEADER_DATA_IMAGE".toByteArray())
        }
        val summary = repository.importFiles(listOf(android.net.Uri.fromFile(tempImage))) { _, _, _ -> }
        assertEquals(1, summary.successCount)

        val activeItems = database.vaultDao().getActiveItems().first()
        val vaultFile = java.io.File(context.filesDir, "vault_files/${activeItems[0].encryptedFileName}")
        assertTrue(vaultFile.exists())

        // Clear temporary cache
        repository.clearTemporaryCache()

        // Permanent vault file must remain intact
        assertTrue(vaultFile.exists())
        assertTrue(cryptoManager.verifyVaultFileReadable(vaultFile))
    }

    @Test
    fun `import video and audio files and verify decryption integrity`() = runBlocking {
        val testVideo = java.io.File(context.cacheDir, "sample_clip.mp4").apply {
            writeBytes("MP4_SAMPLE_VIDEO_DATA_0000000000".toByteArray())
        }
        val summary = repository.importFiles(listOf(android.net.Uri.fromFile(testVideo))) { _, _, _ -> }
        assertEquals(1, summary.successCount)

        val activeItems = database.vaultDao().getActiveItems().first()
        val videoItem = activeItems.find { it.originalDisplayName == "sample_clip.mp4" }
        assertNotNull(videoItem)
        assertEquals("VIDEO", videoItem!!.type)

        val vaultFile = repository.getEncryptedFile(videoItem)
        assertTrue(vaultFile.exists())
        assertTrue(cryptoManager.verifyVaultFileReadable(vaultFile))

        val decrypted = cryptoManager.decryptFileToBytes(vaultFile)
        assertEquals("MP4_SAMPLE_VIDEO_DATA_0000000000", String(decrypted))
    }

    @Test
    fun `import mixed media batch imports images videos and audio in single operation`() = runBlocking {
        val img1 = java.io.File(context.cacheDir, "vacation1.jpg").apply { writeBytes("IMG_DATA_1".toByteArray()) }
        val img2 = java.io.File(context.cacheDir, "vacation2.png").apply { writeBytes("IMG_DATA_2".toByteArray()) }
        val aud1 = java.io.File(context.cacheDir, "memo.m4a").apply { writeBytes("AUDIO_MEMO_DATA".toByteArray()) }
        val vid1 = java.io.File(context.cacheDir, "movie.mkv").apply { writeBytes("VIDEO_MKV_DATA".toByteArray()) }

        val summary = repository.importFiles(
            listOf(
                android.net.Uri.fromFile(img1),
                android.net.Uri.fromFile(img2),
                android.net.Uri.fromFile(aud1),
                android.net.Uri.fromFile(vid1)
            )
        ) { _, _, _ -> }

        assertEquals(4, summary.totalProcessed)
        assertEquals(4, summary.successCount)
        assertEquals(0, summary.failureCount)

        val activeItems = database.vaultDao().getActiveItems().first()
        assertEquals(4, activeItems.size)
        assertTrue(activeItems.any { it.type == "IMAGE" && it.originalDisplayName == "vacation1.jpg" })
        assertTrue(activeItems.any { it.type == "IMAGE" && it.originalDisplayName == "vacation2.png" })
        assertTrue(activeItems.any { it.type == "AUDIO" && it.originalDisplayName == "memo.m4a" })
        assertTrue(activeItems.any { it.type == "VIDEO" && it.originalDisplayName == "movie.mkv" })
    }

    @Test
    fun `decryptThumbnail handles media gracefully without corrupting vault file`() = runBlocking {
        val testImg = java.io.File(context.cacheDir, "test_thumb.jpg").apply {
            writeBytes("DUMMY_IMAGE_BYTES_FOR_THUMBNAIL".toByteArray())
        }
        val summary = repository.importFiles(listOf(android.net.Uri.fromFile(testImg))) { _, _, _ -> }
        assertEquals(1, summary.successCount)

        val activeItems = database.vaultDao().getActiveItems().first()
        val item = activeItems[0]

        // Calling decryptThumbnail should not fail or corrupt the vault file
        val thumb = repository.decryptThumbnail(item)
        val vaultFile = repository.getEncryptedFile(item)
        assertTrue(vaultFile.exists())
        assertTrue(cryptoManager.verifyVaultFileReadable(vaultFile))
    }
}
