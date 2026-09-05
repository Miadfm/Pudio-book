package com.miadfm.podcasts.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.miadfm.podcasts.data.podcast.Episode
import com.miadfm.podcasts.data.podcast.PodcastArtworkManager

@Composable
fun PodcastArtworkThumbnail(
    episode: Episode,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shape: Shape = RoundedCornerShape(14.dp),
    isPlaying: Boolean = false,
    contentDescription: String? = null
) {
    val context = LocalContext.current

    // Asynchronously load embedded artwork if available; does not block UI or playback
    val artworkBitmap by produceState<Bitmap?>(
        initialValue = PodcastArtworkManager.getCachedArtwork(episode),
        key1 = episode.id,
        key2 = episode.assetPath
    ) {
        value = PodcastArtworkManager.loadArtwork(context, episode)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        if (artworkBitmap != null) {
            // Priority 1: Real embedded audio artwork
            Image(
                bitmap = artworkBitmap!!.asImageBitmap(),
                contentDescription = contentDescription ?: episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Priority 2: Clean built-in default podcast artwork
            PodcastSpecificArtwork(
                episode = episode,
                size = size,
                contentDescription = contentDescription ?: episode.title
            )
        }

        // Active playing indicator overlay
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "Playing",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.45f)
                )
            }
        }
    }
}

/**
 * Renders the clean built-in default podcast artwork cover when embedded metadata artwork is not present.
 */
@Composable
fun PodcastSpecificArtwork(
    episode: Episode,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    contentDescription: String? = null
) {
    val (gradientColors, icon) = getArtworkVisualsForEpisode(episode)
    val iconSize = (size * 0.42f).coerceAtLeast(24.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        // Subtle circular glass-like backing for aesthetic depth and contrast
        Box(
            modifier = Modifier
                .size(iconSize * 1.55f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

private fun getArtworkVisualsForEpisode(episode: Episode): Pair<List<Color>, ImageVector> {
    return when (episode.id) {
        // Item 1: شبهای روشن / White Nights (Audiobook) - Deep twilight indigo & night sky
        "ep_1" -> Pair(
            listOf(Color(0xFF1A237E), Color(0xFF311B92), Color(0xFF4A148C)),
            Icons.Default.AutoStories
        )
        // Item 2: رادیو عجایب / Radio Ajaeb (Podcast) - Mystery crimson & obsidian
        "ep_2" -> Pair(
            listOf(Color(0xFF880E4F), Color(0xFF4A0033), Color(0xFF1E0018)),
            Icons.Default.Radio
        )
        // Item 3: تابستان تاریک — قسمت اول / Dark Summer - Noir charcoal & burnt amber
        "ep_3" -> Pair(
            listOf(Color(0xFFBF360C), Color(0xFF4E1D0E), Color(0xFF1A1A1A)),
            Icons.Default.Podcasts
        )
        // Item 4: رقص خطرناک / Dangerous Dance (Podcast) - Electric violet & deep rhythm
        "ep_4" -> Pair(
            listOf(Color(0xFF7B1FA2), Color(0xFF4527A0), Color(0xFF1A237E)),
            Icons.Default.MusicNote
        )
        // Item 5: آخرین شیفت تارا / Tara's Last Shift (Podcast) - Nocturnal cyan & dark slate
        "ep_5" -> Pair(
            listOf(Color(0xFF006064), Color(0xFF00363A), Color(0xFF121B22)),
            Icons.Default.Schedule
        )
        // Fallback default
        else -> Pair(
            listOf(Color(0xFF1E3A8A), Color(0xFF0F172A)),
            Icons.Default.Headphones
        )
    }
}
