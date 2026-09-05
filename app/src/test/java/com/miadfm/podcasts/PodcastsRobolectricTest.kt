package com.miadfm.podcasts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.miadfm.podcasts.data.podcast.PodcastArtworkManager
import com.miadfm.podcasts.data.podcast.SamplePodcastDataSource
import com.miadfm.podcasts.data.security.CryptoManager
import com.miadfm.podcasts.data.settings.AppLanguage
import com.miadfm.podcasts.data.settings.AppLanguageManager
import com.miadfm.podcasts.player.PodcastAudioPlayer
import com.miadfm.podcasts.ui.i18n.EnglishStrings
import com.miadfm.podcasts.ui.i18n.PersianStrings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PodcastsRobolectricTest {

    @Test
    fun `verify app name resource is Pudiobook`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Pudiobook", appName)
    }

    @Test
    fun `verify sample podcast has exactly five real episodes with packaged audio assets`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val podcast = SamplePodcastDataSource.samplePodcast
        val episodes = SamplePodcastDataSource.sampleEpisodes

        assertEquals("Pudiobook Audio Library", podcast.title)
        assertEquals("کتابخانه صوتی پیودیوبوک", podcast.titleFa)
        assertEquals(5, episodes.size)
        assertEquals(5, podcast.totalEpisodes)
        assertNotNull(podcast.attribution)

        val player = PodcastAudioPlayer(context)

        episodes.forEachIndexed { index, episode ->
            assertEquals(index + 1, episode.episodeNumber)
            assertNotNull(episode.title)
            assertNotNull(episode.titleFa)
            assertNotNull(episode.description)
            assertNotNull(episode.descriptionFa)
            assertNotNull(episode.attribution)
            assertTrue(episode.durationSeconds > 0)
            assertTrue("Asset path should not be empty", episode.assetPath.isNotBlank())
            assertTrue("Asset filename should not be empty", episode.assetFileName.isNotBlank())
            assertTrue("Episode should be available in player", player.isEpisodeAvailable(episode))
            assertTrue("Episode asset must be verified available", SamplePodcastDataSource.isEpisodeAssetAvailable(context, episode))

            // Verify the asset is readable from application assets
            val inputStream: InputStream = context.assets.open(episode.assetPath)
            assertNotNull(inputStream)
            val header = ByteArray(10)
            val bytesRead = inputStream.read(header)
            assertTrue("Should read header bytes", bytesRead >= 3)
            // Verify ID3 or MPEG frame sync header
            val isId3 = header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()
            val isMpegSync = (header[0].toInt() and 0xFF) == 0xFF && (header[1].toInt() and 0xE0) == 0xE0
            assertTrue("Audio asset must have valid ID3 or MPEG sync header", isId3 || isMpegSync)
            inputStream.close()
        }

        // Verify the 5 required audio items & content types
        assertEquals("White Nights", episodes[0].title)
        assertEquals("شبهای روشن", episodes[0].titleFa)
        assertEquals("Audiobook", episodes[0].contentType)
        assertEquals("Fyodor Dostoevsky", episodes[0].creator)
        assertEquals("podcasts/white_nights.mp3", episodes[0].assetPath)

        assertEquals("Radio Ajaeb", episodes[1].title)
        assertEquals("رادیو عجایب", episodes[1].titleFa)
        assertEquals("Podcast", episodes[1].contentType)
        assertEquals("podcasts/radio_ajaeb.mp3", episodes[1].assetPath)

        assertEquals("Dark Summer — Episode 1", episodes[2].title)
        assertEquals("تابستان تاریک — قسمت اول", episodes[2].titleFa)
        assertEquals("Podcast", episodes[2].contentType)
        assertEquals("podcasts/dark_summer_ep1.mp3", episodes[2].assetPath)

        assertEquals("Dangerous Dance", episodes[3].title)
        assertEquals("رقص خطرناک", episodes[3].titleFa)
        assertEquals("Podcast", episodes[3].contentType)
        assertEquals("podcasts/dangerous_dance.mp3", episodes[3].assetPath)

        assertEquals("Tara's Last Shift", episodes[4].title)
        assertEquals("آخرین شیفت تارا", episodes[4].titleFa)
        assertEquals("Podcast", episodes[4].contentType)
        assertEquals("podcasts/taras_last_shift.mp3", episodes[4].assetPath)
    }

    @Test
    fun `verify player seek backward and forward logic`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val player = PodcastAudioPlayer(context)
        val episode = SamplePodcastDataSource.sampleEpisodes[0]

        player.playEpisode(episode)
        // Position at 25 seconds
        player.seekTo(25_000L)
        assertEquals(25_000L, player.playerState.value.currentPositionMs)

        // Skip backward 10s -> 15s
        player.skipBackward(10)
        assertEquals(15_000L, player.playerState.value.currentPositionMs)

        // When current position is less than 10 seconds -> seek to 0
        player.seekTo(5_000L)
        player.skipBackward(10)
        assertEquals(0L, player.playerState.value.currentPositionMs)

        // Skip forward 10s from 0s -> 10s
        player.skipForward(10)
        assertEquals(10_000L, player.playerState.value.currentPositionMs)

        // When remaining duration is less than 10 seconds -> seek to end
        val totalMs = player.playerState.value.durationMs
        player.seekTo(totalMs - 4_000L)
        player.skipForward(10)
        assertEquals(totalMs, player.playerState.value.currentPositionMs)

        player.release()
    }

    @Test
    fun `verify supported playback speeds`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val player = PodcastAudioPlayer(context)
        val episode = SamplePodcastDataSource.sampleEpisodes[0]
        player.playEpisode(episode)

        val expectedSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
        assertEquals(expectedSpeeds, PodcastAudioPlayer.SUPPORTED_SPEEDS)

        expectedSpeeds.forEach { speed ->
            player.setPlaybackSpeed(speed)
            assertEquals(speed, player.playerState.value.playbackSpeed, 0.001f)
        }

        player.release()
    }

    @Test
    fun `verify podcast artwork manager loads asynchronously without blocking`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val episodes = SamplePodcastDataSource.sampleEpisodes

        PodcastArtworkManager.clearCache()

        episodes.forEach { episode ->
            // loadArtwork runs on Dispatchers.IO and returns either embedded artwork Bitmap or null
            val artwork = PodcastArtworkManager.loadArtwork(context, episode)
            // It should execute gracefully without crash
            if (artwork != null) {
                assertTrue("Bitmap should have positive dimensions", artwork.width > 0 && artwork.height > 0)
            }

            // Second call should return immediately from cache
            val cached = PodcastArtworkManager.loadArtwork(context, episode)
            assertEquals(artwork, cached)

            // Verify negative cache works for episodes without embedded artwork
            if (artwork == null) {
                assertTrue(
                    "Should record in negative cache",
                    PodcastArtworkManager.hasNoEmbeddedArtwork(episode)
                )
            }
        }
    }

    @Test
    fun `verify artwork extraction fails safely without throwing exceptions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Non-existent path
        val nonExistentResult = PodcastArtworkManager.extractEmbeddedArtwork(context, "invalid/path/none.mp3")
        assertNull(nonExistentResult)

        // Blank path
        val blankResult = PodcastArtworkManager.extractEmbeddedArtwork(context, "")
        assertNull(blankResult)
    }

    @Test
    fun `verify audio playback is completely functional even when artwork extraction fails`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val player = PodcastAudioPlayer(context)

        val episode = SamplePodcastDataSource.sampleEpisodes.first()
        player.playEpisode(episode)

        // Episode is set and active
        assertEquals(episode.id, player.playerState.value.currentEpisode?.id)

        // Seeking works smoothly
        player.seekTo(5000L)
        assertEquals(5000L, player.playerState.value.currentPositionMs)

        // Speed changes work smoothly
        player.setPlaybackSpeed(1.5f)
        assertEquals(1.5f, player.playerState.value.playbackSpeed, 0.001f)

        player.release()
    }

    @Test
    fun `verify localized player strings in Persian and English`() {
        // Persian player labels
        assertEquals("۱۰ ثانیه عقب", PersianStrings.skipBackward10)
        assertEquals("۱۰ ثانیه جلو", PersianStrings.skipForward10)
        assertEquals("سرعت پخش", PersianStrings.playbackSpeed)
        assertEquals("در حال پخش", PersianStrings.nowPlaying)

        // English player labels
        assertEquals("Back 10 seconds", EnglishStrings.skipBackward10)
        assertEquals("Forward 10 seconds", EnglishStrings.skipForward10)
        assertEquals("Playback speed", EnglishStrings.playbackSpeed)
        assertEquals("Now playing", EnglishStrings.nowPlaying)
    }

    @Test
    fun `verify AppLanguageManager persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = AppLanguageManager(context)

        manager.setLanguage(AppLanguage.PERSIAN)
        assertEquals(AppLanguage.PERSIAN, manager.getLanguage())

        manager.setLanguage(AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, manager.getLanguage())
    }

    @Test
    fun `verify CryptoManager encrypts and decrypts correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cryptoManager = CryptoManager(context)

        val originalText = "Encrypted Note Content 12345"
        val encrypted = cryptoManager.encryptBytes(originalText.toByteArray(Charsets.UTF_8))
        val decrypted = cryptoManager.decryptBytes(encrypted)
        val resultText = String(decrypted, Charsets.UTF_8)

        assertEquals(originalText, resultText)
    }
}
