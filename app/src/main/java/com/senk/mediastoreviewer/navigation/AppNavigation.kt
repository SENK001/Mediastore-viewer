package com.senk.mediastoreviewer.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.senk.mediastoreviewer.ui.screens.DirectoryListScreen
import com.senk.mediastoreviewer.ui.screens.FileDetailScreen
import com.senk.mediastoreviewer.ui.screens.MediaViewerScreen
import com.senk.mediastoreviewer.ui.screens.PhotoWallScreen
import com.senk.mediastoreviewer.viewmodel.MediaViewModel
import java.net.URLDecoder
import java.net.URLEncoder

object NavRoutes {
    const val DIRECTORY_LIST = "directory_list"
    const val FILE_DETAIL = "file_detail/{itemId}/{isVideo}"
    const val MEDIA_VIEWER = "media_viewer/{itemId}/{isVideo}"
    const val PHOTO_WALL = "photo_wall/{directoryName}"

    fun photoWall(directoryName: String): String {
        val encoded = URLEncoder.encode(directoryName, "UTF-8")
        return "photo_wall/$encoded"
    }

    fun fileDetail(itemId: Long, isVideo: Boolean): String {
        return "file_detail/$itemId/$isVideo"
    }

    fun mediaViewer(itemId: Long, isVideo: Boolean): String {
        return "media_viewer/$itemId/$isVideo"
    }
}

@Composable
fun AppNavigation(viewModel: MediaViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavRoutes.DIRECTORY_LIST) {
        composable(NavRoutes.DIRECTORY_LIST) {
            DirectoryListScreen(
                viewModel = viewModel,
                onDirectoryClick = { directoryName ->
                    navController.navigate(NavRoutes.photoWall(directoryName))
                }
            )
        }

        composable(
            route = NavRoutes.FILE_DETAIL,
            arguments = listOf(
                navArgument("itemId") { type = NavType.LongType },
                navArgument("isVideo") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
            val isVideo = backStackEntry.arguments?.getBoolean("isVideo") ?: false

            FileDetailScreen(
                itemId = itemId,
                isVideo = isVideo,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.PHOTO_WALL,
            arguments = listOf(navArgument("directoryName") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("directoryName") ?: ""
            val directoryName = URLDecoder.decode(encodedName, "UTF-8")

            PhotoWallScreen(
                directoryName = directoryName,
                viewModel = viewModel,
                onItemClick = { itemId, isVideo ->
                    navController.navigate(NavRoutes.mediaViewer(itemId, isVideo))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.MEDIA_VIEWER,
            arguments = listOf(
                navArgument("itemId") { type = NavType.LongType },
                navArgument("isVideo") { type = NavType.BoolType }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
            val isVideo = backStackEntry.arguments?.getBoolean("isVideo") ?: false

            MediaViewerScreen(
                itemId = itemId,
                isVideo = isVideo,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { navController.navigate(NavRoutes.fileDetail(itemId, isVideo)) }
            )
        }
    }
}
