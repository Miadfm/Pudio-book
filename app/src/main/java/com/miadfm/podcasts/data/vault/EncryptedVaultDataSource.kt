package com.miadfm.podcasts.data.vault

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.miadfm.podcasts.data.security.ChunkedEncryption
import com.miadfm.podcasts.data.security.CryptoManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import javax.crypto.SecretKey
import kotlin.math.min

/**
 * Custom Media3 / ExoPlayer DataSource that reads AES-256-GCM chunked encrypted files
 * on-the-fly with O(1) seeking, instant zero-delay startup, and minimal RAM footprint.
 * Also gracefully handles legacy whole-file encrypted Vault items via secure cache fallback.
 */
@OptIn(UnstableApi::class)
class EncryptedVaultDataSource(
    private val encryptedFile: File,
    private val cryptoManager: CryptoManager,
    private val cacheManager: TemporaryPlaybackCacheManager
) : BaseDataSource(/* isNetwork = */ false) {

    companion object {
        private const val TAG = "EncryptedVaultDS"
    }

    private var dataSpec: DataSpec? = null
    private var uri: Uri? = null
    private var secretKey: SecretKey? = null
    private var chunkedHeader: ChunkedEncryption.ChunkedHeader? = null
    private var isChunked: Boolean = false

    private var currentPosition: Long = 0L
    private var bytesRemaining: Long = 0L
    private var totalPlaintextSize: Long = 0L
    private var isOpen: Boolean = false

    // 2-slot LRU chunk cache for microsecond sequential reads
    private var cachedChunkIndex1: Int = -1
    private var cachedChunkBytes1: ByteArray? = null
    private var cachedChunkIndex2: Int = -1
    private var cachedChunkBytes2: ByteArray? = null

    // Legacy fallback state
    private var legacyTempFile: File? = null
    private var legacyRaf: RandomAccessFile? = null

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        this.uri = dataSpec.uri
        transferInitializing(dataSpec)

        if (!encryptedFile.exists()) {
            throw IOException("Encrypted vault file not found: ${encryptedFile.name}")
        }

        secretKey = cryptoManager.getSecretKey()
        isChunked = ChunkedEncryption.isChunkedFile(encryptedFile)

        if (isChunked) {
            val header = ChunkedEncryption.readHeader(encryptedFile)
                ?: throw IOException("Failed to parse chunked encrypted header")
            chunkedHeader = header
            totalPlaintextSize = header.totalPlaintextSize

            if (dataSpec.position > totalPlaintextSize) {
                throw IOException("Requested position ${dataSpec.position} exceeds size $totalPlaintextSize")
            }

            currentPosition = dataSpec.position
            bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                min(dataSpec.length, totalPlaintextSize - currentPosition)
            } else {
                totalPlaintextSize - currentPosition
            }
        } else {
            // Legacy single-file AES-GCM fallback: Decrypt to bounded playback cache once
            prepareLegacyFallback()
            val raf = legacyRaf ?: throw IOException("Legacy media preparation failed")
            val fileLen = raf.length()
            if (dataSpec.position > fileLen) {
                throw IOException("Requested position ${dataSpec.position} exceeds size $fileLen")
            }
            raf.seek(dataSpec.position)
            currentPosition = dataSpec.position
            bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                min(dataSpec.length, fileLen - currentPosition)
            } else {
                fileLen - currentPosition
            }
        }

        isOpen = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    private fun prepareLegacyFallback() {
        if (legacyTempFile != null && legacyTempFile!!.exists() && legacyRaf != null) {
            return
        }
        val tempFile = cacheManager.createTempPlaybackFile("legacy_stream", ".tmp")
        cacheManager.markFileActive(tempFile)
        try {
            FileOutputStream(tempFile).use { fos ->
                cryptoManager.decryptFileToStream(encryptedFile, fos)
            }
            legacyTempFile = tempFile
            legacyRaf = RandomAccessFile(tempFile, "r")
        } catch (e: Exception) {
            cacheManager.safelyDeleteTempFile(tempFile)
            throw IOException("Failed to decrypt legacy media", e)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val bytesToRead = min(length.toLong(), bytesRemaining).toInt()

        val bytesRead = if (isChunked) {
            readChunked(buffer, offset, bytesToRead)
        } else {
            val raf = legacyRaf ?: return C.RESULT_END_OF_INPUT
            val read = raf.read(buffer, offset, bytesToRead)
            if (read > 0) {
                currentPosition += read
                bytesRemaining -= read
                bytesTransferred(read)
            }
            read
        }

        return bytesRead
    }

    private fun readChunked(buffer: ByteArray, offset: Int, length: Int): Int {
        val header = chunkedHeader ?: return C.RESULT_END_OF_INPUT
        val key = secretKey ?: return C.RESULT_END_OF_INPUT

        val chunkSize = header.chunkSize
        val chunkIndex = (currentPosition / chunkSize).toInt()
        val chunkOffset = (currentPosition % chunkSize).toInt()

        val chunkBytes = getOrFetchChunk(chunkIndex, key, header)
            ?: throw IOException("Failed to decrypt chunk $chunkIndex")

        val availableInChunk = chunkBytes.size - chunkOffset
        if (availableInChunk <= 0) {
            return C.RESULT_END_OF_INPUT
        }

        val toCopy = min(length, availableInChunk)
        System.arraycopy(chunkBytes, chunkOffset, buffer, offset, toCopy)

        currentPosition += toCopy
        bytesRemaining -= toCopy
        bytesTransferred(toCopy)
        return toCopy
    }

    private fun getOrFetchChunk(
        chunkIndex: Int,
        key: SecretKey,
        header: ChunkedEncryption.ChunkedHeader
    ): ByteArray? {
        if (cachedChunkIndex1 == chunkIndex && cachedChunkBytes1 != null) {
            return cachedChunkBytes1
        }
        if (cachedChunkIndex2 == chunkIndex && cachedChunkBytes2 != null) {
            return cachedChunkBytes2
        }

        val decrypted = ChunkedEncryption.decryptChunk(encryptedFile, chunkIndex, key, header)
            ?: return null

        // Shift 2-slot cache
        cachedChunkIndex2 = cachedChunkIndex1
        cachedChunkBytes2 = cachedChunkBytes1
        cachedChunkIndex1 = chunkIndex
        cachedChunkBytes1 = decrypted

        return decrypted
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        if (isOpen) {
            isOpen = false
            transferEnded()
        }
        dataSpec = null
        uri = null
        cachedChunkBytes1 = null
        cachedChunkBytes2 = null
        cachedChunkIndex1 = -1
        cachedChunkIndex2 = -1

        try {
            legacyRaf?.close()
        } catch (_: Exception) {}
        legacyRaf = null

        legacyTempFile?.let {
            cacheManager.safelyDeleteTempFile(it)
            legacyTempFile = null
        }
    }

    class Factory(
        private val encryptedFile: File,
        private val cryptoManager: CryptoManager,
        private val cacheManager: TemporaryPlaybackCacheManager
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return EncryptedVaultDataSource(encryptedFile, cryptoManager, cacheManager)
        }
    }
}
