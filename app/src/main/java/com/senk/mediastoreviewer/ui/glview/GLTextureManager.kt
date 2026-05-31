package com.senk.mediastoreviewer.ui.glview

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils

internal class GLTextureManager(private val maxCacheSize: Int = 64) {

    private data class Entry(val textureId: Int, val itemId: Long)

    private val lru = object : LinkedHashMap<Long, Entry>(maxCacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Entry>?): Boolean {
            if (size > maxCacheSize && eldest != null) {
                deleteTexture(eldest.value.textureId)
                return true
            }
            return false
        }
    }

    @Synchronized
    fun get(itemId: Long): Int {
        val entry = lru[itemId]
        if (entry != null) {
            return entry.textureId
        }
        return 0
    }

    @Synchronized
    fun put(itemId: Long, bitmap: Bitmap): Int {
        evict(itemId)

        val textureId = uploadTexture(bitmap)
        if (textureId != 0) {
            lru[itemId] = Entry(textureId, itemId)
        }
        return textureId
    }

    @Synchronized
    fun evict(itemId: Long) {
        lru.remove(itemId)?.let { deleteTexture(it.textureId) }
    }

    @Synchronized
    fun clear() {
        lru.values.forEach { deleteTexture(it.textureId) }
        lru.clear()
    }

    fun deleteTexture(textureId: Int) {
        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
    }

    fun uploadTexture(bitmap: Bitmap): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val textureId = textures[0]
        if (textureId == 0) return 0

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        return textureId
    }
}
