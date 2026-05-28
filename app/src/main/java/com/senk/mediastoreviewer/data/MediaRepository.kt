package com.senk.mediastoreviewer.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(private val contentResolver: ContentResolver) {

    suspend fun queryAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.IS_FAVORITE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
        )

        queryImages(items, projection)
        queryVideos(items, projection)

        items.sortByDescending { it.id }
        items
    }

    private fun queryImages(items: MutableList<MediaItem>, projection: Array<String>) {
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null,
            null
        )?.use { cursor ->
            items.addAll(readCursor(cursor, false))
        }
    }

    private fun queryVideos(items: MutableList<MediaItem>, projection: Array<String>) {
        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null,
            null
        )?.use { cursor ->
            items.addAll(readCursor(cursor, true))
        }
    }

    private fun readCursor(cursor: Cursor, isVideo: Boolean): List<MediaItem> {
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        val favoriteCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_FAVORITE)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)

        val result = mutableListOf<MediaItem>()
        val baseUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                      else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            result.add(
                MediaItem(
                    id = id,
                    displayName = cursor.getString(displayNameCol) ?: "",
                    bucketDisplayName = cursor.getString(bucketCol) ?: "Unknown",
                    isFavorite = cursor.getInt(favoriteCol) == 1,
                    mimeType = cursor.getString(mimeCol) ?: "",
                    size = cursor.getLong(sizeCol),
                    isVideo = isVideo,
                    uri = ContentUris.withAppendedId(baseUri, id)
                )
            )
        }
        return result
    }

    @Immutable
    data class FieldEntry(val name: String, val value: String)

    suspend fun queryItemFields(id: Long, isVideo: Boolean): List<FieldEntry> = withContext(Dispatchers.IO) {
        val baseUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                      else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = ContentUris.withAppendedId(baseUri, id)

        val fields = mutableListOf<FieldEntry>()
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                for (i in 0 until cursor.columnCount) {
                    val name = cursor.getColumnName(i)
                    val value = when (cursor.getType(i)) {
                        Cursor.FIELD_TYPE_NULL -> "null"
                        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i).toString()
                        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i).toString()
                        Cursor.FIELD_TYPE_STRING -> cursor.getString(i) ?: "null"
                        Cursor.FIELD_TYPE_BLOB -> "[BLOB]"
                        else -> "[UNKNOWN]"
                    }
                    fields.add(FieldEntry(name, value))
                }
            }
        }
        fields
    }
}
