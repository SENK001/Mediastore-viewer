package com.senk.mediastoreviewer.ui.glview

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class GLThumbnailLoader(private val contentResolver: ContentResolver) {

    suspend fun loadThumbnail(uri: Uri, targetSize: Int = 256): Bitmap? = withContext(Dispatchers.IO) {
        try {
            contentResolver.loadThumbnail(uri, Size(targetSize, targetSize), null)
        } catch (_: Exception) {
            null
        }
    }
}
