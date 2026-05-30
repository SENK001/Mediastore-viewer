package com.senk.mediastoreviewer.ui.screens

import android.app.Activity
import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.senk.mediastoreviewer.R
import com.senk.mediastoreviewer.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    itemId: Long,
    isVideo: Boolean,
    viewModel: MediaViewModel,
    onBack: () -> Unit,
    onNavigateToDetail: () -> Unit
) {
    val item = viewModel.getItemById(itemId)
    val view = LocalView.current
    var isImmersive by remember { mutableStateOf(false) }
    val initialLightStatusBars = remember {
        WindowInsetsControllerCompat(
            (view.context as Activity).window,
            view
        ).isAppearanceLightStatusBars
    }

    DisposableEffect(isImmersive) {
        val controller = WindowInsetsControllerCompat(
            (view.context as Activity).window,
            view
        )
        if (isImmersive) {
            controller.isAppearanceLightStatusBars = false
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.isAppearanceLightStatusBars = true
        }
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.isAppearanceLightStatusBars = initialLightStatusBars
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isImmersive) Color.Black else Color.White)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (item != null) {
                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { isImmersive = !isImmersive }
                                )
                            }
                    ) {
                        VideoPlayer(uri = item.uri)
                    }
                } else {
                    ZoomableImage(
                        uri = item.uri,
                        onToggleImmersive = { isImmersive = !isImmersive }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isImmersive,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = item?.displayName ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDetail) {
                        Icon(Icons.Outlined.Info, contentDescription = "查看详情")
                    }
                }
            )
        }
    }
}

@Composable
private fun ZoomableImage(uri: Uri, onToggleImmersive: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    if (newScale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                    scale = newScale
                    if (scale <= 1f) {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleImmersive() },
                    onDoubleTap = {
                        if (scale > 1.5f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun VideoPlayer(uri: Uri) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            val playerView = LayoutInflater.from(ctx)
                .inflate(R.layout.player_view, null) as PlayerView
            playerView.player = exoPlayer
            playerView.useController = true
            playerView
        },
        modifier = Modifier.fillMaxSize()
    )
}
