package com.miadfm.podcasts.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.miadfm.podcasts.ui.theme.CharcoalBorderSubtle
import com.miadfm.podcasts.ui.theme.CharcoalCard
import com.miadfm.podcasts.ui.theme.CharcoalDark
import com.miadfm.podcasts.ui.theme.CharcoalElevated
import com.miadfm.podcasts.ui.theme.ForestGreen
import com.miadfm.podcasts.ui.theme.TextMuted
import com.miadfm.podcasts.ui.theme.TextPrimary
import com.miadfm.podcasts.ui.theme.TextSecondary
import com.miadfm.podcasts.viewmodel.PodcastUiState
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: PodcastUiState,
    onNavigateToPodcasts: () -> Unit,
    onNavigateToVault: () -> Unit,
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

    val currentEpisode = uiState.playerState.currentEpisode
    val isPlaying = uiState.playerState.isPlaying

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PudiobookTopBar(
                subtitle = strings.discoverAndListen
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_dashboard_scroll"),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 16.dp,
                    bottom = if (currentEpisode != null) 100.dp else 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Dashboard Welcome & Identity
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BlueContainer,
                            border = BorderStroke(1.dp, CharcoalBorder)
                        ) {
                            Text(
                                text = if (isPersian) "حریم خصوصی و رسانه" else "PRIVATE MEDIA & AUDIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = ForestGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isPersian) "رسانه‌ها و پادکست‌های شما" else "Your Media & Audiobooks",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isPersian) "پخش صوتی آفلاین و گاوصندوق رمزگذاری‌شده خصوصی" else "Offline audio player & hardware-backed private vault",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Quick Access Section Header
                item {
                    Text(
                        text = strings.quickAccess,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Quick Access Cards: Podcasts & Vault
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quick Access: Podcasts
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(onClick = onNavigateToPodcasts)
                                .testTag("quick_access_podcasts"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                            border = BorderStroke(1.dp, CharcoalBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BlueContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Headphones,
                                        contentDescription = null,
                                        tint = ForestGreen,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.explorePodcasts,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = strings.explorePodcastsSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Quick Access: Private Vault
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(onClick = onNavigateToVault)
                                .testTag("quick_access_vault"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                            border = BorderStroke(1.dp, CharcoalBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BlueContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = ForestGreen,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.openVaultAction,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = strings.openVaultActionSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Section: Continue Listening / Recently Accessed Media
                item {
                    Text(
                        text = if (currentEpisode != null) strings.continueListening else strings.recentEpisodes,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    if (currentEpisode != null) {
                        // Continue Listening Card
                        val durationMs = if (uiState.playerState.durationMs > 0) uiState.playerState.durationMs else (currentEpisode.durationSeconds * 1000L)
                        val progress = if (durationMs > 0) {
                            (uiState.playerState.currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                        } else 0f

                        val episodeTitle = if (isPersian) currentEpisode.titleFa else currentEpisode.title
                        val episodeCreator = if (isPersian) (currentEpisode.creatorFa ?: currentEpisode.creator) else currentEpisode.creator

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showPlayerSheet = true }
                                .testTag("continue_listening_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CharcoalElevated),
                            border = BorderStroke(1.dp, ForestGreen),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PodcastArtworkThumbnail(
                                        episode = currentEpisode,
                                        size = 68.dp,
                                        shape = RoundedCornerShape(14.dp),
                                        isPlaying = isPlaying,
                                        modifier = Modifier.testTag("continue_listening_artwork")
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = BlueContainer
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.GraphicEq,
                                                    contentDescription = null,
                                                    tint = ForestGreen,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = strings.nowPlaying,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ForestGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = episodeTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (!episodeCreator.isNullOrBlank()) {
                                            Text(
                                                text = episodeCreator,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = onTogglePlayPause,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(ForestGreen)
                                            .testTag("continue_listening_play_pause")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) strings.pause else strings.play,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                if (progress > 0f) {
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
                    } else {
                        // Clean Empty State when no recent media exists
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_empty_recent_media"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                            border = BorderStroke(1.dp, CharcoalBorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(CharcoalDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Headphones,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = strings.noRecentMedia,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = strings.noRecentMediaSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = onNavigateToPodcasts,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ForestGreen,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = strings.explorePodcasts,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Docked audio player bar if active
        AnimatedVisibility(
            visible = currentEpisode != null,
            modifier = Modifier.align(Alignment.BottomCenter)
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

        // Full-screen player sheet
        if (showPlayerSheet && currentEpisode != null) {
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
