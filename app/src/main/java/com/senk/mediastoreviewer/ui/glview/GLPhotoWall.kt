package com.senk.mediastoreviewer.ui.glview

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.senk.mediastoreviewer.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

private fun centerCropSquare(bitmap: Bitmap): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w == h) return bitmap
    val size = min(w, h)
    val x = (w - size) / 2
    val y = (h - size) / 2
    val cropped = Bitmap.createBitmap(bitmap, x, y, size, size)
    bitmap.recycle()
    return cropped
}

@Composable
fun GLPhotoWall(
    items: List<MediaItem>,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    val context = LocalContext.current

    val textureManager = remember { GLTextureManager() }
    val thumbnailLoader = remember { GLThumbnailLoader(context.contentResolver) }
    val loadChannel = remember { Channel<Long>(Channel.UNLIMITED) }

    val renderer = remember {
        GLPhotoWallRenderer(context, textureManager).apply {
            this.columns = columns
            this.items = items
            onItemClickListener = onItemClick
            onRequestLoad = { itemId -> loadChannel.trySend(itemId) }
        }
    }

    renderer.items = items
    renderer.columns = columns
    renderer.onItemClickListener = onItemClick

    AndroidView(
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(2)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

                renderer.requestRenderAction = { requestRender() }
                renderer.queueEventAction = { r -> queueEvent(r) }

                setOnTouchListener { _, event ->
                    val handled = renderer.handleTouchEvent(event)
                    if (handled) requestRender()
                    handled
                }
            }
        },
        modifier = modifier,
        onRelease = { glView ->
            glView.queueEvent {
                textureManager.clear()
            }
        }
    )

    LaunchedEffect(Unit) {
        for (itemId in loadChannel) {
            val item = renderer.items.find { it.id == itemId } ?: continue
            launch {
                val bitmap = withContext(Dispatchers.IO) {
                    val raw = thumbnailLoader.loadThumbnail(item.uri)
                    raw?.let { centerCropSquare(it) }
                }
                if (bitmap != null) {
                    renderer.queueEvent {
                        textureManager.put(itemId, bitmap)
                        bitmap.recycle()
                        renderer.loadCompleted(itemId)
                        renderer.requestRender()
                    }
                } else {
                    renderer.loadCompleted(itemId)
                }
            }
        }
    }
}
