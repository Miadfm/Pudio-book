package com.miadfm.podcasts.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.miadfm.podcasts.data.vault.UnhideSummary
import com.miadfm.podcasts.ui.theme.BlueAccent
import com.miadfm.podcasts.ui.theme.CharcoalBorder
import com.miadfm.podcasts.ui.theme.CharcoalCard
import com.miadfm.podcasts.ui.theme.CharcoalDark
import com.miadfm.podcasts.ui.theme.CharcoalElevated
import com.miadfm.podcasts.ui.theme.ErrorRed
import com.miadfm.podcasts.ui.theme.SuccessGreen
import com.miadfm.podcasts.ui.theme.TextMuted
import com.miadfm.podcasts.ui.theme.TextPrimary
import com.miadfm.podcasts.ui.theme.TextSecondary
import com.miadfm.podcasts.ui.theme.WarningAmber

@Composable
fun UnhideProgressDialog(
    progressText: String,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("unhide_progress_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = CharcoalDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = BlueAccent,
                    trackColor = CharcoalElevated
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Restoring...",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Move out of Vault",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = progressText.ifBlank { "Decrypting and verifying selected files..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun UnhideSummaryDialog(
    summary: UnhideSummary,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleText = when {
        summary.failureCount == 0 -> "Restore completed"
        summary.successCount == 0 -> "Restore failed"
        else -> "Restore completed with issues"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("unhide_summary_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = CharcoalDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (summary.failureCount == 0) SuccessGreen.copy(alpha = 0.2f)
                                else if (summary.successCount > 0) WarningAmber.copy(alpha = 0.2f)
                                else ErrorRed.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (summary.failureCount == 0) Icons.Default.CheckCircle
                            else if (summary.successCount > 0) Icons.Default.Warning
                            else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (summary.failureCount == 0) SuccessGreen
                            else if (summary.successCount > 0) WarningAmber
                            else ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Moved out of Vault",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CharcoalCard)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Restored",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${summary.successCount}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("unhide_success_count")
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CharcoalCard)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Failed",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${summary.failureCount}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (summary.failureCount > 0) ErrorRed else TextMuted,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("unhide_failure_count")
                            )
                        }
                    }
                }

                // Failed Files List (if any)
                if (summary.failureCount > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Failed Files:",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val failedList = summary.results.filter { !it.isSuccess }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(failedList) { result ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = CharcoalCard)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = ErrorRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = result.fileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = result.errorReason ?: "Unable to restore selected content",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ErrorRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("unhide_summary_done_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent)
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
