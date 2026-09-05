package com.miadfm.podcasts.ui.vault

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.miadfm.podcasts.data.vault.DecryptedNote
import com.miadfm.podcasts.data.vault.VaultFolderEntity
import com.miadfm.podcasts.ui.theme.BlueAccent
import com.miadfm.podcasts.ui.theme.CharcoalBlack
import com.miadfm.podcasts.ui.theme.CharcoalBorder
import com.miadfm.podcasts.ui.theme.CharcoalCard
import com.miadfm.podcasts.ui.theme.CharcoalDark
import com.miadfm.podcasts.ui.theme.ErrorRed
import com.miadfm.podcasts.ui.theme.TextPrimary
import com.miadfm.podcasts.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    note: DecryptedNote?,
    isCreating: Boolean,
    folders: List<VaultFolderEntity>,
    selectedFolderId: String?,
    onSave: (title: String, content: String, noteId: String?, folderId: String?) -> Unit,
    onClose: () -> Unit,
    onMoveToTrash: ((String) -> Unit)? = null,
    onDeletePermanently: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var title by remember(note) { mutableStateOf(note?.title ?: "") }
    var content by remember(note) { mutableStateOf(note?.content ?: "") }
    var folderId by remember(note) { mutableStateOf(note?.folderId ?: selectedFolderId ?: folders.firstOrNull()?.id) }
    var showFolderMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("note_editor_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_note_editor_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (isCreating) "New Note" else "Edit Note",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    // Folder selection
                    if (folders.isNotEmpty()) {
                        Box {
                            IconButton(
                                onClick = { showFolderMenu = true },
                                modifier = Modifier.testTag("note_folder_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DriveFileMove,
                                    contentDescription = "Folder",
                                    tint = TextPrimary
                                )
                            }

                            DropdownMenu(
                                expanded = showFolderMenu,
                                onDismissRequest = { showFolderMenu = false }
                            ) {
                                folders.forEach { folder ->
                                    DropdownMenuItem(
                                        text = { Text(folder.name) },
                                        onClick = {
                                            folderId = folder.id
                                            showFolderMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (!isCreating && note != null) {
                        onMoveToTrash?.let { trashAction ->
                            IconButton(
                                onClick = {
                                    trashAction(note.id)
                                    onClose()
                                },
                                modifier = Modifier.testTag("trash_note_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Move to Trash",
                                    tint = TextPrimary
                                )
                            }
                        }

                        onDeletePermanently?.let { deleteAction ->
                            IconButton(
                                onClick = {
                                    deleteAction(note.id)
                                    onClose()
                                },
                                modifier = Modifier.testTag("delete_note_permanently_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Delete Permanently",
                                    tint = ErrorRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            val finalTitle = title.trim().ifEmpty { "Untitled Note" }
                            onSave(finalTitle, content, note?.id, folderId)
                        },
                        modifier = Modifier.testTag("save_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Note",
                            tint = BlueAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Note Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CharcoalCard,
                        unfocusedContainerColor = CharcoalCard,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = CharcoalBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Note Content Area
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Write your encrypted note here...", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("note_content_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CharcoalCard,
                        unfocusedContainerColor = CharcoalCard,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = CharcoalBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Save button
                Button(
                    onClick = {
                        val finalTitle = title.trim().ifEmpty { "Untitled Note" }
                        onSave(finalTitle, content, note?.id, folderId)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("bottom_save_note_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent)
                ) {
                    Text(
                        text = "Save Note (AES-256-GCM Encrypted)",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
