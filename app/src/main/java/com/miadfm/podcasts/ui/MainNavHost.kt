package com.miadfm.podcasts.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miadfm.podcasts.ui.favorites.FavoritesScreen
import com.miadfm.podcasts.ui.i18n.appStrings
import com.miadfm.podcasts.ui.podcasts.PodcastsScreen
import com.miadfm.podcasts.ui.recent.RecentScreen
import com.miadfm.podcasts.ui.settings.SettingsScreen
import com.miadfm.podcasts.ui.theme.BlueAccentPill
import com.miadfm.podcasts.ui.theme.CharcoalBlack
import com.miadfm.podcasts.ui.theme.ForestGreen
import com.miadfm.podcasts.ui.theme.TextMuted
import com.miadfm.podcasts.ui.vault.PinAuthScreen
import com.miadfm.podcasts.ui.vault.VaultScreen
import com.miadfm.podcasts.viewmodel.PodcastViewModel
import com.miadfm.podcasts.viewmodel.VaultViewModel

enum class MainTab {
    RECENT,
    PODCASTS,
    FAVORITES,
    SETTINGS
}

@Composable
fun MainNavHost(
    podcastViewModel: PodcastViewModel,
    vaultViewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val podcastUiState by podcastViewModel.uiState.collectAsStateWithLifecycle()
    val vaultUiState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val strings = appStrings()

    var currentTab by remember { mutableStateOf(MainTab.RECENT) }
    var isVaultOpen by remember { mutableStateOf(false) }

    if (isVaultOpen) {
        BackHandler {
            vaultViewModel.lockVault()
            isVaultOpen = false
        }

        Box(modifier = modifier.fillMaxSize().background(CharcoalBlack)) {
            if (!vaultUiState.isUnlocked) {
                PinAuthScreen(
                    pinMode = vaultUiState.pinMode,
                    isPinSet = vaultUiState.isPinSet,
                    errorMessage = vaultUiState.pinError,
                    onSubmitPin = { pin ->
                        vaultViewModel.submitPin(pin)
                    },
                    onBack = {
                        vaultViewModel.lockVault()
                        isVaultOpen = false
                    }
                )
            } else {
                VaultScreen(
                    viewModel = vaultViewModel,
                    uiState = vaultUiState,
                    onExitVault = {
                        vaultViewModel.lockVault()
                        isVaultOpen = false
                    }
                )
            }
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = CharcoalBlack,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("main_bottom_navigation")
                ) {
                    // 1. Recent
                    NavigationBarItem(
                        selected = currentTab == MainTab.RECENT,
                        onClick = { currentTab = MainTab.RECENT },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = strings.navRecent,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = strings.navRecent,
                                fontWeight = if (currentTab == MainTab.RECENT) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreen,
                            selectedTextColor = ForestGreen,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = BlueAccentPill
                        ),
                        modifier = Modifier.testTag("nav_tab_recent")
                    )

                    // 2. Podcasts
                    NavigationBarItem(
                        selected = currentTab == MainTab.PODCASTS,
                        onClick = { currentTab = MainTab.PODCASTS },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = strings.navPodcasts,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = strings.navPodcasts,
                                fontWeight = if (currentTab == MainTab.PODCASTS) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreen,
                            selectedTextColor = ForestGreen,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = BlueAccentPill
                        ),
                        modifier = Modifier.testTag("nav_tab_podcasts")
                    )

                    // 3. Favorites
                    NavigationBarItem(
                        selected = currentTab == MainTab.FAVORITES,
                        onClick = { currentTab = MainTab.FAVORITES },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = strings.navFavorites,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = strings.navFavorites,
                                fontWeight = if (currentTab == MainTab.FAVORITES) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreen,
                            selectedTextColor = ForestGreen,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = BlueAccentPill
                        ),
                        modifier = Modifier.testTag("nav_tab_favorites")
                    )

                    // 4. Settings
                    NavigationBarItem(
                        selected = currentTab == MainTab.SETTINGS,
                        onClick = { currentTab = MainTab.SETTINGS },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = strings.navSettings,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = strings.navSettings,
                                fontWeight = if (currentTab == MainTab.SETTINGS) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreen,
                            selectedTextColor = ForestGreen,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = BlueAccentPill
                        ),
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "MainTabAnimation"
                ) { targetTab ->
                    when (targetTab) {
                        MainTab.RECENT -> {
                            RecentScreen(
                                uiState = podcastUiState,
                                onPlayEpisode = { podcastViewModel.playEpisode(it) },
                                onTogglePlayPause = { podcastViewModel.togglePlayPause() },
                                onSeekTo = { podcastViewModel.seekTo(it) },
                                onSkipBackward = { podcastViewModel.skipBackward() },
                                onSkipForward = { podcastViewModel.skipForward() },
                                onToggleFavorite = { podcastViewModel.toggleFavorite(it) },
                                onSpeedChange = { podcastViewModel.setPlaybackSpeed(it) }
                            )
                        }

                        MainTab.PODCASTS -> {
                            PodcastsScreen(
                                uiState = podcastUiState,
                                onPlayEpisode = { podcastViewModel.playEpisode(it) },
                                onTogglePlayPause = { podcastViewModel.togglePlayPause() },
                                onSeekTo = { podcastViewModel.seekTo(it) },
                                onSkipBackward = { podcastViewModel.skipBackward() },
                                onSkipForward = { podcastViewModel.skipForward() },
                                onToggleFavorite = { podcastViewModel.toggleFavorite(it) },
                                onSpeedChange = { podcastViewModel.setPlaybackSpeed(it) }
                            )
                        }

                        MainTab.FAVORITES -> {
                            FavoritesScreen(
                                uiState = podcastUiState,
                                onPlayEpisode = { podcastViewModel.playEpisode(it) },
                                onTogglePlayPause = { podcastViewModel.togglePlayPause() },
                                onSeekTo = { podcastViewModel.seekTo(it) },
                                onSkipBackward = { podcastViewModel.skipBackward() },
                                onSkipForward = { podcastViewModel.skipForward() },
                                onToggleFavorite = { podcastViewModel.toggleFavorite(it) },
                                onSpeedChange = { podcastViewModel.setPlaybackSpeed(it) }
                            )
                        }

                        MainTab.SETTINGS -> {
                            SettingsScreen(
                                currentLanguage = podcastUiState.language,
                                onSelectLanguage = { newLang ->
                                    podcastViewModel.setLanguage(newLang)
                                },
                                onOpenPrivateStorage = {
                                    vaultViewModel.refreshPinStatus()
                                    isVaultOpen = true
                                },
                                onClearTemporaryCache = { onSuccess ->
                                    vaultViewModel.clearTemporaryCache(onSuccess)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
