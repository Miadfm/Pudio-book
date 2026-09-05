package com.miadfm.podcasts.ui.vault

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.miadfm.podcasts.data.vault.VaultItemEntity
import com.miadfm.podcasts.ui.components.formatBytes
import com.miadfm.podcasts.ui.theme.BlueAccent
import com.miadfm.podcasts.ui.theme.BlueAccentLight
import com.miadfm.podcasts.ui.theme.BlueContainer
import com.miadfm.podcasts.ui.theme.CharcoalBorder
import com.miadfm.podcasts.ui.theme.CharcoalCard
import com.miadfm.podcasts.ui.theme.CharcoalDark
import com.miadfm.podcasts.ui.theme.CharcoalElevated
import com.miadfm.podcasts.ui.theme.ErrorRed
import com.miadfm.podcasts.ui.theme.TextMuted
import com.miadfm.podcasts.ui.theme.TextPrimary
import com.miadfm.podcasts.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.produceState

private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

@OptIn(UnstableApi::class)
@Composable
fun VaultAudioPlayerDialog(
    item: VaultItemEntity,
    dataSourceFactory: androidx.media3.datasource.DataSource.Factory? = null,
    mediaFile: File? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onClose: () -> Unit,
    onOpenExternal: () -> Unit,
    getThumbnail: (suspend () -> Bitmap?)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isBuffering by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPositionMs by remember { mutableLongStateOf(0L) }
    var showSpeedSelector by remember { mutableStateOf(false) }

    // Asynchronously loads album artwork without delaying player init or playback
    val artworkBitmap by produceState<Bitmap?>(initialValue = null, key1 = item.id) {
        if (getThumbnail != null) {
            value = getThumbnail()
        }
    }

    val exoPlayer = remember(item.id) {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
        val builder = ExoPlayer.Builder(context, renderersFactory)
        if (dataSourceFactory != null) {
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                .setDataSourceFactory(dataSourceFactory)
            val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 2500,
                    /* maxBufferMs = */ 15000,
                    /* bufferForPlaybackMs = */ 500,
                    /* bufferForPlaybackAfterRebufferMs = */ 1000
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
            builder.setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
        }

        val player = builder.build()
        val mediaUri = if (dataSourceFactory != null) {
            Uri.parse("vault://audio/${item.id}")
        } else if (mediaFile != null && mediaFile.exists()) {
            Uri.fromFile(mediaFile)
        } else {
            null
        }

        if (mediaUri != null) {
            val mediaItem = MediaItem.fromUri(mediaUri)
            player.setMediaItem(mediaItem)
            player.playbackParameters = PlaybackParameters(playbackSpeed)
            player.prepare()
            player.playWhenReady = true
            player
        } else {
            player.release()
            null
        }
    }

    // Player state listener and progress polling loop
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == Player.STATE_BUFFERING)
                if (state == Player.STATE_READY) {
                    val d = exoPlayer?.duration ?: 0L
                    if (d > 0) durationMs = d
                    playerError = null
                } else if (state == Player.STATE_ENDED) {
                    isPlaying = false
                    currentPositionMs = durationMs
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                isPlaying = false
                val desc = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                    PlaybackException.ERROR_CODE_DECODING_FAILED -> "Unsupported media format"
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                    PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "Unable to load media"
                    else -> "Playback failed: ${error.localizedMessage ?: "Unknown error"}"
                }
                playerError = desc
            }
        }

        exoPlayer?.addListener(listener)

        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.stop()
            exoPlayer?.release()
        }
    }

    // Polling tracker for position
    LaunchedEffect(exoPlayer, isPlaying, isDraggingSlider) {
        while (isActive && exoPlayer != null) {
            if (!isDraggingSlider) {
                currentPositionMs = exoPlayer.currentPosition
                val d = exoPlayer.duration
                if (d > 0) durationMs = d
            }
            delay(250L)
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .testTag("vault_audio_player_card"),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = BlueAccentLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = item.originalDisplayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatBytes(item.sizeBytes)} • AES-256 Vault Audio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenExternal,
                            modifier = Modifier.testTag("audio_player_open_external")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open with external app",
                                tint = BlueAccentLight
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("audio_player_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Error or Loading message
                    val displayError = errorMessage ?: playerError
                    if (displayError != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = displayError,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Large Decorative Vinyl / Visualizer art / Extracted Artwork
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(CharcoalElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading || isBuffering) {
                            CircularProgressIndicator(
                                color = BlueAccent,
                                modifier = Modifier.size(48.dp)
                            )
                        } else if (artworkBitmap != null) {
                            Image(
                                bitmap = artworkBitmap!!.asImageBitmap(),
                                contentDescription = "Album Artwork",
                                modifier = Modifier
                                    .size(122.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(BlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = BlueAccent,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Progress Slider
                    val effectiveDuration = if (durationMs > 0) durationMs else 1L
                    val sliderValue = if (isDraggingSlider) sliderPositionMs else currentPositionMs
                    val clampedPosition = sliderValue.coerceIn(0L, effectiveDuration)

                    Slider(
                        value = clampedPosition.toFloat(),
                        onValueChange = { pos ->
                            isDraggingSlider = true
                            sliderPositionMs = pos.toLong()
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            exoPlayer?.seekTo(sliderPositionMs)
                        },
                        valueRange = 0f..effectiveDuration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = BlueAccent,
                            activeTrackColor = BlueAccent,
                            inactiveTrackColor = CharcoalElevated
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("audio_player_seek_bar")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTimeMs(clampedPosition),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = formatTimeMs(durationMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Controls Row (Speed, Replay 10, Play/Pause, Forward 10)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback Speed Button
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showSpeedSelector = !showSpeedSelector }
                                .testTag("audio_player_speed_button"),
                            shape = RoundedCornerShape(16.dp),
                            color = CharcoalElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Playback Speed",
                                    tint = BlueAccentLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%.2fx", playbackSpeed).replace(".00", ".0").replace("0x", "x"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Skip Backward 10s
                        IconButton(
                            onClick = {
                                exoPlayer?.let { player ->
                                    val newPos = max(0L, player.currentPosition - 10000L)
                                    player.seekTo(newPos)
                                }
                            },
                            modifier = Modifier.size(48.dp).testTag("audio_player_skip_back")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Skip backward 10s",
                                tint = TextPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Primary Play / Pause button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(BlueAccent)
                                .clickable {
                                    exoPlayer?.let { player ->
                                        if (player.isPlaying) {
                                            player.pause()
                                        } else {
                                            if (player.playbackState == Player.STATE_ENDED) {
                                                player.seekTo(0)
                                            }
                                            player.play()
                                        }
                                    }
                                }
                                .testTag("audio_player_play_pause"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = TextPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Skip Forward 10s
                        IconButton(
                            onClick = {
                                exoPlayer?.let { player ->
                                    val target = min(durationMs, player.currentPosition + 10000L)
                                    player.seekTo(target)
                                }
                            },
                            modifier = Modifier.size(48.dp).testTag("audio_player_skip_forward")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Skip forward 10s",
                                tint = TextPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Speed Selector Chips Drawer
                    AnimatedVisibility(
                        visible = showSpeedSelector,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Playback Speed",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(PLAYBACK_SPEEDS) { speed ->
                                    val isSelected = (playbackSpeed == speed)
                                    val label = String.format(Locale.US, "%.2fx", speed)
                                        .replace(".00", ".0")
                                        .replace("0x", "x")

                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                playbackSpeed = speed
                                                exoPlayer?.playbackParameters = PlaybackParameters(speed)
                                            }
                                            .testTag("audio_speed_${speed}"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) BlueAccent else CharcoalElevated,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) BlueAccent else CharcoalBorder
                                        )
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatTimeMs(millis: Long): String {
    val totalSeconds = max(0L, millis / 1000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val hours = minutes / 60L
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
