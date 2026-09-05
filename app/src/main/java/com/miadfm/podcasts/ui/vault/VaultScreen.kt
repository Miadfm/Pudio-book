package com.miadfm.podcasts.ui.vault

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miadfm.podcasts.data.settings.AppLanguage
import com.miadfm.podcasts.data.vault.DecryptedNote
import com.miadfm.podcasts.data.vault.VaultContentType
import com.miadfm.podcasts.data.vault.VaultFolderEntity
import com.miadfm.podcasts.data.vault.VaultItemEntity
import com.miadfm.podcasts.ui.components.EmptyStateView
import com.miadfm.podcasts.ui.components.NotificationBanner
import com.miadfm.podcasts.ui.components.formatBytes
import com.miadfm.podcasts.ui.i18n.LocalAppLanguage
import com.miadfm.podcasts.ui.i18n.LocalAppStrings
import com.miadfm.podcasts.ui.theme.BlueAccent
import com.miadfm.podcasts.ui.theme.BlueAccentLight
import com.miadfm.podcasts.ui.theme.BlueContainer
import com.miadfm.podcasts.ui.theme.CharcoalBlack
import com.miadfm.podcasts.ui.theme.CharcoalBorder
import com.miadfm.podcasts.ui.theme.CharcoalCard
import com.miadfm.podcasts.ui.theme.CharcoalDark
import com.miadfm.podcasts.ui.theme.CharcoalElevated
import com.miadfm.podcasts.ui.theme.ErrorRed
import com.miadfm.podcasts.ui.theme.ForestGreen
import com.miadfm.podcasts.ui.theme.ForestGreenLight
import com.miadfm.podcasts.ui.theme.ForestGreenContainer
import com.miadfm.podcasts.ui.theme.TextMuted
import com.miadfm.podcasts.ui.theme.TextPrimary
import com.miadfm.podcasts.ui.theme.TextSecondary
import com.miadfm.podcasts.viewmodel.VaultTab
import com.miadfm.podcasts.viewmodel.VaultUiState
import com.miadfm.podcasts.viewmodel.VaultViewModel

@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    uiState: VaultUiState,
    onExitVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val isPersian = LocalAppLanguage.current == AppLanguage.PERSIAN
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var folderToRename by remember { mutableStateOf<VaultFolderEntity?>(null) }
    var renameFolderName by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<VaultFolderEntity?>(null) }
    var showTopMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.ensureDefaultFolder(isPersian)
    }

    // Intercept back actions to guarantee proper hierarchy
    BackHandler {
        when {
            uiState.viewingImageBitmap != null -> viewModel.closeImageViewer()
            uiState.activeEditingNote != null || uiState.isCreatingNote -> viewModel.closeNoteEditor()
            uiState.isMultiSelectMode -> viewModel.clearItemSelection()
            uiState.selectedTab == VaultTab.TRASH -> viewModel.selectTab(VaultTab.IMAGES)
            uiState.selectedFolderId != null -> viewModel.selectFolder(null)
            else -> {
                viewModel.lockVault()
                onExitVault()
            }
        }
    }

    // Unhide SAF Destination Picker
    val unhideFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.executeUnhide(uri)
        } else {
            viewModel.cancelPendingUnhide()
        }
    }

    // Media Pickers - always bind to the currently open real folder
    val photoVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val destinationFolderId = uiState.selectedFolderId ?: uiState.folders.firstOrNull()?.id
            destinationFolderId?.let { fId ->
                viewModel.importFiles(uris, folderId = fId)
            }
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val destinationFolderId = uiState.selectedFolderId ?: uiState.folders.firstOrNull()?.id
            destinationFolderId?.let { fId ->
                viewModel.importFiles(uris, folderId = fId)
            }
        }
    }

    val currentFolder = uiState.folders.find { it.id == uiState.selectedFolderId }

    // Filter items and notes by category & current open folder
    val filteredItems = if (uiState.selectedFolderId != null) {
        uiState.items.filter { item ->
            val matchesCategory = when (uiState.selectedTab) {
                VaultTab.IMAGES -> item.type == VaultContentType.IMAGE.name
                VaultTab.VIDEOS -> item.type == VaultContentType.VIDEO.name
                VaultTab.AUDIO -> item.type == VaultContentType.AUDIO.name
                else -> false
            }
            matchesCategory && item.folderId == uiState.selectedFolderId
        }
    } else {
        emptyList()
    }

    val filteredNotes = if (uiState.selectedFolderId != null) {
        uiState.notes.filter { note ->
            note.folderId == uiState.selectedFolderId
        }
    } else {
        emptyList()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.selectedTab == VaultTab.TRASH) {
            // --- Trash Screen View ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.selectTab(VaultTab.IMAGES) },
                        modifier = Modifier.testTag("vault_trash_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.tabTrash,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                TrashScreen(
                    trashedItems = uiState.trashedItems,
                    trashedNotes = uiState.trashedNotes,
                    onRestoreItem = { viewModel.restoreItem(it) },
                    onDeletePermanentlyItem = { viewModel.deleteItemPermanently(it) },
                    onRestoreNote = { viewModel.restoreNote(it) },
                    onDeletePermanentlyNote = { viewModel.deleteNotePermanently(it) }
                )
            }
        } else if (uiState.selectedFolderId == null) {
            // --- Vault Folder Selection Screen (Shown First Upon Every Entry) ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.lockVault()
                            onExitVault()
                        },
                        modifier = Modifier.testTag("vault_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Vault",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ForestGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Vault",
                            tint = ForestGreenLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.vaultTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.vaultEncryptedStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreenLight
                        )
                    }

                    // Trash button
                    IconButton(
                        onClick = { viewModel.selectTab(VaultTab.TRASH) },
                        modifier = Modifier.testTag("vault_trash_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = strings.tabTrash,
                            tint = TextSecondary
                        )
                    }

                    // Lock Vault button
                    IconButton(
                        onClick = {
                            viewModel.lockVault()
                            onExitVault()
                        },
                        modifier = Modifier.testTag("lock_vault_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = TextPrimary
                        )
                    }

                    // Options Menu
                    Box {
                        IconButton(
                            onClick = { showTopMenu = true },
                            modifier = Modifier.testTag("vault_top_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = TextPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.newFolder) },
                                leadingIcon = {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = ForestGreenLight)
                                },
                                onClick = {
                                    showTopMenu = false
                                    showNewFolderDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Change PIN") },
                                leadingIcon = {
                                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = ForestGreenLight)
                                },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.startChangePin()
                                }
                            )
                        }
                    }
                }

                // Notification Banner
                NotificationBanner(
                    message = uiState.userNotification,
                    onDismiss = { viewModel.clearNotification() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Folders Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPersian) "پوشه‌ها" else "Folders",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = { showNewFolderDialog = true },
                        modifier = Modifier.testTag("add_folder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = ForestGreenLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.newFolder,
                            color = ForestGreenLight,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Folders Grid or Empty State
                if (uiState.folders.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.CreateNewFolder,
                        title = strings.newFolder,
                        message = "Tap '+ New Folder' to create your first vault folder."
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("vault_folders_grid"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.folders, key = { it.id }) { folder ->
                            val itemCount = uiState.items.count { it.folderId == folder.id && !it.isTrashed } +
                                    uiState.notes.count { it.folderId == folder.id && !it.isTrashed }
                            VaultFolderCard(
                                folder = folder,
                                itemCount = itemCount,
                                onClick = { viewModel.selectFolder(folder.id) },
                                onRename = {
                                    folderToRename = folder
                                    renameFolderName = folder.name
                                },
                                onDelete = if (uiState.folders.size > 1) {
                                    { folderToDelete = folder }
                                } else null
                            )
                        }
                    }
                }
            }

            // Floating Action Button on Folder Selection screen
            FloatingActionButton(
                onClick = { showNewFolderDialog = true },
                containerColor = ForestGreen,
                contentColor = TextPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("create_folder_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = strings.newFolder,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            // --- Inside Folder Screen (Shows ONLY this folder's encrypted items) ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                if (uiState.isMultiSelectMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.clearItemSelection() },
                            modifier = Modifier.testTag("exit_multi_select_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Selection",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${uiState.selectedItemIds.size} Selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        // Select / Deselect All
                        val allSelected = filteredItems.isNotEmpty() && filteredItems.all { uiState.selectedItemIds.contains(it.id) }
                        IconButton(
                            onClick = {
                                if (allSelected) {
                                    viewModel.clearItemSelection()
                                } else {
                                    viewModel.selectAllCurrentItems(filteredItems)
                                }
                            },
                            modifier = Modifier.testTag("select_all_button")
                        ) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.CheckCircle else Icons.Default.SelectAll,
                                contentDescription = if (allSelected) "Deselect All" else "Select All",
                                tint = ForestGreenLight
                            )
                        }

                        // Unhide Selected Action
                        IconButton(
                            onClick = {
                                viewModel.prepareMultiItemUnhide {
                                    unhideFolderPickerLauncher.launch(null)
                                }
                            },
                            enabled = uiState.selectedItemIds.isNotEmpty(),
                            modifier = Modifier.testTag("multi_unhide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DriveFileMove,
                                contentDescription = "Unhide Selected",
                                tint = if (uiState.selectedItemIds.isNotEmpty()) ForestGreenLight else TextMuted
                            )
                        }

                        // Trash Selected Action
                        IconButton(
                            onClick = { viewModel.batchMoveSelectedToTrash() },
                            enabled = uiState.selectedItemIds.isNotEmpty(),
                            modifier = Modifier.testTag("multi_trash_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Move Selected to Trash",
                                tint = if (uiState.selectedItemIds.isNotEmpty()) TextPrimary else TextMuted
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.selectFolder(null) },
                            modifier = Modifier.testTag("vault_folder_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Folders",
                                tint = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ForestGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = ForestGreenLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentFolder?.name ?: "Folder",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val totalCount = uiState.items.count { it.folderId == uiState.selectedFolderId && !it.isTrashed } +
                                    uiState.notes.count { it.folderId == uiState.selectedFolderId && !it.isTrashed }
                            Text(
                                text = "$totalCount items",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        // Multi-Select Toggle Button
                        if (uiState.selectedTab != VaultTab.NOTES && filteredItems.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.toggleMultiSelectMode(true) },
                                modifier = Modifier.testTag("enter_multi_select_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircleOutline,
                                    contentDescription = "Select Items",
                                    tint = TextSecondary
                                )
                            }
                        }

                        // Options menu for current folder (Rename, Delete)
                        Box {
                            IconButton(
                                onClick = { showTopMenu = true },
                                modifier = Modifier.testTag("vault_top_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = TextPrimary
                                )
                            }

                            DropdownMenu(
                                expanded = showTopMenu,
                                onDismissRequest = { showTopMenu = false }
                            ) {
                                currentFolder?.let { f ->
                                    DropdownMenuItem(
                                        text = { Text(strings.renameFolderTitle) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = ForestGreenLight)
                                        },
                                        onClick = {
                                            showTopMenu = false
                                            folderToRename = f
                                            renameFolderName = f.name
                                        }
                                    )
                                    if (uiState.folders.size > 1) {
                                        DropdownMenuItem(
                                            text = { Text(strings.deleteFolderTitle) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed)
                                            },
                                            onClick = {
                                                showTopMenu = false
                                                folderToDelete = f
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Lock button
                        IconButton(
                            onClick = {
                                viewModel.lockVault()
                                onExitVault()
                            },
                            modifier = Modifier.testTag("lock_vault_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Vault",
                                tint = TextPrimary
                            )
                        }
                    }
                }

                // Notification Banner
                NotificationBanner(
                    message = uiState.userNotification,
                    onDismiss = { viewModel.clearNotification() }
                )

                // Category Tabs: Images, Videos, Audio, Notes
                ScrollableTabRow(
                    selectedTabIndex = when (uiState.selectedTab) {
                        VaultTab.IMAGES -> 0
                        VaultTab.VIDEOS -> 1
                        VaultTab.AUDIO -> 2
                        VaultTab.NOTES -> 3
                        VaultTab.TRASH -> 0
                    },
                    containerColor = Color.Transparent,
                    contentColor = ForestGreenLight,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        val tabIdx = when (uiState.selectedTab) {
                            VaultTab.IMAGES -> 0
                            VaultTab.VIDEOS -> 1
                            VaultTab.AUDIO -> 2
                            VaultTab.NOTES -> 3
                            VaultTab.TRASH -> 0
                        }
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[tabIdx]),
                            color = ForestGreenLight
                        )
                    },
                    divider = {}
                ) {
                    val folderTabs = listOf(VaultTab.IMAGES, VaultTab.VIDEOS, VaultTab.AUDIO, VaultTab.NOTES)
                    folderTabs.forEach { tab ->
                        val isSelected = uiState.selectedTab == tab
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(tab) },
                            text = {
                                Text(
                                    text = when (tab) {
                                        VaultTab.IMAGES -> strings.tabImages
                                        VaultTab.VIDEOS -> strings.tabVideos
                                        VaultTab.AUDIO -> strings.tabAudio
                                        VaultTab.NOTES -> strings.tabNotes
                                        else -> ""
                                    },
                                    color = if (isSelected) ForestGreenLight else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("vault_tab_${tab.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Main Content Body inside folder
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (uiState.selectedTab) {
                        VaultTab.IMAGES -> {
                            if (filteredItems.isEmpty()) {
                                EmptyStateView(
                                    icon = Icons.Default.FolderOpen,
                                    title = strings.emptyFolderTitle,
                                    message = strings.emptyFolderSubtitle
                                )
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 108.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("vault_images_grid"),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredItems, key = { it.id }) { item ->
                                        VaultImageGridCard(
                                            item = item,
                                            folders = uiState.folders,
                                            isMultiSelectMode = uiState.isMultiSelectMode,
                                            isSelected = uiState.selectedItemIds.contains(item.id),
                                            onToggleSelect = { viewModel.toggleItemSelection(item.id) },
                                            onUnhide = {
                                                viewModel.prepareSingleItemUnhide(item) {
                                                    unhideFolderPickerLauncher.launch(null)
                                                }
                                            },
                                            onClick = { viewModel.viewImage(item) },
                                            onMoveToTrash = { viewModel.moveItemToTrash(item) },
                                            onDeletePermanently = { viewModel.deleteItemPermanently(item) },
                                            onSetFolder = { id, fId -> viewModel.setItemFolder(id, fId) },
                                            getThumbnail = { viewModel.getThumbnail(item) }
                                        )
                                    }
                                }
                            }
                        }

                        VaultTab.VIDEOS -> {
                            if (filteredItems.isEmpty()) {
                                EmptyStateView(
                                    icon = Icons.Default.FolderOpen,
                                    title = strings.emptyFolderTitle,
                                    message = strings.emptyFolderSubtitle
                                )
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 130.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("vault_videos_grid"),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredItems, key = { it.id }) { item ->
                                        VaultVideoGridCard(
                                            item = item,
                                            folders = uiState.folders,
                                            isMultiSelectMode = uiState.isMultiSelectMode,
                                            isSelected = uiState.selectedItemIds.contains(item.id),
                                            onToggleSelect = { viewModel.toggleItemSelection(item.id) },
                                            onUnhide = {
                                                viewModel.prepareSingleItemUnhide(item) {
                                                    unhideFolderPickerLauncher.launch(null)
                                                }
                                            },
                                            onClick = { viewModel.playVideoItem(item) },
                                            onMoveToTrash = { viewModel.moveItemToTrash(item) },
                                            onDeletePermanently = { viewModel.deleteItemPermanently(item) },
                                            onSetFolder = { id, fId -> viewModel.setItemFolder(id, fId) },
                                            getThumbnail = { viewModel.getThumbnail(item) }
                                        )
                                    }
                                }
                            }
                        }

                        VaultTab.AUDIO -> {
                            if (filteredItems.isEmpty()) {
                                EmptyStateView(
                                    icon = Icons.Default.FolderOpen,
                                    title = strings.emptyFolderTitle,
                                    message = strings.emptyFolderSubtitle
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("vault_audio_list"),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredItems, key = { it.id }) { item ->
                                        VaultItemRow(
                                            item = item,
                                            folders = uiState.folders,
                                            icon = Icons.Default.Audiotrack,
                                            isMultiSelectMode = uiState.isMultiSelectMode,
                                            isSelected = uiState.selectedItemIds.contains(item.id),
                                            onToggleSelect = { viewModel.toggleItemSelection(item.id) },
                                            onUnhide = {
                                                viewModel.prepareSingleItemUnhide(item) {
                                                    unhideFolderPickerLauncher.launch(null)
                                                }
                                            },
                                            onClick = { viewModel.playAudioItem(item) },
                                            onOpenExternal = {
                                                viewModel.openExternalMedia(item) { uri, mime ->
                                                    launchExternalViewIntent(context, uri, mime)
                                                }
                                            },
                                            onMoveToTrash = { viewModel.moveItemToTrash(item) },
                                            onDeletePermanently = { viewModel.deleteItemPermanently(item) },
                                            onSetFolder = { id, fId -> viewModel.setItemFolder(id, fId) },
                                            getThumbnail = { viewModel.getThumbnail(item) }
                                        )
                                    }
                                }
                            }
                        }

                        VaultTab.NOTES -> {
                            if (filteredNotes.isEmpty()) {
                                EmptyStateView(
                                    icon = Icons.Default.FolderOpen,
                                    title = strings.emptyFolderTitle,
                                    message = strings.emptyFolderSubtitle
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("vault_notes_list"),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredNotes, key = { it.id }) { note ->
                                        VaultNoteRow(
                                            note = note,
                                            folders = uiState.folders,
                                            onClick = { viewModel.editNote(note) },
                                            onMoveToTrash = { viewModel.moveNoteToTrash(note.id) },
                                            onDeletePermanently = { viewModel.deleteNotePermanently(note.id) },
                                            onSetFolder = { id, fId -> viewModel.setNoteFolder(id, fId) }
                                        )
                                    }
                                }
                            }
                        }

                        VaultTab.TRASH -> {}
                    }

                    // Floating Action Speed-Dial for Import & Creation inside folder
                    VaultFabRow(
                        selectedTab = uiState.selectedTab,
                        onImportMedia = {
                            val request = when (uiState.selectedTab) {
                                VaultTab.IMAGES -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                VaultTab.VIDEOS -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                else -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            }
                            try {
                                photoVideoPickerLauncher.launch(request)
                            } catch (e: Exception) {
                                android.util.Log.e("VaultScreen", "Error launching media picker", e)
                            }
                        },
                        onImportAudio = {
                            try {
                                audioPickerLauncher.launch(arrayOf("audio/*", "*/*"))
                            } catch (e: Exception) {
                                android.util.Log.e("VaultScreen", "Error launching audio picker", e)
                            }
                        },
                        onNewNote = {
                            viewModel.startCreatingNote()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp)
                    )
                }
            }
        }
    }

    // --- Dialogs & Overlays ---

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text(strings.createFolderTitle, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text(strings.folderNamePlaceholder, color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ForestGreen,
                        unfocusedBorderColor = CharcoalBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName)
                            newFolderName = ""
                            showNewFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text(strings.createFolderTitle, color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            },
            containerColor = CharcoalDark
        )
    }

    // Rename Folder Dialog
    if (folderToRename != null) {
        AlertDialog(
            onDismissRequest = { folderToRename = null },
            title = { Text(strings.renameFolderTitle, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = renameFolderName,
                    onValueChange = { renameFolderName = it },
                    label = { Text(strings.folderNamePlaceholder, color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ForestGreen,
                        unfocusedBorderColor = CharcoalBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val folder = folderToRename
                        if (folder != null && renameFolderName.isNotBlank()) {
                            viewModel.renameFolder(folder.id, renameFolderName)
                        }
                        folderToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text(strings.renameFolderTitle, color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToRename = null }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            },
            containerColor = CharcoalDark
        )
    }

    // Delete Folder Dialog
    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text(strings.deleteFolderTitle, color = TextPrimary) },
            text = {
                Text(
                    text = strings.deleteFolderMessage,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val folder = folderToDelete
                        if (folder != null) {
                            viewModel.deleteFolder(folder.id)
                        }
                        folderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text(strings.deleteFolderTitle, color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            },
            containerColor = CharcoalDark
        )
    }


    // Change PIN Dialog / Sub-Flow
    if (uiState.isChangingPinDialogVisible) {
        PinAuthScreen(
            pinMode = uiState.pinMode,
            isPinSet = uiState.isPinSet,
            errorMessage = uiState.pinError,
            onSubmitPin = { viewModel.submitPin(it) },
            onBack = { viewModel.cancelChangePin() }
        )
    }

    // In-App Image Viewer Overlay
    if (uiState.viewingImageBitmap != null && uiState.viewingItem != null) {
        ImageViewerScreen(
            bitmap = uiState.viewingImageBitmap,
            item = uiState.viewingItem,
            folders = uiState.folders,
            onClose = { viewModel.closeImageViewer() },
            onUnhide = { itemToUnhide ->
                viewModel.prepareSingleItemUnhide(itemToUnhide) {
                    unhideFolderPickerLauncher.launch(null)
                }
            },
            onMoveToTrash = { viewModel.moveItemToTrash(it) },
            onDeletePermanently = { viewModel.deleteItemPermanently(it) },
            onSetFolder = { id, fId -> viewModel.setItemFolder(id, fId) }
        )
    }

    // Note Editor Overlay
    if (uiState.isCreatingNote || uiState.activeEditingNote != null) {
        NoteEditScreen(
            note = uiState.activeEditingNote,
            isCreating = uiState.isCreatingNote,
            folders = uiState.folders,
            selectedFolderId = uiState.selectedFolderId,
            onSave = { title, content, noteId, folderId ->
                viewModel.saveNote(title, content, noteId, folderId)
            },
            onClose = { viewModel.closeNoteEditor() },
            onMoveToTrash = { viewModel.moveNoteToTrash(it) },
            onDeletePermanently = { viewModel.deleteNotePermanently(it) }
        )
    }

    // Import Progress
    if (uiState.isImporting) {
        ImportProgressDialog(progressText = uiState.importProgressText)
    }

    // Import Summary
    uiState.importSummary?.let { summary ->
        ImportSummaryDialog(
            summary = summary,
            onDismiss = { viewModel.dismissImportSummary() }
        )
    }

    // Unhide Progress Dialog
    if (uiState.isUnhiding) {
        UnhideProgressDialog(progressText = uiState.unhideProgressText)
    }

    // Unhide Summary Dialog
    uiState.unhideSummary?.let { summary ->
        UnhideSummaryDialog(
            summary = summary,
            onDismiss = { viewModel.dismissUnhideSummary() }
        )
    }

    // In-App Audio Player Sheet / Dialog
    if (uiState.playingAudioItem != null) {
        VaultAudioPlayerDialog(
            item = uiState.playingAudioItem,
            dataSourceFactory = viewModel.getMediaDataSourceFactory(uiState.playingAudioItem),
            mediaFile = uiState.playingAudioFile,
            isLoading = uiState.isMediaPreparing,
            errorMessage = uiState.mediaErrorMessage,
            onClose = { viewModel.closeAudioPlayer() },
            onOpenExternal = {
                viewModel.openExternalMedia(uiState.playingAudioItem) { uri, mime ->
                    launchExternalViewIntent(context, uri, mime)
                }
            },
            getThumbnail = { viewModel.getThumbnail(uiState.playingAudioItem) }
        )
    }

    // In-App Video Player Screen / Dialog
    if (uiState.playingVideoItem != null) {
        VaultVideoPlayerScreen(
            item = uiState.playingVideoItem,
            dataSourceFactory = viewModel.getMediaDataSourceFactory(uiState.playingVideoItem),
            mediaFile = uiState.playingVideoFile,
            isLoading = uiState.isMediaPreparing,
            errorMessage = uiState.mediaErrorMessage,
            onClose = { viewModel.closeVideoPlayer() },
            onOpenExternal = {
                viewModel.openExternalMedia(uiState.playingVideoItem) { uri, mime ->
                    launchExternalViewIntent(context, uri, mime)
                }
            }
        )
    }
}

@Composable
private fun VaultFolderCard(
    folder: VaultFolderEntity,
    itemCount: Int,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("vault_folder_card_${folder.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        border = BorderStroke(1.dp, CharcoalBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ForestGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = ForestGreenLight,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("folder_card_menu_${folder.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Folder Options",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = ForestGreenLight)
                            },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed)
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$itemCount items",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun VaultImageGridCard(
    item: VaultItemEntity,
    folders: List<VaultFolderEntity>,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onUnhide: () -> Unit,
    onClick: () -> Unit,
    onMoveToTrash: () -> Unit,
    onDeletePermanently: () -> Unit,
    onSetFolder: (itemId: String, folderId: String?) -> Unit,
    getThumbnail: suspend () -> Bitmap?
) {
    var showMenu by remember { mutableStateOf(false) }
    val thumbnailBitmap by produceState<Bitmap?>(initialValue = null, key1 = item.id) {
        value = getThumbnail()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (isMultiSelectMode) {
                    onToggleSelect()
                } else {
                    onClick()
                }
            }
            .testTag("vault_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BlueContainer else CharcoalCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) BlueAccent else CharcoalBorder
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap!!.asImageBitmap(),
                    contentDescription = item.originalDisplayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CharcoalElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Bottom Gradient Scrim with title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Transparent, CharcoalDark.copy(alpha = 0.88f))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.originalDisplayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isMultiSelectMode) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("item_menu_button_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Unhide") },
                                    leadingIcon = {
                                        Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = BlueAccent)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onUnhide()
                                    },
                                    modifier = Modifier.testTag("item_unhide_${item.id}")
                                )
                                DropdownMenuItem(
                                    text = { Text("Select") },
                                    leadingIcon = {
                                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = BlueAccent)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onToggleSelect()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Move to Trash") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = TextPrimary)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onMoveToTrash()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Permanently") },
                                    leadingIcon = {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = ErrorRed)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeletePermanently()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Multi-select Checkbox
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = BlueAccent,
                        uncheckedColor = TextPrimary,
                        checkmarkColor = TextPrimary
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .testTag("checkbox_${item.id}")
                )
            }
        }
    }
}

@Composable
private fun VaultVideoGridCard(
    item: VaultItemEntity,
    folders: List<VaultFolderEntity>,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onUnhide: () -> Unit,
    onClick: () -> Unit,
    onMoveToTrash: () -> Unit,
    onDeletePermanently: () -> Unit,
    onSetFolder: (itemId: String, folderId: String?) -> Unit,
    getThumbnail: suspend () -> Bitmap?
) {
    var showMenu by remember { mutableStateOf(false) }
    val thumbnailBitmap by produceState<Bitmap?>(initialValue = null, key1 = item.id) {
        value = getThumbnail()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.25f)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (isMultiSelectMode) {
                    onToggleSelect()
                } else {
                    onClick()
                }
            }
            .testTag("vault_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BlueContainer else CharcoalCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) BlueAccent else CharcoalBorder
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap!!.asImageBitmap(),
                    contentDescription = item.originalDisplayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CharcoalElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Play Badge Overlay
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CharcoalBlack.copy(alpha = 0.65f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Bottom Gradient Scrim with title & options
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Transparent, CharcoalDark.copy(alpha = 0.92f))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.originalDisplayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!isMultiSelectMode) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("menu_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Unhide Video") },
                                    leadingIcon = {
                                        Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = BlueAccent)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onUnhide()
                                    },
                                    modifier = Modifier.testTag("unhide_item_${item.id}")
                                )

                                if (folders.isNotEmpty()) {
                                    folders.forEach { folder ->
                                        DropdownMenuItem(
                                            text = { Text("Move to: ${folder.name}") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Folder, contentDescription = null, tint = BlueAccent)
                                            },
                                            onClick = {
                                                showMenu = false
                                                onSetFolder(item.id, folder.id)
                                            }
                                        )
                                    }
                                    if (item.folderId != null) {
                                        DropdownMenuItem(
                                            text = { Text("Remove from Folder") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                                            },
                                            onClick = {
                                                showMenu = false
                                                onSetFolder(item.id, null)
                                            }
                                        )
                                    }
                                }

                                DropdownMenuItem(
                                    text = { Text("Move to Trash") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = TextPrimary)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onMoveToTrash()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Permanently") },
                                    leadingIcon = {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = ErrorRed)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeletePermanently()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Multi-select Checkbox
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = BlueAccent,
                        uncheckedColor = TextPrimary,
                        checkmarkColor = TextPrimary
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .testTag("checkbox_${item.id}")
                )
            }
        }
    }
}

@Composable
private fun VaultItemRow(
    item: VaultItemEntity,
    folders: List<VaultFolderEntity>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String? = null,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onUnhide: () -> Unit = {},
    onClick: () -> Unit,
    onOpenExternal: (() -> Unit)? = null,
    onMoveToTrash: () -> Unit,
    onDeletePermanently: () -> Unit,
    onSetFolder: (itemId: String, folderId: String?) -> Unit,
    getThumbnail: (suspend () -> Bitmap?)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val thumbBitmap by produceState<Bitmap?>(initialValue = null, key1 = item.id) {
        if (getThumbnail != null) {
            value = getThumbnail()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isMultiSelectMode) {
                    onToggleSelect()
                } else {
                    onClick()
                }
            }
            .testTag("vault_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BlueContainer else CharcoalCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) BlueAccent else CharcoalBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = BlueAccent,
                        uncheckedColor = TextSecondary,
                        checkmarkColor = TextPrimary
                    ),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("checkbox_${item.id}")
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) BlueAccent.copy(alpha = 0.3f) else BlueContainer),
                contentAlignment = Alignment.Center
            ) {
                if (thumbBitmap != null) {
                    Image(
                        bitmap = thumbBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (item.type == VaultContentType.VIDEO.name) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BlueAccentLight,
                        modifier = Modifier.size(22.dp)
                    )
                }
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
                    text = "${formatBytes(item.sizeBytes)}${if (actionLabel != null) " • $actionLabel" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            if (!isMultiSelectMode) {
                if (onOpenExternal != null) {
                    IconButton(onClick = onOpenExternal) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open with...",
                            tint = BlueAccentLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (actionLabel != null) {
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = actionLabel,
                            tint = BlueAccentLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("item_menu_button_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Unhide") },
                            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = BlueAccent) },
                            onClick = {
                                showMenu = false
                                onUnhide()
                            },
                            modifier = Modifier.testTag("item_unhide_${item.id}")
                        )
                        DropdownMenuItem(
                            text = { Text("Select") },
                            leadingIcon = { Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = BlueAccent) },
                            onClick = {
                                showMenu = false
                                onToggleSelect()
                            }
                        )
                        if (onOpenExternal != null) {
                            DropdownMenuItem(
                                text = { Text("Open with...") },
                                leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null, tint = BlueAccent) },
                                onClick = {
                                    showMenu = false
                                    onOpenExternal()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Move to Trash") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = TextPrimary) },
                            onClick = {
                                showMenu = false
                                onMoveToTrash()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Permanently") },
                            leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = ErrorRed) },
                            onClick = {
                                showMenu = false
                                onDeletePermanently()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultNoteRow(
    note: DecryptedNote,
    folders: List<VaultFolderEntity>,
    onClick: () -> Unit,
    onMoveToTrash: () -> Unit,
    onDeletePermanently: () -> Unit,
    onSetFolder: (noteId: String, folderId: String?) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("vault_note_${note.id}"),
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
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = BlueAccentLight,
                    modifier = Modifier.size(22.dp)
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
                    text = note.content.ifBlank { "Empty note" },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("note_menu_button_${note.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Move to Trash") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = TextPrimary) },
                        onClick = {
                            showMenu = false
                            onMoveToTrash()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Permanently") },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = ErrorRed) },
                        onClick = {
                            showMenu = false
                            onDeletePermanently()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultFabRow(
    selectedTab: VaultTab,
    onImportMedia: () -> Unit,
    onImportAudio: () -> Unit,
    onNewNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = {
            when (selectedTab) {
                VaultTab.IMAGES, VaultTab.VIDEOS -> onImportMedia()
                VaultTab.AUDIO -> onImportAudio()
                VaultTab.NOTES -> onNewNote()
                else -> onImportMedia()
            }
        },
        containerColor = BlueAccent,
        contentColor = TextPrimary,
        shape = CircleShape,
        modifier = modifier.testTag("vault_primary_fab")
    ) {
        val icon = when (selectedTab) {
            VaultTab.IMAGES -> Icons.Default.PhotoLibrary
            VaultTab.VIDEOS -> Icons.Default.Videocam
            VaultTab.AUDIO -> Icons.Default.Audiotrack
            VaultTab.NOTES -> Icons.Default.NoteAdd
            else -> Icons.Default.Add
        }
        Icon(imageVector = icon, contentDescription = "Add Content", modifier = Modifier.size(24.dp))
    }
}

private fun launchExternalViewIntent(context: Context, uri: Uri, mimeType: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open with...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (e: Exception) {
        android.util.Log.e("VaultScreen", "Unable to open external media", e)
    }
}
