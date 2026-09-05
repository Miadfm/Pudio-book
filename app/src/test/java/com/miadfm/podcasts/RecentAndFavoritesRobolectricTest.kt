package com.miadfm.podcasts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.miadfm.podcasts.data.podcast.PodcastHistoryManager
import com.miadfm.podcasts.data.podcast.SamplePodcastDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecentAndFavoritesRobolectricTest {

    private lateinit var context: Context
    private lateinit var historyManager: PodcastHistoryManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("podcast_playback_history_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        historyManager = PodcastHistoryManager(context)
    }

    @Test
    fun `historyManager records playback and updates timestamp`() = runBlocking {
        val episodes = SamplePodcastDataSource.sampleEpisodes
        assertEquals(5, episodes.size)

        // Initially no recent history
        val initialMap = historyManager.historyMap.first()
        assertTrue(initialMap.isEmpty())

        // Play episode 1
        historyManager.recordPlayback(episodes[0].id, 1000L)
        val mapAfter1 = historyManager.historyMap.first()
        assertTrue(mapAfter1.containsKey(episodes[0].id))
        assertEquals(1000L, mapAfter1[episodes[0].id])

        // Play episode 2
        historyManager.recordPlayback(episodes[1].id, 2000L)
        val mapAfter2 = historyManager.historyMap.first()
        assertEquals(2, mapAfter2.size)
        assertTrue(mapAfter2[episodes[1].id]!! > mapAfter2[episodes[0].id]!!)
    }

    @Test
    fun `recent episodes calculation takes maximum 5 items sorted newest first`() = runBlocking {
        val episodes = SamplePodcastDataSource.sampleEpisodes

        // Record playback with explicit timestamps
        historyManager.recordPlayback(episodes[0].id, 1000L)
        historyManager.recordPlayback(episodes[1].id, 2000L)
        historyManager.recordPlayback(episodes[2].id, 3000L)

        val historyMap = historyManager.historyMap.first()
        val calculatedRecent = episodes
            .filter { historyMap.containsKey(it.id) }
            .sortedByDescending { historyMap[it.id] ?: 0L }
            .take(5)

        assertEquals(3, calculatedRecent.size)
        assertEquals(episodes[2].id, calculatedRecent[0].id)
        assertEquals(episodes[1].id, calculatedRecent[1].id)
        assertEquals(episodes[0].id, calculatedRecent[2].id)

        // Replaying episode 0 moves it to top without duplicates
        historyManager.recordPlayback(episodes[0].id, 4000L)
        val updatedHistoryMap = historyManager.historyMap.first()
        val updatedRecent = episodes
            .filter { updatedHistoryMap.containsKey(it.id) }
            .sortedByDescending { updatedHistoryMap[it.id] ?: 0L }
            .take(5)

        assertEquals(3, updatedRecent.size)
        assertEquals(episodes[0].id, updatedRecent[0].id)
        assertEquals(episodes[2].id, updatedRecent[1].id)
        assertEquals(episodes[1].id, updatedRecent[2].id)
    }

    @Test
    fun `history persists across historyManager instances`() = runBlocking {
        val episodes = SamplePodcastDataSource.sampleEpisodes
        historyManager.recordPlayback(episodes[0].id, 12345L)

        // Create new historyManager instance reading from same SharedPreferences
        val newManager = PodcastHistoryManager(context)
        val map = newManager.historyMap.first()
        assertEquals(12345L, map[episodes[0].id])
    }

    @Test
    fun `clearHistory empties recent playback`() = runBlocking {
        val episodes = SamplePodcastDataSource.sampleEpisodes
        historyManager.recordPlayback(episodes[0].id, 12345L)
        assertEquals(1, historyManager.historyMap.first().size)

        historyManager.clearHistory()
        assertTrue(historyManager.historyMap.first().isEmpty())

        val newManager = PodcastHistoryManager(context)
        assertTrue(newManager.historyMap.first().isEmpty())
    }
}
