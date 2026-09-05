package com.miadfm.podcasts.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miadfm.podcasts.data.security.PinSecurityManager
import com.miadfm.podcasts.data.vault.DecryptedNote
import com.miadfm.podcasts.data.vault.ImportFileResult
import com.miadfm.podcasts.data.vault.ImportSummary
import com.miadfm.podcasts.data.vault.UnhideSummary
import com.miadfm.podcasts.data.vault.VaultContentType
import com.miadfm.podcasts.data.vault.VaultFolderEntity
import com.miadfm.podcasts.data.vault.VaultItemEntity
import com.miadfm.podcasts.data.vault.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PinMode {
    SETUP_ENTER_NEW,
    SETUP_CONFIRM_NEW,
    UNLOCK,
    CHANGE_VERIFY_CURRENT,
    CHANGE_ENTER_NEW,
    CHANGE_CONFIRM_NEW
}

enum class VaultTab {
    IMAGES,
    VIDEOS,
    AUDIO,
    NOTES,
    TRASH
}

data class VaultUiState(
    val isPinSet: Boolean = false,
    val isUnlocked: Boolean = false,
    val pinMode: PinMode = PinMode.UNLOCK,
    val pinError: String? = null,
    val userNotification: String? = null,
    val selectedTab: VaultTab = VaultTab.IMAGES,
    val selectedFolderId: String? = null,
    val folders: List<VaultFolderEntity> = emptyList(),
    val items: List<VaultItemEntity> = emptyList(),
    val notes: List<DecryptedNote> = emptyList(),
    val trashedItems: List<VaultItemEntity> = emptyList(),
    val trashedNotes: List<DecryptedNote> = emptyList(),
    val selectedItemIds: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val isImporting: Boolean = false,
    val importProgressText: String = "",
    val importSummary: ImportSummary? = null,
    val isUnhiding: Boolean = false,
    val unhideProgressText: String = "",
    val unhideSummary: UnhideSummary? = null,
    val pendingUnhideItems: List<VaultItemEntity> = emptyList(),
    val viewingImageBitmap: Bitmap? = null,
    val viewingItem: VaultItemEntity? = null,
    val playingAudioItem: VaultItemEntity? = null,
    val playingAudioFile: java.io.File? = null,
    val playingVideoItem: VaultItemEntity? = null,
    val playingVideoFile: java.io.File? = null,
    val isMediaPreparing: Boolean = false,
    val mediaErrorMessage: String? = null,
    val activeEditingNote: DecryptedNote? = null,
    val isCreatingNote: Boolean = false,
    val preparedExternalMediaUri: Uri? = null,
    val preparedExternalMediaMime: String? = null,
    val isChangingPinDialogVisible: Boolean = false
)

class VaultViewModel(
    private val vaultRepository: VaultRepository,
    private val pinSecurityManager: PinSecurityManager,
    private val languageManager: com.miadfm.podcasts.data.settings.AppLanguageManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private var temporaryNewPin: String = ""

    init {
        refreshPinStatus()
        observeData()
        runStartupCleanup()
    }

    fun ensureDefaultFolder(isPersian: Boolean? = null) {
        val usePersian = isPersian ?: (languageManager?.getLanguage() == com.miadfm.podcasts.data.settings.AppLanguage.PERSIAN)
        val defaultName = if (usePersian) "ولت من" else "My Vault"
        viewModelScope.launch {
            vaultRepository.ensureDefaultFolderExists(defaultName)
        }
    }

    fun refreshPinStatus() {
        val pinSet = pinSecurityManager.isPinSet()
        _uiState.value = _uiState.value.copy(
            isPinSet = pinSet,
            pinMode = if (pinSet) PinMode.UNLOCK else PinMode.SETUP_ENTER_NEW,
            pinError = null
        )
    }

    private fun runStartupCleanup() {
        viewModelScope.launch {
            vaultRepository.cleanupExpiredTrash()
            vaultRepository.cleanTemporaryPlaybackFiles()
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                vaultRepository.allFolders,
                vaultRepository.activeItems,
                vaultRepository.activeNotes,
                vaultRepository.trashedItems,
                vaultRepository.trashedNotes
            ) { folders, items, noteEntities, trashedItems, trashedNoteEntities ->
                val decryptedActiveNotes = vaultRepository.decryptAllNotes(noteEntities)
                val decryptedTrashedNotes = vaultRepository.decryptAllNotes(trashedNoteEntities)

                _uiState.value = _uiState.value.copy(
                    folders = folders,
                    items = items,
                    notes = decryptedActiveNotes,
                    trashedItems = trashedItems,
                    trashedNotes = decryptedTrashedNotes
                )
            }.collect {}
        }
    }

    // --- PIN Handling ---
    fun submitPin(pin: String) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(pinError = null)

        when (currentState.pinMode) {
            PinMode.SETUP_ENTER_NEW -> {
                if (pin.length in 4..8 && pin.all { it.isDigit() }) {
                    temporaryNewPin = pin
                    _uiState.value = _uiState.value.copy(
                        pinMode = PinMode.SETUP_CONFIRM_NEW,
                        pinError = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        pinError = "PIN must be between 4 and 8 digits"
                    )
                }
            }
            PinMode.SETUP_CONFIRM_NEW -> {
                if (pin == temporaryNewPin) {
                    val saved = pinSecurityManager.setPin(pin)
                    if (saved) {
                        temporaryNewPin = ""
                        _uiState.value = _uiState.value.copy(
                            isPinSet = true,
                            isUnlocked = true,
                            selectedFolderId = null,
                            pinMode = PinMode.UNLOCK,
                            pinError = null
                        )
                        runStartupCleanup()
                        ensureDefaultFolder()
                    } else {
                        _uiState.value = _uiState.value.copy(pinError = "Failed to save PIN")
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        pinMode = PinMode.SETUP_ENTER_NEW,
                        pinError = "PINs do not match. Please try again."
                    )
                    temporaryNewPin = ""
                }
            }
            PinMode.UNLOCK -> {
                val isCorrect = pinSecurityManager.verifyPin(pin)
                if (isCorrect) {
                    _uiState.value = _uiState.value.copy(
                        isUnlocked = true,
                        selectedFolderId = null,
                        pinError = null
                    )
                    runStartupCleanup()
                    ensureDefaultFolder()
                } else {
                    _uiState.value = _uiState.value.copy(
                        pinError = "Incorrect PIN"
                    )
                }
            }
            PinMode.CHANGE_VERIFY_CURRENT -> {
                val isCorrect = pinSecurityManager.verifyPin(pin)
                if (isCorrect) {
                    _uiState.value = _uiState.value.copy(
                        pinMode = PinMode.CHANGE_ENTER_NEW,
                        pinError = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        pinError = "Incorrect PIN"
                    )
                }
            }
            PinMode.CHANGE_ENTER_NEW -> {
                if (pin.length in 4..8 && pin.all { it.isDigit() }) {
                    temporaryNewPin = pin
                    _uiState.value = _uiState.value.copy(
                        pinMode = PinMode.CHANGE_CONFIRM_NEW,
                        pinError = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        pinError = "PIN must be between 4 and 8 digits"
                    )
                }
            }
            PinMode.CHANGE_CONFIRM_NEW -> {
                if (pin == temporaryNewPin) {
                    val saved = pinSecurityManager.setPin(pin)
                    if (saved) {
                        temporaryNewPin = ""
                        _uiState.value = _uiState.value.copy(
                            pinMode = PinMode.UNLOCK,
                            isChangingPinDialogVisible = false,
                            userNotification = "PIN changed successfully",
                            pinError = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(pinError = "Failed to change PIN")
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        pinMode = PinMode.CHANGE_ENTER_NEW,
                        pinError = "PINs do not match. Please try again."
                    )
                    temporaryNewPin = ""
                }
            }
        }
    }

    fun startChangePin() {
        temporaryNewPin = ""
        _uiState.value = _uiState.value.copy(
            isChangingPinDialogVisible = true,
            pinMode = PinMode.CHANGE_VERIFY_CURRENT,
            pinError = null
        )
    }

    fun cancelChangePin() {
        temporaryNewPin = ""
        _uiState.value = _uiState.value.copy(
            isChangingPinDialogVisible = false,
            pinMode = PinMode.UNLOCK,
            pinError = null
        )
    }

    fun lockVault() {
        temporaryNewPin = ""
        closeAudioPlayer()
        closeVideoPlayer()
        vaultRepository.cleanTemporaryPlaybackFiles()
        _uiState.value = _uiState.value.copy(
            isUnlocked = false,
            selectedFolderId = null,
            pinMode = if (pinSecurityManager.isPinSet()) PinMode.UNLOCK else PinMode.SETUP_ENTER_NEW,
            pinError = null,
            viewingImageBitmap = null,
            viewingItem = null,
            playingAudioItem = null,
            playingAudioFile = null,
            playingVideoItem = null,
            playingVideoFile = null,
            activeEditingNote = null,
            isCreatingNote = false,
            preparedExternalMediaUri = null,
            isChangingPinDialogVisible = false
        )
    }

    // --- Tab and Folder Navigation ---
    fun selectTab(tab: VaultTab) {
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            selectedFolderId = null
        )
    }

    fun selectFolder(folderId: String?) {
        _uiState.value = _uiState.value.copy(selectedFolderId = folderId)
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            vaultRepository.createFolder(name)
        }
    }

    fun deleteFolder(id: String) {
        viewModelScope.launch {
            vaultRepository.deleteFolder(id)
            if (_uiState.value.selectedFolderId == id) {
                _uiState.value = _uiState.value.copy(selectedFolderId = null)
            }
        }
    }

    fun renameFolder(id: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            vaultRepository.renameFolder(id, newName)
        }
    }

    // --- Import Flow ---
    fun importFiles(uris: List<Uri>, folderId: String? = null) {
        if (uris.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            isImporting = true,
            importProgressText = "Preparing to import ${uris.size} file${if (uris.size > 1) "s" else ""}...",
            importSummary = null
        )

        viewModelScope.launch {
            try {
                val summary = vaultRepository.importFiles(
                    uris = uris,
                    folderId = folderId ?: _uiState.value.selectedFolderId,
                    onProgress = { current, total, fileName ->
                        _uiState.value = _uiState.value.copy(
                            importProgressText = "Processing $current of $total: $fileName"
                        )
                    }
                )

                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importProgressText = "",
                    importSummary = summary
                )
            } catch (e: Exception) {
                Log.e("VaultViewModel", "Error importing files", e)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importProgressText = "",
                    importSummary = ImportSummary(
                        totalProcessed = uris.size,
                        successCount = 0,
                        failureCount = uris.size,
                        results = uris.map {
                            ImportFileResult(
                                fileName = "Selected file",
                                isSuccess = false,
                                errorReason = "Import error: ${e.localizedMessage ?: "Operation failed"}"
                            )
                        }
                    )
                )
            }
        }
    }

    fun dismissImportSummary() {
        _uiState.value = _uiState.value.copy(importSummary = null)
    }

    // --- Multi-Select Mode ---
    fun toggleMultiSelectMode(enable: Boolean? = null) {
        val newMode = enable ?: !_uiState.value.isMultiSelectMode
        _uiState.value = _uiState.value.copy(
            isMultiSelectMode = newMode,
            selectedItemIds = if (!newMode) emptySet() else _uiState.value.selectedItemIds
        )
    }

    fun toggleItemSelection(id: String) {
        val current = _uiState.value.selectedItemIds.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _uiState.value = _uiState.value.copy(
            selectedItemIds = current,
            isMultiSelectMode = if (current.isNotEmpty()) true else _uiState.value.isMultiSelectMode
        )
    }

    fun selectAllCurrentItems(items: List<VaultItemEntity>) {
        val allIds = items.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(
            selectedItemIds = allIds,
            isMultiSelectMode = true
        )
    }

    fun clearItemSelection() {
        _uiState.value = _uiState.value.copy(
            selectedItemIds = emptySet(),
            isMultiSelectMode = false
        )
    }

    fun batchMoveSelectedToTrash() {
        val selectedIds = _uiState.value.selectedItemIds
        if (selectedIds.isEmpty()) return
        val itemsToTrash = _uiState.value.items.filter { selectedIds.contains(it.id) }
        viewModelScope.launch {
            for (item in itemsToTrash) {
                vaultRepository.moveItemToTrash(item)
            }
            clearItemSelection()
        }
    }

    // --- Unhide Flow ---
    fun prepareSingleItemUnhide(item: VaultItemEntity, onTriggerPicker: () -> Unit) {
        _uiState.value = _uiState.value.copy(
            pendingUnhideItems = listOf(item)
        )
        onTriggerPicker()
    }

    fun prepareMultiItemUnhide(onTriggerPicker: () -> Unit) {
        val selectedIds = _uiState.value.selectedItemIds
        if (selectedIds.isEmpty()) return
        val itemsToUnhide = _uiState.value.items.filter { selectedIds.contains(it.id) }
        if (itemsToUnhide.isEmpty()) return

        _uiState.value = _uiState.value.copy(
            pendingUnhideItems = itemsToUnhide
        )
        onTriggerPicker()
    }

    fun executeUnhide(destinationTreeUri: Uri) {
        val itemsToProcess = _uiState.value.pendingUnhideItems
        if (itemsToProcess.isEmpty()) return

        _uiState.value = _uiState.value.copy(
            isUnhiding = true,
            unhideProgressText = "Restoring 1 of ${itemsToProcess.size}: ${itemsToProcess.first().originalDisplayName}",
            unhideSummary = null
        )

        viewModelScope.launch {
            val summary = vaultRepository.unhideItems(
                items = itemsToProcess,
                destinationTreeUri = destinationTreeUri,
                deleteFromVaultOnSuccess = true, // Default: Move out of Vault
                onProgress = { current, total, fileName ->
                    _uiState.value = _uiState.value.copy(
                        unhideProgressText = "Restoring $current of $total: $fileName"
                    )
                }
            )

            _uiState.value = _uiState.value.copy(
                isUnhiding = false,
                unhideProgressText = "",
                unhideSummary = summary,
                pendingUnhideItems = emptyList(),
                selectedItemIds = emptySet(),
                isMultiSelectMode = false
            )
        }
    }

    fun cancelPendingUnhide() {
        _uiState.value = _uiState.value.copy(
            pendingUnhideItems = emptyList()
        )
    }

    fun dismissUnhideSummary() {
        _uiState.value = _uiState.value.copy(
            unhideSummary = null
        )
    }

    // --- Image Viewing ---
    fun viewImage(item: VaultItemEntity) {
        viewModelScope.launch {
            val bitmap = vaultRepository.decryptImageBitmap(item)
            if (bitmap != null) {
                _uiState.value = _uiState.value.copy(
                    viewingImageBitmap = bitmap,
                    viewingItem = item
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    userNotification = "Unable to open selected content"
                )
            }
        }
    }

    fun closeImageViewer() {
        _uiState.value = _uiState.value.copy(
            viewingImageBitmap = null,
            viewingItem = null
        )
    }

    // --- Media DataSource Factory ---
    fun getMediaDataSourceFactory(item: VaultItemEntity): androidx.media3.datasource.DataSource.Factory {
        return vaultRepository.getMediaDataSourceFactory(item)
    }

    // --- Internal Audio Player ---
    fun playAudioItem(item: VaultItemEntity) {
        closeVideoPlayer()
        _uiState.value = _uiState.value.copy(
            playingAudioItem = item,
            playingAudioFile = null,
            isMediaPreparing = false,
            mediaErrorMessage = null
        )
    }

    fun closeAudioPlayer() {
        val currentFile = _uiState.value.playingAudioFile
        _uiState.value = _uiState.value.copy(
            playingAudioItem = null,
            playingAudioFile = null,
            isMediaPreparing = false,
            mediaErrorMessage = null
        )
        viewModelScope.launch {
            if (currentFile != null) {
                vaultRepository.deleteTemporaryFile(currentFile)
            }
            vaultRepository.cleanStaleTemporaryPlaybackFiles()
        }
    }

    // --- Internal Video Player ---
    fun playVideoItem(item: VaultItemEntity) {
        closeAudioPlayer()
        _uiState.value = _uiState.value.copy(
            playingVideoItem = item,
            playingVideoFile = null,
            isMediaPreparing = false,
            mediaErrorMessage = null
        )
    }

    fun closeVideoPlayer() {
        val currentFile = _uiState.value.playingVideoFile
        _uiState.value = _uiState.value.copy(
            playingVideoItem = null,
            playingVideoFile = null,
            isMediaPreparing = false,
            mediaErrorMessage = null
        )
        viewModelScope.launch {
            if (currentFile != null) {
                vaultRepository.deleteTemporaryFile(currentFile)
            }
            vaultRepository.cleanStaleTemporaryPlaybackFiles()
        }
    }

    suspend fun getThumbnail(item: VaultItemEntity): Bitmap? {
        return vaultRepository.decryptThumbnail(item)
    }

    // --- App Lifecycle ---
    fun onAppResume() {
        viewModelScope.launch {
            vaultRepository.cleanStaleTemporaryPlaybackFiles(maxAgeMs = 3 * 60 * 1000L)
        }
    }

    // --- External Media Playback (Open With...) ---
    fun openExternalMedia(item: VaultItemEntity, onReady: (uri: Uri, mimeType: String) -> Unit) {
        viewModelScope.launch {
            val prepared = vaultRepository.prepareTemporaryMediaUri(item)
            if (prepared != null) {
                val (uri, mime) = prepared
                _uiState.value = _uiState.value.copy(
                    preparedExternalMediaUri = uri,
                    preparedExternalMediaMime = mime
                )
                onReady(uri, mime)
            } else {
                _uiState.value = _uiState.value.copy(
                    userNotification = "Unable to open selected content"
                )
            }
        }
    }

    fun clearPreparedExternalMedia() {
        _uiState.value = _uiState.value.copy(
            preparedExternalMediaUri = null,
            preparedExternalMediaMime = null
        )
        vaultRepository.cleanTemporaryPlaybackFiles()
    }

    // --- Notes Management ---
    fun startCreatingNote() {
        _uiState.value = _uiState.value.copy(
            isCreatingNote = true,
            activeEditingNote = null
        )
    }

    fun editNote(note: DecryptedNote) {
        _uiState.value = _uiState.value.copy(
            isCreatingNote = false,
            activeEditingNote = note
        )
    }

    fun closeNoteEditor() {
        _uiState.value = _uiState.value.copy(
            isCreatingNote = false,
            activeEditingNote = null
        )
    }

    fun saveNote(title: String, content: String, noteId: String?, folderId: String?) {
        viewModelScope.launch {
            val resolvedFolderId = folderId ?: _uiState.value.selectedFolderId ?: _uiState.value.folders.firstOrNull()?.id
            vaultRepository.saveNote(
                title = title,
                content = content,
                noteId = noteId,
                folderId = resolvedFolderId
            )
            closeNoteEditor()
        }
    }

    // --- Item & Note Trash / Restore / Delete ---
    fun moveItemToTrash(item: VaultItemEntity) {
        viewModelScope.launch {
            vaultRepository.moveItemToTrash(item)
        }
    }

    fun restoreItem(item: VaultItemEntity) {
        viewModelScope.launch {
            vaultRepository.restoreItem(item)
        }
    }

    fun deleteItemPermanently(item: VaultItemEntity) {
        viewModelScope.launch {
            vaultRepository.deleteItemPermanently(item)
        }
    }

    fun moveNoteToTrash(noteId: String) {
        viewModelScope.launch {
            vaultRepository.moveNoteToTrash(noteId)
        }
    }

    fun restoreNote(noteId: String) {
        viewModelScope.launch {
            vaultRepository.restoreNote(noteId)
        }
    }

    fun deleteNotePermanently(noteId: String) {
        viewModelScope.launch {
            vaultRepository.deleteNotePermanently(noteId)
        }
    }

    fun setItemFolder(itemId: String, folderId: String?) {
        viewModelScope.launch {
            vaultRepository.setItemFolder(itemId, folderId)
        }
    }

    fun setNoteFolder(noteId: String, folderId: String?) {
        viewModelScope.launch {
            vaultRepository.setNoteFolder(noteId, folderId)
        }
    }

    fun clearNotification() {
        _uiState.value = _uiState.value.copy(userNotification = null)
    }

    fun clearTemporaryCache(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            vaultRepository.clearTemporaryCache()
            _uiState.value = _uiState.value.copy(
                userNotification = "Temporary cache cleared"
            )
            onSuccess?.invoke()
        }
    }
}
