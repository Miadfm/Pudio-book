package com.miadfm.podcasts.data.security

import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * High-performance Chunked Authenticated Encryption (AES-256-GCM) designed for
 * instant-startup media streaming and random-access seeking without full-file decryption.
 */
object ChunkedEncryption {
    const val MAGIC: Int = 0x564C5432 // "VLT2"
    const val VERSION: Short = 1
    const val DEFAULT_CHUNK_SIZE: Int = 256 * 1024 // 256 KB plaintext chunks
    const val GCM_TAG_LENGTH: Int = 128
    const val GCM_IV_LENGTH: Int = 12
    const val HEADER_SIZE: Int = 4 + 2 + 4 + 8 + 4 // 22 bytes
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    data class ChunkedHeader(
        val magic: Int,
        val version: Short,
        val chunkSize: Int,
        val totalPlaintextSize: Long,
        val totalChunks: Int
    )

    /**
     * Checks if a file has the VLT2 chunked encrypted format.
     */
    fun isChunkedFile(file: File): Boolean {
        if (!file.exists() || file.length() < HEADER_SIZE) return false
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val magic = raf.readInt()
                magic == MAGIC
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reads the 22-byte header from a chunked encrypted file.
     */
    fun readHeader(file: File): ChunkedHeader? {
        if (!file.exists() || file.length() < HEADER_SIZE) return null
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val magic = raf.readInt()
                if (magic != MAGIC) return null
                val version = raf.readShort()
                val chunkSize = raf.readInt()
                val totalPlaintextSize = raf.readLong()
                val totalChunks = raf.readInt()
                ChunkedHeader(magic, version, chunkSize, totalPlaintextSize, totalChunks)
            }
        } catch (e: Exception) {
            Log.e("ChunkedEncryption", "Error reading chunked header from ${file.name}", e)
            null
        }
    }

    /**
     * Encrypts an input stream into a chunked AES-256-GCM authenticated file.
     * Each chunk is authenticated independently with its own IV and AAD (chunk index).
     */
    fun encryptStreamToChunkedFile(
        inputStream: InputStream,
        destinationFile: File,
        secretKey: SecretKey,
        chunkSize: Int = DEFAULT_CHUNK_SIZE
    ) {
        val tempEncFile = File(destinationFile.parentFile, "${destinationFile.name}.tmp_${System.currentTimeMillis()}")

        var totalBytesWritten = 0L
        var chunkCount = 0

        try {
            RandomAccessFile(tempEncFile, "rw").use { raf ->
                raf.seek(0)
                // Write placeholder header (22 bytes)
                raf.writeInt(MAGIC)
                raf.writeShort(VERSION.toInt())
                raf.writeInt(chunkSize)
                raf.writeLong(0L) // Placeholder for total plain text length
                raf.writeInt(0)  // Placeholder for total chunk count

                val buffer = ByteArray(chunkSize)
                val bufferedInput = if (inputStream is BufferedInputStream) inputStream else BufferedInputStream(inputStream, 64 * 1024)

                while (true) {
                    var offset = 0
                    while (offset < chunkSize) {
                        val read = bufferedInput.read(buffer, offset, chunkSize - offset)
                        if (read == -1) break
                        offset += read
                    }
                    if (offset == 0) break

                    val chunkPlaintextLen = offset
                    totalBytesWritten += chunkPlaintextLen

                    // Initialize encryption without a caller-provided IV so Keystore-backed keys generate a fresh IV securely
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                    val iv = cipher.iv ?: throw IllegalStateException("Cipher did not generate IV")
                    if (iv.size != GCM_IV_LENGTH) {
                        throw IllegalStateException("Unexpected IV length ${iv.size}, expected $GCM_IV_LENGTH")
                    }

                    // Additional Authenticated Data: chunk index
                    val aad = ByteBuffer.allocate(4).putInt(chunkCount).array()
                    cipher.updateAAD(aad)

                    val cipherTextWithTag = cipher.doFinal(buffer, 0, chunkPlaintextLen)

                    // Write chunk record: [4-byte pt len] [4-byte ct len] [12-byte IV] [ciphertext + tag]
                    raf.writeInt(chunkPlaintextLen)
                    raf.writeInt(cipherTextWithTag.size)
                    raf.write(iv)
                    raf.write(cipherTextWithTag)

                    chunkCount++
                }

                // Finalize header with exact plain text size and total chunk count
                raf.seek(0)
                raf.writeInt(MAGIC)
                raf.writeShort(VERSION.toInt())
                raf.writeInt(chunkSize)
                raf.writeLong(totalBytesWritten)
                raf.writeInt(chunkCount)
            }

            if (destinationFile.exists()) destinationFile.delete()
            if (!tempEncFile.renameTo(destinationFile)) {
                tempEncFile.copyTo(destinationFile, overwrite = true)
                tempEncFile.delete()
            }
        } catch (e: Exception) {
            if (tempEncFile.exists()) tempEncFile.delete()
            throw e
        }
    }

    /**
     * Decrypts a specific chunk from a chunked file in O(1) time.
     */
    fun decryptChunk(
        file: File,
        chunkIndex: Int,
        secretKey: SecretKey,
        header: ChunkedHeader? = null
    ): ByteArray? {
        val h = header ?: readHeader(file) ?: return null
        if (chunkIndex < 0 || chunkIndex >= h.totalChunks) return null

        return try {
            RandomAccessFile(file, "r").use { raf ->
                // Chunks 0 until totalChunks - 2 are guaranteed to have full standard size
                val standardDiskChunkSize = 4L + 4L + GCM_IV_LENGTH + (h.chunkSize + 16L)
                val chunkOffset = HEADER_SIZE + chunkIndex * standardDiskChunkSize

                raf.seek(chunkOffset)
                val plainTextLen = raf.readInt()
                val cipherTextLen = raf.readInt()
                val iv = ByteArray(GCM_IV_LENGTH)
                raf.readFully(iv)

                val cipherTextWithTag = ByteArray(cipherTextLen)
                raf.readFully(cipherTextWithTag)

                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                val aad = ByteBuffer.allocate(4).putInt(chunkIndex).array()
                cipher.updateAAD(aad)

                cipher.doFinal(cipherTextWithTag)
            }
        } catch (e: Exception) {
            Log.e("ChunkedEncryption", "Failed to decrypt chunk $chunkIndex of ${file.name}", e)
            null
        }
    }

    /**
     * Streams full decryption of chunked file to an output stream (e.g. for SAF Unhide).
     */
    fun decryptChunkedFileToStream(
        file: File,
        outputStream: OutputStream,
        secretKey: SecretKey
    ) {
        val header = readHeader(file) ?: throw IllegalStateException("Invalid chunked file header")
        val bufferedOut = if (outputStream is BufferedOutputStream) outputStream else BufferedOutputStream(outputStream, 64 * 1024)

        for (i in 0 until header.totalChunks) {
            val decryptedBytes = decryptChunk(file, i, secretKey, header)
                ?: throw IllegalStateException("Failed to decrypt chunk $i")
            bufferedOut.write(decryptedBytes)
        }
        bufferedOut.flush()
    }
}
