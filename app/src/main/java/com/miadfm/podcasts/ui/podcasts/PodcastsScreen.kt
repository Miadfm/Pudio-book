package com.miadfm.podcasts.ui.podcasts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miadfm.podcasts.data.podcast.Episode
import com.miadfm.podcasts.data.settings.AppLanguage
import com.miadfm.podcasts.ui.components.AudioPlayerBar
import com.miadfm.podcasts.ui.components.PodcastArtworkThumbnail
import com.miadfm.podcasts.ui.components.PodcastPlayerSheet
import com.miadfm.podcasts.ui.components.PudiobookTopBar
import com.miadfm.podcasts.ui.i18n.LocalAppLanguage
import com.miadfm.podcasts.ui.i18n.appStrings
import com.miadfm.podcasts.ui.theme.BlueAccent
import com.miadfm.podcasts.ui.theme.BlueAccentLight
import com.miadfm.podcasts.ui.theme.BlueContainer
import com.miadfm.podcasts.ui.theme.CharcoalBlack
import com.miadfm.podcasts.ui.theme.CharcoalBorder
import com.miadfm.podcasts.ui.theme.CharcoalCard
import com.miadfm.podcasts.ui.theme.CharcoalDark
import com.miadfm.podcasts.ui.theme.CharcoalElevated
import com.miadfm.podcasts.ui.theme.ErrorRed
import com.miadfm.podcasts.ui.theme.ForestGreen
import com.miadfm.podcasts.ui.theme.TextMuted
import com.miadfm.podcasts.ui.theme.TextPrimary
import com.miadfm.podcasts.ui.theme.TextSecondary
import com.miadfm.podcasts.viewmodel.PodcastUiState
import java.util.Locale

private enum class PodcastFilter {
    ALL,
    AUDIOBOOKS,
    PODCASTS
}

@Composable
fun PodcastsScreen(
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
    val isPersian = LocalAppLanguage.current == AppLanguage.PERSIAN
    var showPlayerSheet by remember { mutableStateOf(false) }
    var currentFilter by remember { mutableStateOf(PodcastFilter.ALL) }

    val allEpisodes: List<Episode> = uiState.episodes
    val filteredEpisodes = remember(allEpisodes, currentFilter) {
        when (currentFilter) {
            PodcastFilter.ALL -> allEpisodes
            PodcastFilter.AUDIOBOOKS -> allEpisodes.filter { it.contentType.equals("Audiobook", ignoreCase = true) }
            PodcastFilter.PODCASTS -> allEpisodes.filter { it.contentType.equals("Podcast", ignoreCase = true) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PudiobookTopBar(
                subtitle = strings.podcastsTitle
            )

            // Filter Chips Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = currentFilter == PodcastFilter.ALL,
                        onClick = { currentFilter = PodcastFilter.ALL },
                        label = { Text("All (${allEpisodes.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlueContainer,
                            selectedLabelColor = ForestGreen,
                            containerColor = CharcoalDark,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (currentFilter == PodcastFilter.ALL) ForestGreen else CharcoalBorder),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == PodcastFilter.AUDIOBOOKS,
                        onClick = { currentFilter = PodcastFilter.AUDIOBOOKS },
                        label = { Text(if (isPersian) "کتاب‌های صوتی" else "Audiobooks") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlueContainer,
                            selectedLabelColor = ForestGreen,
                            containerColor = CharcoalDark,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (currentFilter == PodcastFilter.AUDIOBOOKS) ForestGreen else CharcoalBorder),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = currentFilter == PodcastFilter.PODCASTS,
                        onClick = { currentFilter = PodcastFilter.PODCASTS },
                        label = { Text(if (isPersian) "پادکست‌ها" else "Podcasts") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlueContainer,
                            selectedLabelColor = ForestGreen,
                            containerColor = CharcoalDark,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (currentFilter == PodcastFilter.PODCASTS) ForestGreen else CharcoalBorder),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Episode Cards List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("podcasts_list"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = if (uiState.playerState.currentEpisode != null) 100.dp else 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = filteredEpisodes,
                    key = { _, item -> item.id }
                ) { index, episode ->
                    val isCurrentPlaying = uiState.playerState.currentEpisode?.id == episode.id
                    val isPlaying = isCurrentPlaying && uiState.playerState.isPlaying

                    PodcastEpisodeCard(
                        episode = episode,
                        isCurrentPlaying = isCurrentPlaying,
                        isPlaying = isPlaying,
                        currentPositionMs = if (isCurrentPlaying) uiState.playerState.currentPositionMs else 0L,
                        durationMs = if (isCurrentPlaying && uiState.playerState.durationMs > 0) uiState.playerState.durationMs else (episode.durationSeconds.toLong() * 1000L),
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

@Composable
fun PodcastEpisodeCard(
    episode: Episode,
    isCurrentPlaying: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onCardClick: () -> Unit,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    testTagIndex: Int,
    modifier: Modifier = Modifier
) {
    val isPersian = LocalAppLanguage.current == AppLanguage.PERSIAN
    val title = if (isPersian) episode.titleFa else episode.title
    val creator = if (isPersian) (episode.creatorFa ?: episode.creator) else episode.creator
    val contentType = if (isPersian) episode.contentTypeFa else episode.contentType

    val progress = if (durationMs > 0 && currentPositionMs > 0) {
        (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onCardClick)
            .testTag("episode_item_$testTagIndex"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlaying) CharcoalElevated else CharcoalCard
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCurrentPlaying) ForestGreen else CharcoalBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Square Artwork with Rounded Corners
                PodcastArtworkThumbnail(
                    episode = episode,
                    size = 76.dp,
                    shape = RoundedCornerShape(14.dp),
                    isPlaying = isPlaying,
                    modifier = Modifier.testTag("episode_artwork_$testTagIndex")
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Text Hierarchy: Title (Prominent), Creator (Secondary), Duration (Small muted)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Content Type Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCurrentPlaying) BlueContainer else CharcoalDark,
                            border = BorderStroke(0.5.dp, CharcoalBorder)
                        ) {
                            Text(
                                text = contentType,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrentPlaying) ForestGreen else TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (isCurrentPlaying) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Active Playing",
                                tint = ForestGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title: Large and prominent
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Creator: Smaller secondary text
                    if (!creator.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = creator,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Duration: Small muted text
                    Text(
                        text = formatDurationMinutes(episode.durationSeconds.toLong(), isPersian),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Action buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Favorite button
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("episode_favorite_btn_$testTagIndex")
                    ) {
                        Icon(
                            imageVector = if (episode.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (episode.isFavorite) "Remove Favorite" else "Add Favorite",
                            tint = if (episode.isFavorite) ErrorRed else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play Button with round shape
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isCurrentPlaying) ForestGreen else CharcoalDark)
                            .border(1.dp, if (isCurrentPlaying) ForestGreen else CharcoalBorder, CircleShape)
                            .testTag("episode_play_btn_$testTagIndex")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = if (isCurrentPlaying) Color.White else TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Playback progress: Thin and subtle along bottom
            if (isCurrentPlaying && progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = ForestGreen,
                    trackColor = CharcoalBorder
                )
            }
        }
    }
}

private fun formatDurationMinutes(seconds: Long, isPersian: Boolean): String {
    val mins = seconds / 60
    val secs = seconds % 60
    val formatted = String.format(Locale.US, "%d:%02d", mins, secs)
    return if (isPersian) toPersianDigits(formatted) else formatted
}

private fun toPersianDigits(input: String): String {
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val builder = StringBuilder()
    for (char in input) {
        if (char in '0'..'9') {
            builder.append(persianDigits[char - '0'])
        } else {
            builder.append(char)
        }
    }
    return builder.toString()
}
