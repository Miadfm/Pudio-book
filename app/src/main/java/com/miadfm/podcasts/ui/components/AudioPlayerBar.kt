package com.miadfm.podcasts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.miadfm.podcasts.player.PodcastAudioPlayer
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miadfm.podcasts.data.settings.AppLanguage
import com.miadfm.podcasts.player.PlayerState
import com.miadfm.podcasts.ui.i18n.LocalAppLanguage
import com.miadfm.podcasts.ui.i18n.appStrings
import com.miadfm.podcasts.ui.theme.BlueAccent
import com.miadfm.podcasts.ui.theme.BlueAccentLight
import com.miadfm.podcasts.ui.theme.CharcoalBorder
import com.miadfm.podcasts.ui.theme.CharcoalCard
import com.miadfm.podcasts.ui.theme.CharcoalElevated
import com.miadfm.podcasts.ui.theme.TextMuted
import com.miadfm.podcasts.ui.theme.TextPrimary
import com.miadfm.podcasts.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AudioPlayerBar(
    playerState: PlayerState,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    modifier: Modifier = Modifier,
    onSpeedChange: (Float) -> Unit = {},
    onOpenPlayer: () -> Unit = {}
) {
    val episode = playerState.currentEpisode ?: return
    val strings = appStrings()
    val isPersian = LocalAppLanguage.current == AppLanguage.PERSIAN
    val episodeTitle = if (isPersian) episode.titleFa else episode.title

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragSliderValue by remember { mutableFloatStateOf(0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val currentPosition = if (isDraggingSlider) {
        (dragSliderValue * playerState.durationMs).toLong()
    } else {
        playerState.currentPositionMs
    }

    val sliderProgress = if (playerState.durationMs > 0) {
        if (isDraggingSlider) dragSliderValue else (playerState.currentPositionMs.toFloat() / playerState.durationMs).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clickable(onClick = onOpenPlayer)
            .testTag("audio_player_bar"),
        shape = RoundedCornerShape(24.dp),
        color = CharcoalCard,
        tonalElevation = 12.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, CharcoalBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Row: Thumbnail + Title + Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Artwork Thumbnail & Info
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Real Embedded Artwork or Podcast Specific Artwork
                    PodcastArtworkThumbnail(
                        episode = episode,
                        size = 46.dp,
                        shape = RoundedCornerShape(12.dp),
                        isPlaying = playerState.isPlaying,
                        contentDescription = episodeTitle,
                        modifier = Modifier.testTag("bar_artwork_thumbnail")
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = strings.nowPlaying,
                                style = MaterialTheme.typography.labelSmall,
                                color = BlueAccentLight,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (playerState.playbackSpeed != 1.0f) BlueAccent else CharcoalElevated,
                                border = BorderStroke(1.dp, if (playerState.playbackSpeed != 1.0f) BlueAccentLight else CharcoalBorder),
                                modifier = Modifier
                                    .clickable { showSpeedDialog = true }
                                    .testTag("bar_playback_speed_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = strings.playbackSpeed,
                                        tint = if (playerState.playbackSpeed != 1.0f) Color.White else BlueAccentLight,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${playerState.playbackSpeed}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (playerState.playbackSpeed != 1.0f) Color.White else BlueAccentLight,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = episodeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right side: Player Controls (Backward 10s, Play/Pause, Forward 10s)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Backward 10 seconds
                    IconButton(
                        onClick = onSkipBackward,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("skip_backward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = strings.skipBackward10,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Play / Pause
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(BlueAccent)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) strings.pause else strings.play,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Forward 10 seconds
                    IconButton(
                        onClick = onSkipForward,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("skip_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = strings.skipForward10,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Seek Slider
            Slider(
                value = sliderProgress,
                onValueChange = { newValue ->
                    isDraggingSlider = true
                    dragSliderValue = newValue
                },
                onValueChangeFinished = {
                    val targetMs = (dragSliderValue * playerState.durationMs).toLong()
                    onSeekTo(targetMs)
                    isDraggingSlider = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .testTag("player_seek_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = BlueAccentLight,
                    activeTrackColor = BlueAccent,
                    inactiveTrackColor = CharcoalElevated
                )
            )

            // Timestamps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMillis(currentPosition, isPersian),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    ),
                    color = TextSecondary,
                    modifier = Modifier.testTag("player_current_time")
                )
                Text(
                    text = formatMillis(playerState.durationMs, isPersian),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    ),
                    color = TextMuted,
                    modifier = Modifier.testTag("player_total_duration")
                )
            }
        }
    }

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = BlueAccentLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.playbackSpeed,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = listOf(
                        listOf(0.5f, 0.75f, 1.0f),
                        listOf(1.25f, 1.5f, 1.75f),
                        listOf(2.0f, 2.5f, 3.0f)
                    )
                    for (rowSpeeds in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (speed in rowSpeeds) {
                                val isSelected = (playerState.playbackSpeed == speed)
                                val speedLabel = if (speed == speed.toInt().toFloat()) "${speed.toInt()}x" else "${speed}x"
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) BlueAccent else CharcoalElevated,
                                    border = BorderStroke(1.dp, if (isSelected) BlueAccentLight else CharcoalBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onSpeedChange(speed)
                                            showSpeedDialog = false
                                        }
                                        .testTag("bar_speed_option_$speedLabel")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = speedLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSpeedDialog = false },
                    modifier = Modifier.testTag("speed_dialog_close")
                ) {
                    Text(text = strings.cancel, color = BlueAccentLight)
                }
            },
            containerColor = CharcoalCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

private fun formatMillis(ms: Long, isPersian: Boolean = false): String {
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
