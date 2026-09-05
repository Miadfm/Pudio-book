package com.miadfm.podcasts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.miadfm.podcasts.data.security.ChunkedEncryption
import com.miadfm.podcasts.data.security.CryptoManager
import com.miadfm.podcasts.data.security.PinSecurityManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SecurityUnitTest {

    private lateinit var context: Context
    private lateinit var pinSecurityManager: PinSecurityManager
    private lateinit var cryptoManager: CryptoManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        pinSecurityManager = PinSecurityManager(context)
        cryptoManager = CryptoManager(context)
    }

    @Test
    fun `isPinSet initially returns false`() {
        assertFalse(pinSecurityManager.isPinSet())
    }

    @Test
    fun `reject PIN shorter than 4 digits`() {
        assertFalse(pinSecurityManager.setPin("123"))
        assertFalse(pinSecurityManager.isPinSet())
    }

    @Test
    fun `reject PIN longer than 8 digits`() {
        assertFalse(pinSecurityManager.setPin("123456789"))
        assertFalse(pinSecurityManager.isPinSet())
    }

    @Test
    fun `reject non-numeric PIN`() {
        assertFalse(pinSecurityManager.setPin("12ab"))
        assertFalse(pinSecurityManager.isPinSet())
    }

    @Test
    fun `accept valid 4 to 8 digit PIN and verify successfully`() {
        assertTrue(pinSecurityManager.setPin("4829"))
        assertTrue(pinSecurityManager.isPinSet())

        assertTrue(pinSecurityManager.verifyPin("4829"))
        assertFalse(pinSecurityManager.verifyPin("0000"))
        assertFalse(pinSecurityManager.verifyPin("4828"))
    }

    @Test
    fun `update PIN with new valid code`() {
        assertTrue(pinSecurityManager.setPin("1234"))
        assertTrue(pinSecurityManager.verifyPin("1234"))

        assertTrue(pinSecurityManager.setPin("87654321"))
        assertTrue(pinSecurityManager.verifyPin("87654321"))
        assertFalse(pinSecurityManager.verifyPin("1234"))
    }

    @Test
    fun `cryptoManager encryptBytes and decryptBytes roundtrip`() {
        val originalText = "Secret podcast vault note content: 987654321"
        val plainBytes = originalText.toByteArray(Charsets.UTF_8)

        val encrypted = cryptoManager.encryptBytes(plainBytes)
        assertTrue(encrypted.size > plainBytes.size)

        val decrypted = cryptoManager.decryptBytes(encrypted)
        assertEquals(originalText, String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun `chunked encryption encrypts stream and decrypts correctly without caller-provided IV error`() {
        val testData = "CHUNKED_AUTHENTICATED_ENCRYPTION_STREAM_CONTENT_".repeat(2000).toByteArray()
        val inputStream = ByteArrayInputStream(testData)
        val targetFile = File(context.cacheDir, "test_chunked_out.vlt")

        cryptoManager.encryptStreamToFile(inputStream, targetFile)
        assertTrue(targetFile.exists())
        assertTrue(ChunkedEncryption.isChunkedFile(targetFile))

        val header = ChunkedEncryption.readHeader(targetFile)
        assertNotNull(header)
        assertEquals(testData.size.toLong(), header!!.totalPlaintextSize)
        assertTrue(header.totalChunks >= 1)

        // Decrypt back to stream
        val outputBaos = ByteArrayOutputStream()
        cryptoManager.decryptFileToStream(targetFile, outputBaos)
        assertArrayEquals(testData, outputBaos.toByteArray())

        // Decrypt to bytes directly
        val decryptedBytes = cryptoManager.decryptFileToBytes(targetFile)
        assertArrayEquals(testData, decryptedBytes)
    }

    @Test
    fun `legacy single-file AES-GCM file remains readable`() {
        val secretKey = cryptoManager.getSecretKey()
        val originalText = "Legacy format unchunked file content for backward compatibility"
        val plainBytes = originalText.toByteArray(Charsets.UTF_8)

        // Create legacy encrypted file format: [12-byte IV] + [AES/GCM ciphertext]
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plainBytes)

        val legacyFile = File(context.cacheDir, "legacy_file.enc").apply {
            outputStream().use { fos ->
                fos.write(iv)
                fos.write(ciphertext)
            }
        }

        assertFalse(ChunkedEncryption.isChunkedFile(legacyFile))
        assertTrue(cryptoManager.verifyVaultFileReadable(legacyFile))

        val decryptedBytes = cryptoManager.decryptFileToBytes(legacyFile)
        assertEquals(originalText, String(decryptedBytes, Charsets.UTF_8))

        val baos = ByteArrayOutputStream()
        cryptoManager.decryptFileToStream(legacyFile, baos)
        assertEquals(originalText, String(baos.toByteArray(), Charsets.UTF_8))
    }
}
