package com.senk.mediastoreviewer.data

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class MediaItem(
    val id: Long,
    val displayName: String,
    val bucketDisplayName: String,
    val isFavorite: Boolean,
    val mimeType: String,
    val size: Long,
    val isVideo: Boolean,
    val uri: Uri
)
