package com.miadfm.podcasts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miadfm.podcasts.data.podcast.Episode
import com.miadfm.podcasts.data.settings.AppLanguage
import com.miadfm.podcasts.player.PlayerState
import com.miadfm.podcasts.player.PodcastAudioPlayer
import com.miadfm.podcasts.ui.i18n.LocalAppLanguage
import com.miadfm.podcasts.ui.i18n.appStrings
import com.miadfm.podcasts.ui.theme.BlueAccent
import com.miadfm.podcasts.ui.theme.BlueAccentLight
import com.miadfm.podcasts.ui.theme.CharcoalBorder
import com.miadfm.podcasts.ui.theme.CharcoalCard
import com.miadfm.podcasts.ui.theme.CharcoalDark
import com.miadfm.podcasts.ui.theme.CharcoalElevated
import com.miadfm.podcasts.ui.theme.ErrorRed
import com.miadfm.podcasts.ui.theme.TextMuted
import com.miadfm.podcasts.ui.theme.TextPrimary
import com.miadfm.podcasts.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastPlayerSheet(
    playerState: PlayerState,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleFavorite: (Episode) -> Unit
) {
    val episode = playerState.currentEpisode ?: return
    val strings = appStrings()
    val isPersian = LocalAppLanguage.current == AppLanguage.PERSIAN
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val title = if (isPersian) episode.titleFa else episode.title
    val creator = if (isPersian) (episode.creatorFa ?: episode.creator) else episode.creator
    val contentType = if (isPersian) episode.contentTypeFa else episode.contentType

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderValue by remember { mutableFloatStateOf(0f) }

    val currentPosition = if (isDraggingSlider) {
        (dragSliderValue * playerState.durationMs).toLong()
    } else {
        playerState.currentPositionMs
    }

    val sliderProgress = if (playerState.durationMs > 0) {
        if (isDraggingSlider) dragSliderValue else (playerState.currentPositionMs.toFloat() / playerState.durationMs).coerceIn(0f, 1f)
    } else 0f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CharcoalDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextMuted.copy(alpha = 0.4f))
            )
        },
        modifier = Modifier.testTag("podcast_player_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp).testTag("close_player_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }

                Text(
                    text = strings.nowPlaying,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { onToggleFavorite(episode) },
                    modifier = Modifier.size(48.dp).testTag("player_toggle_favorite_button")
                ) {
                    Icon(
                        imageVector = if (episode.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (episode.isFavorite) strings.removeFavorite else strings.addFavorite,
                        tint = if (episode.isFavorite) ErrorRed else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Artwork Thumbnail (Priority 1: Embedded MP3 artwork, Priority 2: Podcast-specific cover)
            PodcastArtworkThumbnail(
                episode = episode,
                size = 230.dp,
                shape = RoundedCornerShape(24.dp),
                isPlaying = playerState.isPlaying,
                modifier = Modifier.testTag("player_large_artwork")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Type badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CharcoalCard,
                border = BorderStroke(1.dp, CharcoalBorder)
            ) {
                Text(
                    text = contentType,
                    style = MaterialTheme.typography.labelMedium,
                    color = BlueAccentLight,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Episode Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!creator.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = creator,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progress Slider
            Slider(
                value = sliderProgress,
                onValueChange = {
                    isDraggingSlider = true
                    dragSliderValue = it
                },
                onValueChangeFinished = {
                    val targetMs = (dragSliderValue * playerState.durationMs).toLong()
                    onSeekTo(targetMs)
                    isDraggingSlider = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_progress_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = BlueAccent,
                    activeTrackColor = BlueAccent,
                    inactiveTrackColor = CharcoalElevated
                )
            )

            // Timestamps: Current Time & Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(currentPosition, isPersian),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = formatMs(playerState.durationMs, isPersian),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Controls: -10s, Play/Pause, +10s
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Backward 10 seconds
                IconButton(
                    onClick = onSkipBackward,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("player_backward_10_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = strings.skipBackward10,
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Play / Pause FAB
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(BlueAccent)
                        .testTag("player_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) strings.pause else strings.play,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Forward 10 seconds
                IconButton(
                    onClick = onSkipForward,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("player_forward_10_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = strings.skipForward10,
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Playback Speed Selector (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 1.75x, 2.0x, 2.5x, 3.0x)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = BlueAccentLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.playbackSpeed,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${playerState.playbackSpeed}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = BlueAccentLight,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal row of speed chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PodcastAudioPlayer.SUPPORTED_SPEEDS.forEach { speed ->
                    val isSelected = (playerState.playbackSpeed == speed)
                    val speedLabel = if (speed == speed.toInt().toFloat()) {
                        "${speed.toInt()}x"
                    } else {
                        "${speed}x"
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { onSpeedChange(speed) },
                        label = {
                            Text(
                                text = speedLabel,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CharcoalCard,
                            labelColor = TextSecondary,
                            selectedContainerColor = BlueAccent,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = CharcoalBorder,
                            selectedBorderColor = BlueAccentLight
                        ),
                        modifier = Modifier.testTag("speed_chip_$speedLabel")
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Long, isPersian: Boolean): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    val str = if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
    return if (isPersian) toPersianDigits(str) else str
}

private fun toPersianDigits(str: String): String {
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val sb = StringBuilder()
    for (ch in str) {
        if (ch in '0'..'9') {
            sb.append(persianDigits[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}
