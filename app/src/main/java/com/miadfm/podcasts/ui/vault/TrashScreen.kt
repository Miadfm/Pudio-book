package com.miadfm.podcasts.ui.vault

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miadfm.podcasts.data.vault.DecryptedNote
import com.miadfm.podcasts.data.vault.VaultContentType
import com.miadfm.podcasts.data.vault.VaultItemEntity
import com.miadfm.podcasts.ui.components.EmptyStateView
import com.miadfm.podcasts.ui.components.formatBytes
import com.miadfm.podcasts.ui.theme.BlueAccent
import com.miadfm.podcasts.ui.theme.CharcoalBorder
import com.miadfm.podcasts.ui.theme.CharcoalCard
import com.miadfm.podcasts.ui.theme.CharcoalDark
import com.miadfm.podcasts.ui.theme.CharcoalElevated
import com.miadfm.podcasts.ui.theme.ErrorRed
import com.miadfm.podcasts.ui.theme.TextMuted
import com.miadfm.podcasts.ui.theme.TextPrimary
import com.miadfm.podcasts.ui.theme.TextSecondary
import java.util.concurrent.TimeUnit

@Composable
fun TrashScreen(
    trashedItems: List<VaultItemEntity>,
    trashedNotes: List<DecryptedNote>,
    onRestoreItem: (VaultItemEntity) -> Unit,
    onDeletePermanentlyItem: (VaultItemEntity) -> Unit,
    onRestoreNote: (String) -> Unit,
    onDeletePermanentlyNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = trashedItems.size + trashedNotes.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Trash Policy Info Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = BlueAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Items in Trash are automatically deleted permanently after 30 days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (totalCount == 0) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    icon = Icons.Default.DeleteForever,
                    title = "Trash is Empty",
                    message = "Items you delete will stay here for 30 days before permanent removal."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("trash_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Trashed Files
                items(trashedItems, key = { it.id }) { item ->
                    val daysRemaining = calculateDaysRemaining(item.trashedTimestamp)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CharcoalElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when (item.type) {
                                    VaultContentType.IMAGE.name -> Icons.Default.Image
                                    VaultContentType.VIDEO.name -> Icons.Default.Videocam
                                    else -> Icons.Default.Audiotrack
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = BlueAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.originalDisplayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${formatBytes(item.sizeBytes)} • Deletes in $daysRemaining days",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }

                            // Restore
                            IconButton(
                                onClick = { onRestoreItem(item) },
                                modifier = Modifier.testTag("restore_item_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = "Restore Item",
                                    tint = BlueAccent
                                )
                            }

                            // Delete Permanently
                            IconButton(
                                onClick = { onDeletePermanentlyItem(item) },
                                modifier = Modifier.testTag("delete_permanently_item_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Delete Permanently",
                                    tint = ErrorRed
                                )
                            }
                        }
                    }
                }

                // Trashed Notes
                items(trashedNotes, key = { it.id }) { note ->
                    val daysRemaining = calculateDaysRemaining(note.trashedTimestamp)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CharcoalElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = BlueAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Note • Deletes in $daysRemaining days",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }

                            // Restore
                            IconButton(
                                onClick = { onRestoreNote(note.id) },
                                modifier = Modifier.testTag("restore_note_${note.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = "Restore Note",
                                    tint = BlueAccent
                                )
                            }

                            // Delete Permanently
                            IconButton(
                                onClick = { onDeletePermanentlyNote(note.id) },
                                modifier = Modifier.testTag("delete_permanently_note_${note.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Delete Permanently",
                                    tint = ErrorRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateDaysRemaining(trashedTimestamp: Long?): Int {
    if (trashedTimestamp == null) return 30
    val elapsedMs = System.currentTimeMillis() - trashedTimestamp
    val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsedMs)
    return (30 - elapsedDays).coerceIn(0, 30).toInt()
}
