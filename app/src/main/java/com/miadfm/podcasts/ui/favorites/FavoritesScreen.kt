package com.miadfm.podcasts.ui.favorites

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miadfm.podcasts.data.podcast.Episode
import com.miadfm.podcasts.ui.components.AudioPlayerBar
import com.miadfm.podcasts.ui.components.PodcastPlayerSheet
import com.miadfm.podcasts.ui.components.PudiobookTopBar
import com.miadfm.podcasts.ui.i18n.appStrings
import com.miadfm.podcasts.ui.podcasts.PodcastEpisodeCard
import com.miadfm.podcasts.ui.theme.CharcoalBlack
import com.miadfm.podcasts.ui.theme.CharcoalBorder
import com.miadfm.podcasts.ui.theme.CharcoalDark
import com.miadfm.podcasts.ui.theme.ForestGreen
import com.miadfm.podcasts.ui.theme.TextPrimary
import com.miadfm.podcasts.ui.theme.TextSecondary
import com.miadfm.podcasts.viewmodel.PodcastUiState

@Composable
fun FavoritesScreen(
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
            .testTag("favorites_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PudiobookTopBar(
                subtitle = strings.navFavorites
            )

            if (uiState.favoriteEpisodes.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp)
                        .testTag("favorites_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CharcoalDark,
                            border = BorderStroke(1.dp, CharcoalBorder),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = ForestGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = strings.noFavoritesYet,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.noFavoritesYetSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Favorites list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("favorites_list"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = if (uiState.playerState.currentEpisode != null) 96.dp else 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = uiState.favoriteEpisodes,
                        key = { _, ep -> ep.id }
                    ) { index, episode ->
                        val isCurrentPlaying = uiState.playerState.currentEpisode?.id == episode.id
                        val isPlaying = isCurrentPlaying && uiState.playerState.isPlaying

                        PodcastEpisodeCard(
                            episode = episode,
                            isCurrentPlaying = isCurrentPlaying,
                            isPlaying = isPlaying,
                            currentPositionMs = if (isCurrentPlaying) uiState.playerState.currentPositionMs else 0L,
                            durationMs = if (isCurrentPlaying && uiState.playerState.durationMs > 0) {
                                uiState.playerState.durationMs
                            } else {
                                (episode.durationSeconds.toLong() * 1000L)
                            },
                            onCardClick = {
                                if (isCurrentPlaying) {
                                    showPlayerSheet = true
                                } else {
                                    onPlayEpisode(episode)
                                }
                            },
                            onPlayClick = {
                                if (isCurrentPlaying) {
                                    onTogglePlayPause()
                                } else {
                                    onPlayEpisode(episode)
                                }
                            },
                            onFavoriteClick = { onToggleFavorite(episode) },
                            testTagIndex = index
                        )
                    }
                }
            }
        }

        // Docked Audio Player Bar
        AnimatedVisibility(
            visible = uiState.playerState.currentEpisode != null,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AudioPlayerBar(
                playerState = uiState.playerState,
                onTogglePlayPause = onTogglePlayPause,
                onSeekTo = onSeekTo,
                onSkipBackward = onSkipBackward,
                onSkipForward = onSkipForward,
                onSpeedChange = onSpeedChange,
                onOpenPlayer = { showPlayerSheet = true }
            )
        }

        // Full Screen Player Sheet
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
