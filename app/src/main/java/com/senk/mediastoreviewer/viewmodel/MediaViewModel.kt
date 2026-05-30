package com.senk.mediastoreviewer.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.senk.mediastoreviewer.data.MediaItem
import com.senk.mediastoreviewer.data.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class DirectoryGroup(
    val name: String,
    val items: List<MediaItem>,
    val isVirtual: Boolean = false
) {
    val count: Int get() = items.size
    val thumbnailUri: Uri? get() = items.firstOrNull()?.uri
}

class MediaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaRepository(application.contentResolver)

    private val _allItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val allItems: StateFlow<List<MediaItem>> = _allItems.asStateFlow()

    private val _directories = MutableStateFlow<List<DirectoryGroup>>(emptyList())
    val directories: StateFlow<List<DirectoryGroup>> = _directories.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val items = repository.queryAllMedia()
                _allItems.value = items
                _directories.value = buildDirectories(items)
            } catch (e: SecurityException) {
                _error.value = "缺少媒体读取权限，请在设置中授权"
            } catch (e: Exception) {
                _error.value = "加载失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getItemsForDirectory(name: String): List<MediaItem> {
        return _directories.value
            .find { it.name == name }
            ?.items ?: emptyList()
    }

    fun getItemById(id: Long): MediaItem? {
        return _allItems.value.find { it.id == id }
    }

    fun queryItemFields(id: Long, isVideo: Boolean, callback: (List<MediaRepository.FieldEntry>) -> Unit) {
        viewModelScope.launch {
            try {
                val fields = repository.queryItemFields(id, isVideo)
                callback(fields)
            } catch (e: Exception) {
                callback(emptyList())
            }
        }
    }

    private fun buildDirectories(items: List<MediaItem>): List<DirectoryGroup> {
        if (items.isEmpty()) return emptyList()
        val dirs = mutableListOf<DirectoryGroup>()
        val usedBucketNames = mutableSetOf<String>()

        dirs.add(DirectoryGroup("全部", items, isVirtual = true))

        val cameraItems = findBucketItems(items, setOf("camera"), usedBucketNames)
        if (cameraItems != null) {
            dirs.add(DirectoryGroup("相机", cameraItems))
        }

        val videoItems = items.filter { it.isVideo }
        if (videoItems.isNotEmpty()) {
            dirs.add(DirectoryGroup("视频", videoItems, isVirtual = true))
        }

        val favorites = items.filter { it.isFavorite }
        if (favorites.isNotEmpty()) {
            dirs.add(DirectoryGroup("收藏", favorites, isVirtual = true))
        }

        val rawItems = findBucketItems(items, setOf("raw"), usedBucketNames)
        if (rawItems != null) {
            dirs.add(DirectoryGroup("Raw", rawItems))
        }

        val bucketMap = linkedMapOf<String, MutableList<MediaItem>>()
        for (item in items) {
            if (item.bucketDisplayName !in usedBucketNames) {
                bucketMap.getOrPut(item.bucketDisplayName) { mutableListOf() }.add(item)
            }
        }

        bucketMap.entries
            .sortedByDescending { it.value.size }
            .forEach { (name, bucketItems) ->
                dirs.add(DirectoryGroup(name, bucketItems))
            }

        return dirs
    }

    private fun findBucketItems(
        items: List<MediaItem>,
        keywords: Set<String>,
        used: MutableSet<String>
    ): List<MediaItem>? {
        val bucketName = items.map { it.bucketDisplayName }
            .distinct()
            .firstOrNull { name ->
                keywords.any { kw -> name.contains(kw, ignoreCase = true) }
            } ?: return null
        used.add(bucketName)
        return items.filter { it.bucketDisplayName == bucketName }
    }
}
