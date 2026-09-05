package com.miadfm.podcasts.player

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.miadfm.podcasts.data.podcast.Episode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val currentEpisode: Episode? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f
)

class PodcastAudioPlayer(private val context: Context) {
    companion object {
        val SUPPORTED_SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private fun getOrCreatePlayer(): ExoPlayer {
        val existing = exoPlayer
        if (existing != null) return existing

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val player = ExoPlayer.Builder(context, renderersFactory).build().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .setUsage(C.USAGE_MEDIA)
                .build()
            setAudioAttributes(audioAttributes, true)

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            val dur = if (duration > 0) duration else (_playerState.value.currentEpisode?.durationSeconds?.toLong()?.times(1000L) ?: 0L)
                            _playerState.value = _playerState.value.copy(
                                durationMs = dur,
                                isPlaying = isPlaying
                            )
                        }
                        Player.STATE_ENDED -> {
                            _playerState.value = _playerState.value.copy(
                                isPlaying = false,
                                currentPositionMs = 0L
                            )
                            stopProgressTracker()
                        }
                        Player.STATE_IDLE -> {
                            _playerState.value = _playerState.value.copy(isPlaying = false)
                        }
                        Player.STATE_BUFFERING -> {}
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
                    if (isPlaying) {
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("PodcastAudioPlayer", "Playback error occurred: ${error.message}", error)
                    _playerState.value = _playerState.value.copy(isPlaying = false)
                    stopProgressTracker()
                }

                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                    _playerState.value = _playerState.value.copy(playbackSpeed = playbackParameters.speed)
                }
            })
        }
        exoPlayer = player
        return player
    }

    /**
     * Checks if the episode audio is available (either from custom local import or packaged project asset).
     */
    fun isEpisodeAvailable(episode: Episode): Boolean {
        val localFile = File(context.filesDir, "podcasts/${episode.id}.mp3")
        if (localFile.exists() && localFile.length() > 0) return true

        if (episode.assetPath.isNotBlank()) {
            val cleanPath = episode.assetPath.trimStart('/')
            try {
                context.assets.open(cleanPath).use {
                    return true
                }
            } catch (e: Exception) {
                Log.w("PodcastAudioPlayer", "Asset not found: $cleanPath", e)
            }
        }
        return false
    }

    /**
     * Stores an imported audio file in normal podcast storage (unencrypted, separate from Vault).
     */
    fun saveImportedAudio(episodeId: String, inputStream: java.io.InputStream): Boolean {
        return try {
            val podcastDir = File(context.filesDir, "podcasts")
            if (!podcastDir.exists()) {
                podcastDir.mkdirs()
            }
            val targetFile = File(podcastDir, "$episodeId.mp3")
            targetFile.outputStream().use { out ->
                inputStream.copyTo(out)
            }
            true
        } catch (e: Exception) {
            Log.e("PodcastAudioPlayer", "Failed to save imported podcast audio for $episodeId", e)
            false
        }
    }

    fun playEpisode(episode: Episode) {
        val player = getOrCreatePlayer()

        if (_playerState.value.currentEpisode?.id == episode.id) {
            if (!_playerState.value.isPlaying) {
                play()
            }
            return
        }

        try {
            val mediaItem = buildMediaItemForEpisode(episode)
            if (mediaItem == null) {
                Log.e("PodcastAudioPlayer", "No valid media asset found for episode: ${episode.title}")
                return
            }

            _playerState.value = _playerState.value.copy(
                currentEpisode = episode,
                currentPositionMs = 0L,
                durationMs = episode.durationSeconds * 1000L,
                isPlaying = false
            )

            // Maintain current playback speed if user selected one
            val currentSpeed = _playerState.value.playbackSpeed
            if (currentSpeed != 1.0f) {
                player.playbackParameters = PlaybackParameters(currentSpeed)
            }

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            startProgressTracker()
        } catch (e: Exception) {
            Log.e("PodcastAudioPlayer", "Error starting episode playback: ${episode.title}", e)
        }
    }

    private fun buildMediaItemForEpisode(episode: Episode): MediaItem? {
        // 1. Check if user imported custom local file on device
        val localFile = File(context.filesDir, "podcasts/${episode.id}.mp3")
        if (localFile.exists() && localFile.length() > 0) {
            return MediaItem.fromUri(Uri.fromFile(localFile))
        }

        // 2. Play packaged project asset
        if (episode.assetPath.isNotBlank()) {
            val cleanPath = episode.assetPath.trimStart('/')
            try {
                context.assets.open(cleanPath).use {
                    return MediaItem.fromUri(Uri.parse("asset:///$cleanPath"))
                }
            } catch (e: Exception) {
                Log.e("PodcastAudioPlayer", "Asset not found in application package at: $cleanPath", e)
            }
        }
        return null
    }

    fun play() {
        val player = exoPlayer ?: run {
            _playerState.value.currentEpisode?.let { playEpisode(it) }
            return
        }
        player.playbackParameters = PlaybackParameters(_playerState.value.playbackSpeed)
        player.play()
        startProgressTracker()
    }

    fun pause() {
        exoPlayer?.pause()
        stopProgressTracker()
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val clamped = positionMs.coerceIn(0L, _playerState.value.durationMs.coerceAtLeast(1L))
        player.seekTo(clamped)
        _playerState.value = _playerState.value.copy(currentPositionMs = clamped)
    }

    /**
     * Backward 10 seconds. If current position is less than 10 seconds, seeks to 0.
     */
    fun skipBackward(seconds: Int = 10) {
        val current = _playerState.value.currentPositionMs
        val target = (current - (seconds * 1000L)).coerceAtLeast(0L)
        seekTo(target)
    }

    /**
     * Forward 10 seconds. If remaining duration is less than 10 seconds, seeks to end.
     */
    fun skipForward(seconds: Int = 10) {
        val current = _playerState.value.currentPositionMs
        val duration = _playerState.value.durationMs
        val target = (current + (seconds * 1000L)).coerceAtMost(duration)
        seekTo(target)
    }

    /**
     * Sets playback speed smoothly without restarting playback and preserving current position.
     */
    fun setPlaybackSpeed(speed: Float) {
        val validSpeed = speed.coerceIn(0.5f, 3.0f)
        _playerState.value = _playerState.value.copy(playbackSpeed = validSpeed)
        exoPlayer?.playbackParameters = PlaybackParameters(validSpeed)
    }

    fun stop() {
        stopProgressTracker()
        exoPlayer?.stop()
        _playerState.value = _playerState.value.copy(isPlaying = false)
    }

    fun release() {
        stopProgressTracker()
        exoPlayer?.release()
        exoPlayer = null
        _playerState.value = PlayerState()
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        val current = player.currentPosition.coerceAtLeast(0L)
                        val total = if (player.duration > 0) player.duration else _playerState.value.durationMs
                        _playerState.value = _playerState.value.copy(
                            currentPositionMs = current,
                            durationMs = total,
                            isPlaying = true
                        )
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }
}
