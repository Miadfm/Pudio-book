package com.miadfm.podcasts.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miadfm.podcasts.data.podcast.Episode
import com.miadfm.podcasts.ui.components.AudioPlayerBar
import com.miadfm.podcasts.ui.components.EmptyStateView
import com.miadfm.podcasts.ui.components.PodcastPlayerSheet
import com.miadfm.podcasts.ui.podcasts.PodcastEpisodeCard
import com.miadfm.podcasts.ui.i18n.appStrings
import com.miadfm.podcasts.ui.theme.BlueAccent
import com.miadfm.podcasts.ui.theme.BlueContainer
import com.miadfm.podcasts.ui.theme.CharcoalBlack
import com.miadfm.podcasts.ui.theme.TextPrimary
import com.miadfm.podcasts.ui.theme.TextSecondary
import com.miadfm.podcasts.viewmodel.PodcastUiState

@Composable
fun LibraryScreen(
    uiState: PodcastUiState,
    onPlayEpisode: (Episode) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onToggleFavorite: (Episode) -> Unit,
    modifier: Modifier = Modifier,
    onSpeedChange: (Float) -> Unit = {}
) {
    val strings = appStrings()
    var showPlayerSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp)
        ) {
            // Title Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(BlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = strings.libraryTitle,
                        tint = BlueAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = strings.libraryTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.favoriteEpisodes.size} ${strings.favoriteEpisodesCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.favoriteEpisodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        icon = Icons.Default.FavoriteBorder,
                        title = strings.noFavoritesTitle,
                        message = strings.noFavoritesMessage
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("library_favorites_list"),
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        top = 8.dp,
                        bottom = if (uiState.playerState.currentEpisode != null) 175.dp else 95.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(uiState.favoriteEpisodes, key = { _, it -> it.id }) { index, episode ->
                        val isCurrentPlaying = uiState.playerState.currentEpisode?.id == episode.id
                        val isPlayingThis = isCurrentPlaying && uiState.playerState.isPlaying
                        PodcastEpisodeCard(
                            episode = episode,
                            isCurrentPlaying = isCurrentPlaying,
                            isPlaying = isPlayingThis,
                            currentPositionMs = if (isCurrentPlaying) uiState.playerState.currentPositionMs else 0L,
                            durationMs = if (isCurrentPlaying && uiState.playerState.durationMs > 0) uiState.playerState.durationMs else (episode.durationSeconds.toLong() * 1000L),
                            onCardClick = { onPlayEpisode(episode) },
                            onPlayClick = {
                                if (isCurrentPlaying) onTogglePlayPause() else onPlayEpisode(episode)
                            },
                            onFavoriteClick = { onToggleFavorite(episode) },
                            testTagIndex = index
                        )
                    }
                }
            }
        }

        // Bottom Docked Internal Player Bar
        if (uiState.playerState.currentEpisode != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                AudioPlayerBar(
                    playerState = uiState.playerState,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekTo = onSeekTo,
                    onSkipBackward = onSkipBackward,
                    onSkipForward = onSkipForward,
                    onOpenPlayer = { showPlayerSheet = true }
                )
            }
        }

        // Full Screen/Sheet Podcast Player
        if (showPlayerSheet && uiState.playerState.currentEpisode != null) {
            PodcastPlayerSheet(
                playerState = uiState.playerState,
                onDismiss = { showPlayerSheet = false },
                onTogglePlayPause = onTogglePlayPause,
                onSeekTo = onSeekTo,
                onSkipBackward = onSkipBackward,
                onSkipForward = onSkipForward,
                onSpeedChange = onSpeedChange,
                onToggleFavorite = onToggleFavorite
            )
        }
    }
}
