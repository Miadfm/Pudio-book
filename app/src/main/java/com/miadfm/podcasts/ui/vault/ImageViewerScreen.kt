package com.miadfm.podcasts.ui.vault

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.miadfm.podcasts.data.vault.VaultFolderEntity
import com.miadfm.podcasts.data.vault.VaultItemEntity
import com.miadfm.podcasts.ui.components.formatBytes
import com.miadfm.podcasts.ui.theme.BlueAccentLight
import com.miadfm.podcasts.ui.theme.CharcoalBlack
import com.miadfm.podcasts.ui.theme.CharcoalDark
import com.miadfm.podcasts.ui.theme.ErrorRed
import com.miadfm.podcasts.ui.theme.TextPrimary
import com.miadfm.podcasts.ui.theme.TextSecondary
import kotlin.math.max

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.path

private var _rotateRightIcon: ImageVector? = null
val RotateRightIcon: ImageVector
    get() {
        if (_rotateRightIcon != null) return _rotateRightIcon!!
        _rotateRightIcon = ImageVector.Builder(
            name = "RotateRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            fill = SolidColor(Color.White),
            pathData = listOf(
                androidx.compose.ui.graphics.vector.PathNode.MoveTo(15.55f, 5.55f),
                androidx.compose.ui.graphics.vector.PathNode.LineTo(11f, 1f),
                androidx.compose.ui.graphics.vector.PathNode.VerticalTo(4.07f),
                androidx.compose.ui.graphics.vector.PathNode.CurveTo(7.06f, 4.56f, 4f, 7.92f, 4f, 12f),
                androidx.compose.ui.graphics.vector.PathNode.CurveTo(4f, 16.08f, 7.05f, 19.44f, 11f, 19.93f),
                androidx.compose.ui.graphics.vector.PathNode.VerticalTo(17.91f),
                androidx.compose.ui.graphics.vector.PathNode.CurveTo(8.16f, 17.43f, 6f, 14.97f, 6f, 12f),
                androidx.compose.ui.graphics.vector.PathNode.CurveTo(6f, 8.69f, 8.69f, 6f, 12f, 6f),
                androidx.compose.ui.graphics.vector.PathNode.VerticalTo(9.07f),
                androidx.compose.ui.graphics.vector.PathNode.LineTo(15.55f, 5.55f),
                androidx.compose.ui.graphics.vector.PathNode.Close
            )
        ).build()
        return _rotateRightIcon!!
    }

@Composable
fun ImageViewerScreen(
    bitmap: Bitmap,
    item: VaultItemEntity,
    folders: List<VaultFolderEntity>,
    onClose: () -> Unit,
    onUnhide: (VaultItemEntity) -> Unit = {},
    onMoveToTrash: (VaultItemEntity) -> Unit,
    onDeletePermanently: (VaultItemEntity) -> Unit,
    onSetFolder: (itemId: String, folderId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFolderMenu by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    // Smooth Zoom, Pan & Rotation State (Viewing only - never modifies original file)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }

    // Automatically reset zoom, pan and rotation whenever viewing a different item
    LaunchedEffect(item.id) {
        scale = 1f
        offset = Offset.Zero
        rotationDegrees = 0f
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(CharcoalBlack)
                .testTag("image_viewer_dialog")
        ) {
            val maxOffsetX = (constraints.maxWidth * (scale - 1f) / 2f).coerceAtLeast(0f)
            val maxOffsetY = (constraints.maxHeight * (scale - 1f) / 2f).coerceAtLeast(0f)

            // Zoomable and Pannable Image Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(item.id) {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                            },
                            onDoubleTap = { tapOffset ->
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                    // Center zoom around tapped area
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    offset = Offset(
                                        x = (centerX - tapOffset.x) * 1.5f,
                                        y = (centerY - tapOffset.y) * 1.5f
                                    )
                                }
                            }
                        )
                    }
                    .pointerInput(item.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            if (newScale > 1f) {
                                val currentMaxX = (constraints.maxWidth * (newScale - 1f) / 2f).coerceAtLeast(0f)
                                val currentMaxY = (constraints.maxHeight * (newScale - 1f) / 2f).coerceAtLeast(0f)
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-currentMaxX, currentMaxX),
                                    y = (offset.y + pan.y).coerceIn(-currentMaxY, currentMaxY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = item.originalDisplayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 48.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotationDegrees
                            translationX = offset.x
                            translationY = offset.y
                        },
                    contentScale = ContentScale.Fit
                )
            }

            // Top Overlay Bar (Toggleable / Fade Animation)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalDark.copy(alpha = 0.88f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_image_viewer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.originalDisplayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${formatBytes(item.sizeBytes)}${if (rotationDegrees != 0f) " • ${(rotationDegrees.toInt())}°" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    // Rotate 90 degrees button
                    IconButton(
                        onClick = {
                            rotationDegrees = (rotationDegrees + 90f) % 360f
                        },
                        modifier = Modifier.testTag("image_rotate_button")
                    ) {
                        Icon(
                            imageVector = RotateRightIcon,
                            contentDescription = "Rotate Image 90°",
                            tint = BlueAccentLight
                        )
                    }

                    // Move to folder
                    Box {
                        IconButton(
                            onClick = { showFolderMenu = true },
                            modifier = Modifier.testTag("image_folder_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DriveFileMove,
                                contentDescription = "Move to Folder",
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
                                        onSetFolder(item.id, folder.id)
                                        showFolderMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Unhide
                    IconButton(
                        onClick = {
                            onUnhide(item)
                            onClose()
                        },
                        modifier = Modifier.testTag("image_unhide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DriveFileMove,
                            contentDescription = "Unhide",
                            tint = BlueAccentLight
                        )
                    }

                    // Move to Trash
                    IconButton(
                        onClick = {
                            onMoveToTrash(item)
                            onClose()
                        },
                        modifier = Modifier.testTag("image_trash_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Move to Trash",
                            tint = TextPrimary
                        )
                    }

                    // Delete Permanently
                    IconButton(
                        onClick = {
                            onDeletePermanently(item)
                            onClose()
                        },
                        modifier = Modifier.testTag("image_permanent_delete_button")
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
