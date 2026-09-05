package com.miadfm.podcasts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miadfm.podcasts.data.podcast.PodcastHistoryManager
import com.miadfm.podcasts.data.security.CryptoManager
import com.miadfm.podcasts.data.security.PinSecurityManager
import com.miadfm.podcasts.data.settings.AppLanguageManager
import com.miadfm.podcasts.data.vault.VaultDatabase
import com.miadfm.podcasts.data.vault.VaultRepository
import com.miadfm.podcasts.player.PodcastAudioPlayer
import com.miadfm.podcasts.ui.MainNavHost
import com.miadfm.podcasts.ui.theme.PodcastsTheme
import com.miadfm.podcasts.viewmodel.PodcastViewModel
import com.miadfm.podcasts.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {

    private lateinit var vaultViewModel: VaultViewModel
    private lateinit var podcastViewModel: PodcastViewModel
    private lateinit var audioPlayer: PodcastAudioPlayer
    private lateinit var languageManager: AppLanguageManager
    private lateinit var historyManager: PodcastHistoryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize Data & Security layers
        val database = VaultDatabase.getDatabase(applicationContext)
        val cryptoManager = CryptoManager(applicationContext)
        val pinSecurityManager = PinSecurityManager(applicationContext)
        val vaultRepository = VaultRepository(applicationContext, database.vaultDao(), cryptoManager)
        audioPlayer = PodcastAudioPlayer(applicationContext)
        languageManager = AppLanguageManager(applicationContext)
        historyManager = PodcastHistoryManager(applicationContext)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(PodcastViewModel::class.java) -> {
                        PodcastViewModel(vaultRepository, audioPlayer, languageManager, historyManager) as T
                    }
                    modelClass.isAssignableFrom(VaultViewModel::class.java) -> {
                        VaultViewModel(vaultRepository, pinSecurityManager, languageManager) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }

        podcastViewModel = ViewModelProvider(this, factory)[PodcastViewModel::class.java]
        vaultViewModel = ViewModelProvider(this, factory)[VaultViewModel::class.java]

        setContent {
            val podcastUiState by podcastViewModel.uiState.collectAsStateWithLifecycle()
            PodcastsTheme(language = podcastUiState.language) {
                MainNavHost(
                    podcastViewModel = podcastViewModel,
                    vaultViewModel = vaultViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::vaultViewModel.isInitialized) {
            vaultViewModel.onAppResume()
        }
    }

    override fun onStop() {
        super.onStop()
        // When the user exits the app or navigates away, lock Vault immediately
        if (::vaultViewModel.isInitialized) {
            vaultViewModel.lockVault()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::audioPlayer.isInitialized) {
            audioPlayer.release()
        }
    }
}
