package com.senk.mediastoreviewer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.senk.mediastoreviewer.ui.glview.GLPhotoWall
import com.senk.mediastoreviewer.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoWallScreen(
    directoryName: String,
    viewModel: MediaViewModel,
    onItemClick: (Long, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val allItems by viewModel.allItems.collectAsState()

    val items = remember(allItems, directoryName) {
        viewModel.getItemsForDirectory(directoryName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(directoryName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("没有照片")
            }
        } else {
            GLPhotoWall(
                items = items,
                columns = 4,
                onItemClick = { itemId ->
                    val item = items.find { it.id == itemId }
                    if (item != null) {
                        onItemClick(itemId, item.isVideo)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}
