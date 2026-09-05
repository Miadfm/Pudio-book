package com.miadfm.podcasts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miadfm.podcasts.data.podcast.Episode
import com.miadfm.podcasts.data.podcast.Podcast
import com.miadfm.podcasts.data.podcast.PodcastHistoryManager
import com.miadfm.podcasts.data.podcast.SamplePodcastDataSource
import com.miadfm.podcasts.data.settings.AppLanguage
import com.miadfm.podcasts.data.settings.AppLanguageManager
import com.miadfm.podcasts.data.vault.VaultRepository
import com.miadfm.podcasts.player.PodcastAudioPlayer
import com.miadfm.podcasts.player.PlayerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PodcastUiState(
    val podcast: Podcast = SamplePodcastDataSource.samplePodcast,
    val episodes: List<Episode> = emptyList(),
    val recentEpisodes: List<Episode> = emptyList(),
    val favoriteEpisodes: List<Episode> = emptyList(),
    val playerState: PlayerState = PlayerState(),
    val language: AppLanguage = AppLanguage.ENGLISH
)

class PodcastViewModel(
    private val vaultRepository: VaultRepository,
    private val audioPlayer: PodcastAudioPlayer,
    private val languageManager: AppLanguageManager,
    private val historyManager: PodcastHistoryManager
) : ViewModel() {

    val uiState: StateFlow<PodcastUiState> = combine(
        vaultRepository.allFavorites,
        historyManager.historyMap,
        audioPlayer.playerState,
        languageManager.currentLanguage
    ) { favorites, historyMap, playerState, language ->
        val favIds = favorites.map { it.episodeId }.toSet()
        val isPersian = (language == AppLanguage.PERSIAN)
        val basePodcast = SamplePodcastDataSource.samplePodcast
        val localizedPodcast = if (isPersian) {
            basePodcast.copy(
                title = basePodcast.titleFa ?: basePodcast.title,
                author = basePodcast.authorFa ?: basePodcast.author,
                description = basePodcast.descriptionFa ?: basePodcast.description,
                category = basePodcast.categoryFa ?: basePodcast.category
            )
        } else {
            basePodcast
        }

        val allEpisodes = SamplePodcastDataSource.sampleEpisodes.map { ep ->
            val lastPlayed = historyMap[ep.id]
            val isFav = favIds.contains(ep.id)
            val localizedEp = if (isPersian) {
                ep.copy(
                    title = ep.titleFa ?: ep.title,
                    description = ep.descriptionFa ?: ep.description,
                    publishDate = ep.publishDateFa ?: ep.publishDate,
                    formattedDuration = ep.formattedDurationFa ?: ep.formattedDuration,
                    isFavorite = isFav,
                    lastPlayedAt = lastPlayed
                )
            } else {
                ep.copy(
                    isFavorite = isFav,
                    lastPlayedAt = lastPlayed
                )
            }
            localizedEp
        }
        val favoriteEpisodes = allEpisodes.filter { it.isFavorite }
        val recentEpisodes = allEpisodes
            .filter { it.lastPlayedAt != null }
            .sortedByDescending { it.lastPlayedAt ?: 0L }
            .take(5)

        PodcastUiState(
            podcast = localizedPodcast,
            episodes = allEpisodes,
            recentEpisodes = recentEpisodes,
            favoriteEpisodes = favoriteEpisodes,
            playerState = playerState,
            language = language
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PodcastUiState(
            podcast = SamplePodcastDataSource.samplePodcast,
            episodes = SamplePodcastDataSource.sampleEpisodes,
            language = languageManager.getLanguage()
        )
    )

    fun playEpisode(episode: Episode) {
        historyManager.recordPlayback(episode.id)
        audioPlayer.playEpisode(episode)
    }

    fun togglePlayPause() {
        if (uiState.value.playerState.isPlaying) {
            audioPlayer.pause()
        } else {
            audioPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        audioPlayer.seekTo(positionMs)
    }

    fun skipBackward() {
        audioPlayer.skipBackward(10)
    }

    fun skipForward() {
        audioPlayer.skipForward(10)
    }

    fun setPlaybackSpeed(speed: Float) {
        audioPlayer.setPlaybackSpeed(speed)
    }

    fun toggleFavorite(episode: Episode) {
        viewModelScope.launch {
            vaultRepository.toggleFavorite(episode.id, !episode.isFavorite)
        }
    }

    fun setLanguage(language: AppLanguage) {
        languageManager.setLanguage(language)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
