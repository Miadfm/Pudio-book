package com.miadfm.podcasts.data.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.math.max

/**
 * Provides AES-256-GCM authenticated encryption at rest backed by Android Keystore.
 */
class CryptoManager(private val context: Context) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "PodcastsVaultMasterKey_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val BUFFER_SIZE = 64 * 1024 // 64 KB high-performance streaming buffer
    }

    private val keyStore: KeyStore? = try {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    } catch (e: Exception) {
        null
    }

    @Volatile
    private var fallbackKey: SecretKey? = null

    private fun getOrCreateSecretKey(): SecretKey {
        if (keyStore != null) {
            try {
                val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (existingKey != null) {
                    return existingKey.secretKey
                }

                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()

                keyGenerator.init(spec)
                return keyGenerator.generateKey()
            } catch (e: Exception) {
                Log.w("CryptoManager", "Keystore initialization exception, using memory fallback", e)
            }
        }

        // JVM unit testing fallback when AndroidKeyStore provider is unavailable
        return fallbackKey ?: synchronized(this) {
            fallbackKey ?: run {
                val keyGen = KeyGenerator.getInstance("AES")
                keyGen.init(256, SecureRandom())
                val newKey = keyGen.generateKey()
                fallbackKey = newKey
                newKey
            }
        }
    }

    fun getSecretKey(): SecretKey = getOrCreateSecretKey()

    /**
     * Encrypts a byte array and returns: [12-byte IV] + [AES-256-GCM Ciphertext with 16-byte Tag]
     */
    fun encryptBytes(plainBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainBytes)
        val result = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)
        return result
    }

    /**
     * Decrypts a byte array formatted as: [12-byte IV] + [AES-256-GCM Ciphertext with 16-byte Tag]
     */
    fun decryptBytes(encryptedBytes: ByteArray): ByteArray {
        if (encryptedBytes.size < GCM_IV_LENGTH + 16) {
            throw IllegalArgumentException("Invalid encrypted payload size")
        }
        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(encryptedBytes, 0, iv, 0, GCM_IV_LENGTH)
        val cipherText = ByteArray(encryptedBytes.size - GCM_IV_LENGTH)
        System.arraycopy(encryptedBytes, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
        return cipher.doFinal(cipherText)
    }

    /**
     * Encrypts an input stream directly into a target file using Chunked AES-256-GCM authenticated format.
     */
    fun encryptStreamToFile(inputStream: InputStream, destinationFile: File) {
        ChunkedEncryption.encryptStreamToChunkedFile(inputStream, destinationFile, getOrCreateSecretKey())
    }

    /**
     * Decrypts an encrypted file into a target output stream using AES-256-GCM.
     * Supports both chunked VLT2 files and legacy single-file encrypted formats.
     */
    fun decryptFileToStream(encryptedFile: File, outputStream: OutputStream) {
        if (ChunkedEncryption.isChunkedFile(encryptedFile)) {
            ChunkedEncryption.decryptChunkedFileToStream(encryptedFile, outputStream, getOrCreateSecretKey())
            return
        }

        // Legacy single-file GCM format fallback
        FileInputStream(encryptedFile).use { rawFis ->
            val bufferedFis = BufferedInputStream(rawFis, BUFFER_SIZE)
            val iv = ByteArray(GCM_IV_LENGTH)
            val ivRead = bufferedFis.read(iv)
            if (ivRead != GCM_IV_LENGTH) {
                throw IllegalStateException("Corrupted encrypted file: incomplete IV")
            }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)

            val bufferedOutput = if (outputStream is BufferedOutputStream) outputStream else BufferedOutputStream(outputStream, BUFFER_SIZE)
            CipherInputStream(bufferedFis, cipher).use { cis ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (cis.read(buffer).also { bytesRead = it } != -1) {
                    bufferedOutput.write(buffer, 0, bytesRead)
                }
                bufferedOutput.flush()
            }
        }
    }

    /**
     * Decrypts an encrypted file completely into an in-memory byte array.
     */
    fun decryptFileToBytes(encryptedFile: File): ByteArray {
        if (ChunkedEncryption.isChunkedFile(encryptedFile)) {
            val baos = java.io.ByteArrayOutputStream()
            ChunkedEncryption.decryptChunkedFileToStream(encryptedFile, baos, getOrCreateSecretKey())
            return baos.toByteArray()
        }

        FileInputStream(encryptedFile).use { rawFis ->
            val bufferedFis = BufferedInputStream(rawFis, BUFFER_SIZE)
            val iv = ByteArray(GCM_IV_LENGTH)
            val ivRead = bufferedFis.read(iv)
            if (ivRead != GCM_IV_LENGTH) {
                throw IllegalStateException("Corrupted encrypted file: incomplete IV")
            }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)

            CipherInputStream(bufferedFis, cipher).use { cis ->
                return cis.readBytes()
            }
        }
    }

    /**
     * Memory-efficient downsampled image decoding directly from decrypted bytes.
     * Prevents OOM when opening high-resolution camera photos.
     */
    fun decryptImageBitmap(encryptedFile: File, maxDimension: Int = 2048): Bitmap? {
        return try {
            val decryptedBytes = decryptFileToBytes(encryptedFile)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size, options)

            var sampleSize = 1
            if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / sampleSize) >= maxDimension || (halfWidth / sampleSize) >= maxDimension) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Lower RAM consumption on mid-range devices
            }
            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size, decodeOptions)
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error decoding decrypted image bitmap", e)
            null
        }
    }

    /**
     * Fast downsampled thumbnail decoder for smooth gallery scrolling without high memory footprint.
     */
    fun decryptImageThumbnail(encryptedFile: File, targetSize: Int = 256): Bitmap? {
        return try {
            val decryptedBytes = decryptFileToBytes(encryptedFile)
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size, boundsOptions)

            val width = boundsOptions.outWidth
            val height = boundsOptions.outHeight
            var inSampleSize = 1

            if (height > targetSize || width > targetSize) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while ((halfHeight / inSampleSize) >= targetSize && (halfWidth / inSampleSize) >= targetSize) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = max(1, inSampleSize)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size, decodeOptions)
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error decoding thumbnail", e)
            null
        }
    }

    /**
     * Verifies that the encrypted vault file exists, is non-empty, and can be authenticated & decrypted.
     */
    fun verifyVaultFileReadable(encryptedFile: File): Boolean {
        return try {
            if (!encryptedFile.exists() || encryptedFile.length() < 16) {
                return false
            }
            if (ChunkedEncryption.isChunkedFile(encryptedFile)) {
                val header = ChunkedEncryption.readHeader(encryptedFile) ?: return false
                if (header.totalChunks > 0) {
                    val chunk0 = ChunkedEncryption.decryptChunk(encryptedFile, 0, getOrCreateSecretKey(), header)
                    return chunk0 != null
                }
                return true
            }

            FileInputStream(encryptedFile).use { fis ->
                val iv = ByteArray(GCM_IV_LENGTH)
                if (fis.read(iv) != GCM_IV_LENGTH) return false

                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)

                CipherInputStream(fis, cipher).use { cis ->
                    val probe = ByteArray(64)
                    cis.read(probe)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("CryptoManager", "Vault file verification failed", e)
            false
        }
    }
}
