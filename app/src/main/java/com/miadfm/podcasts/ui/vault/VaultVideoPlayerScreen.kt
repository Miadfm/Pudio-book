package com.miadfm.podcasts.ui.vault

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.miadfm.podcasts.data.vault.VaultItemEntity
import com.miadfm.podcasts.ui.components.formatBytes
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

private var _rotateRightIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
val VideoRotateRightIcon: androidx.compose.ui.graphics.vector.ImageVector
    get() {
        if (_rotateRightIcon != null) return _rotateRightIcon!!
        _rotateRightIcon = androidx.compose.ui.graphics.vector.ImageVector.Builder(
            name = "RotateRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            fill = androidx.compose.ui.graphics.SolidColor(Color.White),
            pathData = listOf(
                androidx.compose.ui.graphics.vector.PathNode.MoveTo(15.55f, 5.55f),
                androidx.compose.ui.graphics.vector.PathNode.LineTo(11f, 1f),
                androidx.compose.ui.graphics.vector.PathNode.VerticalTo(4.07f),
                androidx.compose.ui.graphics.vector.PathNode.CurveTo(7.06f, 4.56f, 4f, 7.92f, 4f, 12f),
                androidx.compose.ui.graphics.vector.PathNode.CurveTo(4f, 16.08f, 7.05f, 19.44f, 11f, 19.93f),
                androidx.compose.ui.graphics.vector.PathNode.VerticalTo(17.91f),
                androidx.compose.ui.graphics.vector.PathNode.CurveTo(8.16f, 17.43f, 6f, 14.97f, 6f, 12f),
                androidx.compose.ui.graphics.vector.PathNode.CurveTo(6f, 8.69f, 8.69f, 6f, 12f, 6f),
                androidx.compose.ui.graphics.vector.PathNode.VerticalTo(9.07f),
                androidx.compose.ui.graphics.vector.PathNode.LineTo(15.55f, 5.55f),
                androidx.compose.ui.graphics.vector.PathNode.Close
            )
        ).build()
        return _rotateRightIcon!!
    }

private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

@OptIn(UnstableApi::class)
@Composable
fun VaultVideoPlayerScreen(
    item: VaultItemEntity,
    dataSourceFactory: androidx.media3.datasource.DataSource.Factory? = null,
    mediaFile: File? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onClose: () -> Unit,
    onOpenExternal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isBuffering by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPositionMs by remember { mutableLongStateOf(0L) }
    var showSpeedSelector by remember { mutableStateOf(false) }
    var isFullScreenMode by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Viewing-only Transformation States (Zoom, Pan, Rotate)
    var videoRotationDegrees by remember { mutableFloatStateOf(0f) }
    var videoScale by remember { mutableFloatStateOf(1f) }
    var videoOffset by remember { mutableStateOf(Offset.Zero) }

    // Reset transformations when switching items
    LaunchedEffect(item.id) {
        videoRotationDegrees = 0f
        videoScale = 1f
        videoOffset = Offset.Zero
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
            Uri.parse("vault://video/${item.id}")
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
                    areControlsVisible = true
                    currentPositionMs = durationMs
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                isPlaying = false
                areControlsVisible = true
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

    // Auto-hide controls timer
    LaunchedEffect(areControlsVisible, isPlaying, lastInteractionTime, showSpeedSelector) {
        if (areControlsVisible && isPlaying && !showSpeedSelector) {
            delay(3500L)
            areControlsVisible = false
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

    fun markInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        areControlsVisible = true
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("vault_video_player_screen")
        ) {
            // Interactive Video Surface with Zoom, Pan, and Rotation (Non-destructive viewing transformations)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(item.id) {
                        detectTapGestures(
                            onTap = {
                                areControlsVisible = !areControlsVisible
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            onDoubleTap = { tapOffset ->
                                markInteraction()
                                if (videoScale > 1.2f) {
                                    videoScale = 1f
                                    videoOffset = Offset.Zero
                                } else {
                                    videoScale = 2.0f
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    videoOffset = Offset(
                                        x = (centerX - tapOffset.x) * 1.0f,
                                        y = (centerY - tapOffset.y) * 1.0f
                                    )
                                }
                            }
                        )
                    }
                    .pointerInput(item.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (videoScale * zoom).coerceIn(1f, 3f)
                            videoScale = newScale
                            if (newScale > 1f) {
                                val currentMaxX = (size.width * (newScale - 1f) / 2f).coerceAtLeast(0f)
                                val currentMaxY = (size.height * (newScale - 1f) / 2f).coerceAtLeast(0f)
                                videoOffset = Offset(
                                    x = (videoOffset.x + pan.x).coerceIn(-currentMaxX, currentMaxX),
                                    y = (videoOffset.y + pan.y).coerceIn(-currentMaxY, currentMaxY)
                                )
                            } else {
                                videoOffset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (exoPlayer != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = videoScale
                                scaleY = videoScale
                                rotationZ = videoRotationDegrees
                                translationX = videoOffset.x
                                translationY = videoOffset.y
                            }
                    )
                }
            }

            val displayError = errorMessage ?: playerError

            // Buffering Spinner or Loading
            if ((isLoading || isBuffering) && displayError == null) {
                CircularProgressIndicator(
                    color = BlueAccent,
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.Center)
                )
            }

            // Error display banner
            if (displayError != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = displayError,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Please check file integrity or use 'Open with...' for external apps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Interactive Controls Overlay (Fades out automatically)
            AnimatedVisibility(
                visible = areControlsVisible || displayError != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.75f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.testTag("video_player_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
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
                                    text = "${formatBytes(item.sizeBytes)} • AES-256 Encrypted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BlueAccentLight
                                )
                            }
                        }

                        // Zoom reset indicator badge if zoomed
                        if (videoScale > 1.05f) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        markInteraction()
                                        videoScale = 1f
                                        videoOffset = Offset.Zero
                                    }
                                    .testTag("video_zoom_reset_badge"),
                                shape = RoundedCornerShape(12.dp),
                                color = BlueAccent.copy(alpha = 0.85f)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1fx Reset", videoScale),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Rotate Video Button (90 degrees per tap - viewing only)
                        IconButton(
                            onClick = {
                                markInteraction()
                                videoRotationDegrees = (videoRotationDegrees + 90f) % 360f
                            },
                            modifier = Modifier.testTag("video_player_rotate_button")
                        ) {
                            Icon(
                                imageVector = VideoRotateRightIcon,
                                contentDescription = "Rotate Video 90°",
                                tint = BlueAccentLight
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Playback Speed Button
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    markInteraction()
                                    showSpeedSelector = !showSpeedSelector
                                }
                                .testTag("video_player_speed_button"),
                            shape = RoundedCornerShape(16.dp),
                            color = CharcoalElevated.copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = BlueAccentLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%.2fx", playbackSpeed)
                                        .replace(".00", ".0")
                                        .replace("0x", "x"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Open with external app
                        IconButton(
                            onClick = {
                                markInteraction()
                                onOpenExternal()
                            },
                            modifier = Modifier.testTag("video_player_open_external")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open with external player",
                                tint = BlueAccentLight
                            )
                        }
                    }

                    // Center Playback Controls (Replay 10, Play/Pause, Forward 10)
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Skip back 10s
                        IconButton(
                            onClick = {
                                markInteraction()
                                exoPlayer?.let { player ->
                                    val target = max(0L, player.currentPosition - 10000L)
                                    player.seekTo(target)
                                }
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .testTag("video_player_skip_back")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Skip back 10s",
                                tint = TextPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Play/Pause Big Button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(BlueAccent)
                                .clickable {
                                    markInteraction()
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
                                .testTag("video_player_play_pause"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = TextPrimary,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        // Skip forward 10s
                        IconButton(
                            onClick = {
                                markInteraction()
                                exoPlayer?.let { player ->
                                    val target = min(durationMs, player.currentPosition + 10000L)
                                    player.seekTo(target)
                                }
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .testTag("video_player_skip_forward")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Skip forward 10s",
                                tint = TextPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Bottom Bar (Progress slider, time stamps, fullscreen toggle)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {
                        // Playback Speed Drawer (if open)
                        AnimatedVisibility(
                            visible = showSpeedSelector,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = CharcoalDark.copy(alpha = 0.95f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Playback Speed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(PLAYBACK_SPEEDS) { speed ->
                                            val isSelected = (playbackSpeed == speed)
                                            val label = String.format(Locale.US, "%.2fx", speed)
                                                .replace(".00", ".0")
                                                .replace("0x", "x")

                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        markInteraction()
                                                        playbackSpeed = speed
                                                        exoPlayer?.playbackParameters = PlaybackParameters(speed)
                                                    }
                                                    .testTag("video_speed_${speed}"),
                                                shape = RoundedCornerShape(10.dp),
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

                        // Progress Slider
                        val effectiveDuration = if (durationMs > 0) durationMs else 1L
                        val sliderValue = if (isDraggingSlider) sliderPositionMs else currentPositionMs
                        val clampedPosition = sliderValue.coerceIn(0L, effectiveDuration)

                        Slider(
                            value = clampedPosition.toFloat(),
                            onValueChange = { pos ->
                                markInteraction()
                                isDraggingSlider = true
                                sliderPositionMs = pos.toLong()
                            },
                            onValueChangeFinished = {
                                markInteraction()
                                isDraggingSlider = false
                                exoPlayer?.seekTo(sliderPositionMs)
                            },
                            valueRange = 0f..effectiveDuration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = BlueAccent,
                                activeTrackColor = BlueAccent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("video_player_seek_bar")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatTimeMs(clampedPosition)} / ${formatTimeMs(durationMs)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )

                            IconButton(
                                onClick = {
                                    markInteraction()
                                    isFullScreenMode = !isFullScreenMode
                                },
                                modifier = Modifier.size(36.dp).testTag("video_fullscreen_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isFullScreenMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Toggle Fullscreen",
                                    tint = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
