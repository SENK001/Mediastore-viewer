package com.senk.mediastoreviewer.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.senk.mediastoreviewer.viewmodel.DirectoryGroup
import com.senk.mediastoreviewer.viewmodel.MediaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val specialNames = setOf("全部", "相机", "视频", "收藏", "Raw")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryListScreen(
    viewModel: MediaViewModel,
    onDirectoryClick: (String) -> Unit
) {
    val directories by viewModel.directories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val specialDirs = remember(directories) {
        directories.filter { it.name in specialNames }
    }
    val otherDirs = remember(directories) {
        directories.filter { it.name !in specialNames }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MediaStore Viewer") },
                actions = {
                    IconButton(onClick = { viewModel.loadMedia() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                directories.isEmpty() -> {
                    Text(
                        text = "没有找到媒体文件",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(specialDirs, key = { it.name }) { dir ->
                            val onClick = remember(dir.name) { { onDirectoryClick(dir.name) } }
                            DirectoryCard(directory = dir, onClick = onClick)
                        }

                        if (otherDirs.isNotEmpty()) {
                            item(
                                key = "header_more_albums",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                Text(
                                    text = "更多相册",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                )
                            }
                        }

                        items(otherDirs, key = { it.name }) { dir ->
                            val onClick = remember(dir.name) { { onDirectoryClick(dir.name) } }
                            DirectoryCard(directory = dir, onClick = onClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectoryCard(
    directory: DirectoryGroup,
    onClick: () -> Unit
) {
    val icon = directoryIcon(directory.name)
    val color = directoryColor(directory.name)
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val context = LocalContext.current
    val firstItem = directory.items.firstOrNull()
    val thumbnailUri = directory.thumbnailUri
    val isVideo = firstItem?.isVideo == true

    var videoThumbnail by remember { mutableStateOf<Bitmap?>(null) }
    if (isVideo && thumbnailUri != null) {
        LaunchedEffect(thumbnailUri) {
            videoThumbnail = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.loadThumbnail(
                        thumbnailUri,
                        android.util.Size(512, 512),
                        null
                    )
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = interactionSource
            )
    ) {
        Surface(
            shape = shape,
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(modifier = Modifier.aspectRatio(1f)) {
                when {
                    isVideo && videoThumbnail != null -> {
                        Image(
                            bitmap = videoThumbnail!!.asImageBitmap(),
                            contentDescription = directory.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    isVideo && videoThumbnail == null -> {
                        DirectoryPlaceholder(icon = icon, color = color)
                    }
                    thumbnailUri != null -> {
                        SubcomposeAsyncImage(
                            model = thumbnailUri,
                            contentDescription = directory.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape),
                            contentScale = ContentScale.Crop,
                            error = {
                                DirectoryPlaceholder(icon = icon, color = color)
                            }
                        )
                    }
                    else -> {
                        DirectoryPlaceholder(icon = icon, color = color)
                    }
                }

                if (isPressed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                    )
                }
            }
        }

        Text(
            text = directory.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp)
        )
        Text(
            text = "${directory.count}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp, end = 2.dp)
        )
    }
}

@Composable
private fun DirectoryPlaceholder(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = color
        )
    }
}

private fun directoryIcon(name: String): ImageVector = when (name) {
    "全部" -> Icons.AutoMirrored.Filled.List
    "相机" -> Icons.Default.CameraAlt
    "视频" -> Icons.Default.Videocam
    "收藏" -> Icons.Default.Star
    "Raw" -> Icons.Default.Image
    else -> Icons.Default.Folder
}

@Composable
private fun directoryColor(name: String): Color = when (name) {
    "全部" -> MaterialTheme.colorScheme.primary
    "相机" -> Color(0xFFE91E63)
    "视频" -> MaterialTheme.colorScheme.secondary
    "收藏" -> MaterialTheme.colorScheme.tertiary
    "Raw" -> Color(0xFF4CAF50)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
