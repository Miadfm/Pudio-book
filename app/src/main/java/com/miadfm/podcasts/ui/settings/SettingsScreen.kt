package com.miadfm.podcasts.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miadfm.podcasts.data.podcast.SamplePodcastDataSource
import com.miadfm.podcasts.data.settings.AppLanguage
import com.miadfm.podcasts.ui.components.PudiobookTopBar
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
import com.miadfm.podcasts.ui.theme.SuccessGreen
import com.miadfm.podcasts.ui.theme.TextMuted
import com.miadfm.podcasts.ui.theme.TextPrimary
import com.miadfm.podcasts.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onOpenPrivateStorage: () -> Unit,
    onClearTemporaryCache: ((onSuccess: () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = appStrings()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showConfirmClearCacheDialog by remember { mutableStateOf(false) }
    var showAttributionDialog by remember { mutableStateOf(false) }
    var showCacheClearedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(showCacheClearedMessage) {
        if (showCacheClearedMessage) {
            delay(3000)
            showCacheClearedMessage = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBlack)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            PudiobookTopBar(
                subtitle = strings.settingsTitle,
                trailingContent = {
                    IconButton(
                        onClick = onOpenPrivateStorage,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_vault_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = strings.securityManagePin,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            // Cache Cleared Success Feedback Banner
            if (showCacheClearedMessage) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("cache_cleared_feedback"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalElevated),
                    border = BorderStroke(1.dp, SuccessGreen)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = strings.clearCacheFeedback,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ============================================
            // GROUP 1: APPEARANCE
            // ============================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.appearanceTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = ForestGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { showLanguageDialog = true }
                    .testTag("language_settings_item"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                border = BorderStroke(1.dp, CharcoalBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BlueContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = strings.languageOptionTitle,
                                tint = ForestGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = strings.languageOptionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.PERSIAN) strings.persianLanguageName else strings.englishLanguageName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ForestGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = strings.selectLanguageDialogTitle,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ============================================
            // GROUP 2: STORAGE
            // ============================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.storageSectionTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = ForestGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { showConfirmClearCacheDialog = true }
                    .testTag("clear_temporary_cache_item"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                border = BorderStroke(1.dp, CharcoalBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CharcoalElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = strings.clearCacheTitle,
                                tint = ForestGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = strings.clearCacheTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.clearCacheSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Cached,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Subtle Content Credits & Legal Notice
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showAttributionDialog = true }
                    .testTag("attribution_settings_item"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalDark),
                border = BorderStroke(1.dp, CharcoalBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Copyright,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.creditsAndAttributionTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = CharcoalDark,
            title = {
                Text(
                    text = strings.selectLanguageDialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // English
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectLanguage(AppLanguage.ENGLISH)
                                showLanguageDialog = false
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .testTag("language_option_english"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == AppLanguage.ENGLISH,
                            onClick = {
                                onSelectLanguage(AppLanguage.ENGLISH)
                                showLanguageDialog = false
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = ForestGreen,
                                unselectedColor = TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.englishLanguageName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (currentLanguage == AppLanguage.ENGLISH) ForestGreen else TextPrimary,
                            fontWeight = if (currentLanguage == AppLanguage.ENGLISH) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    HorizontalDivider(color = CharcoalBorderSubtle)

                    // Persian (فارسی)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectLanguage(AppLanguage.PERSIAN)
                                showLanguageDialog = false
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .testTag("language_option_persian"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == AppLanguage.PERSIAN,
                            onClick = {
                                onSelectLanguage(AppLanguage.PERSIAN)
                                showLanguageDialog = false
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = ForestGreen,
                                unselectedColor = TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.persianLanguageName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (currentLanguage == AppLanguage.PERSIAN) ForestGreen else TextPrimary,
                            fontWeight = if (currentLanguage == AppLanguage.PERSIAN) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLanguageDialog = false },
                    modifier = Modifier.testTag("language_dialog_cancel")
                ) {
                    Text(
                        text = strings.cancel,
                        color = ForestGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    // Confirmation Dialog for Clearing Temporary Cache
    if (showConfirmClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearCacheDialog = false },
            containerColor = CharcoalDark,
            title = {
                Text(
                    text = strings.clearCacheDialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = strings.clearCacheDialogMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClearCacheDialog = false
                        onClearTemporaryCache?.invoke {
                            showCacheClearedMessage = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    modifier = Modifier.testTag("confirm_clear_cache_button")
                ) {
                    Text(
                        text = strings.clear,
                        color = CharcoalBlack,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmClearCacheDialog = false },
                    modifier = Modifier.testTag("cancel_clear_cache_button")
                ) {
                    Text(
                        text = strings.cancel,
                        color = TextSecondary
                    )
                }
            }
        )
    }

    // Audio & Content Attribution Dialog
    if (showAttributionDialog) {
        val podcast = SamplePodcastDataSource.samplePodcast
        AlertDialog(
            onDismissRequest = { showAttributionDialog = false },
            containerColor = CharcoalDark,
            title = {
                Text(
                    text = strings.attributionCredits,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = strings.legalComplianceNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val attr = podcast.attribution
                    if (attr != null) {
                        Text(
                            text = attr.sourceName,
                            style = MaterialTheme.typography.titleMedium,
                            color = ForestGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = attr.licenseName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        if (!attr.licenseUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = attr.licenseUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = BlueAccentLight
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAttributionDialog = false }
                ) {
                    Text(
                        text = strings.close,
                        color = ForestGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}
