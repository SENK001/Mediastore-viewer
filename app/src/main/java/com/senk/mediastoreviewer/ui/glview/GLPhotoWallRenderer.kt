package com.senk.mediastoreviewer.ui.glview

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.OverScroller
import com.senk.mediastoreviewer.data.MediaItem
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

internal class GLPhotoWallRenderer(
    context: Context,
    private val textureManager: GLTextureManager
) : GLSurfaceView.Renderer {

    companion object {
        private const val GAP = 4f
        private const val VERTICES_PER_QUAD = 4
        private const val COORDS_PER_VERTEX = 3
        private const val TEX_COORDS_PER_VERTEX = 2
        private val FULL_TEX_COORDS = floatArrayOf(0f, 0f, 0f, 1f, 1f, 0f, 1f, 1f)
    }

    var items: List<MediaItem> = emptyList()
    var columns: Int = 3
    var onItemClickListener: ((Long) -> Unit)? = null
    var onRequestLoad: ((Long) -> Unit)? = null
    var requestRenderAction: (() -> Unit)? = null
    var queueEventAction: ((Runnable) -> Unit)? = null

    fun requestRender() {
        requestRenderAction?.invoke()
    }

    fun queueEvent(runnable: Runnable) {
        queueEventAction?.invoke(runnable)
    }

    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0
    private var cellSize: Float = 0f
    private var scrollY: Float = 0f
    private var maxScrollY: Float = 0f

    private var program: Int = 0
    private var mvpMatrixHandle: Int = 0
    private var positionHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var textureHandle: Int = 0
    private var useTextureHandle: Int = 0
    private var colorHandle: Int = 0

    private val projectionMatrix = FloatArray(16)

    private val scroller = OverScroller(context)
    private var velocityTracker: VelocityTracker? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var isScrolling = false
    private var lastTouchY = 0f

    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(VERTICES_PER_QUAD * COORDS_PER_VERTEX * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(VERTICES_PER_QUAD * TEX_COORDS_PER_VERTEX * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()

    private val requestedLoads = Collections.newSetFromMap(ConcurrentHashMap<Long, Boolean>())

    init {
        texCoordBuffer.put(FULL_TEX_COORDS)
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                lastTouchY = event.y
                isScrolling = false
                scroller.forceFinished(true)
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(event)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                val dy = lastTouchY - event.y
                val dx = event.x - downX

                if (!isScrolling && (abs(dy) > touchSlop || abs(dx) > touchSlop)) {
                    isScrolling = true
                }

                if (isScrolling) {
                    scrollY = (scrollY + dy).coerceIn(0f, maxScrollY)
                    lastTouchY = event.y
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)
                val vy = velocityTracker?.yVelocity ?: 0f
                velocityTracker?.recycle()
                velocityTracker = null

                if (!isScrolling) {
                    val tappedItemId = hitTest(event.x, event.y + scrollY)
                    if (tappedItemId != null) {
                        onItemClickListener?.invoke(tappedItemId)
                    }
                } else {
                    scroller.fling(
                        0, scrollY.roundToInt(),
                        0, -vy.toInt(),
                        0, 0,
                        0, maxScrollY.roundToInt(),
                        0, 0
                    )
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.recycle()
                velocityTracker = null
                isScrolling = false
                return true
            }
        }
        return false
    }

    fun visibleItemIds(): List<Long> {
        val list = items
        if (list.isEmpty() || surfaceHeight <= 0) return emptyList()

        val firstRow = (scrollY / (cellSize + GAP)).toInt().coerceAtLeast(0)
        val lastRow = ((scrollY + surfaceHeight) / (cellSize + GAP)).toInt().coerceAtMost(rowCount(list) - 1)

        val ids = mutableListOf<Long>()
        for (row in firstRow..lastRow) {
            for (col in 0 until columns) {
                val index = row * columns + col
                if (index < list.size) {
                    ids.add(list[index].id)
                }
            }
        }
        return ids
    }

    fun loadCompleted(itemId: Long) {
        requestedLoads.remove(itemId)
    }

    // ── Renderer callbacks ──────────────────────────────────────────

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(1f, 1f, 1f, 1f)
        program = GLShaders.createProgram()

        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        useTextureHandle = GLES20.glGetUniformLocation(program, "uUseTexture")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        cellSize = (width - GAP * (columns - 1)) / columns
        val rows = rowCount(items)
        maxScrollY = max(0f, rows * (cellSize + GAP) - height)
        Matrix.orthoM(projectionMatrix, 0, 0f, width.toFloat(), height.toFloat(), 0f, -1f, 1f)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        val list = items
        if (list.isEmpty() || surfaceWidth <= 0) return

        updateScroller()
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, projectionMatrix, 0)

        val firstRow = (scrollY / (cellSize + GAP)).toInt().coerceAtLeast(0)
        val lastRow = ((scrollY + surfaceHeight + cellSize + GAP - 1) / (cellSize + GAP)).toInt()
            .coerceAtMost(rowCount(list) - 1)

        for (row in firstRow..lastRow) {
            val top = row * (cellSize + GAP) - scrollY
            if (top + cellSize < 0 || top > surfaceHeight) continue

            for (col in 0 until columns) {
                val index = row * columns + col
                if (index >= list.size) break

                val item = list[index]
                val left = col * (cellSize + GAP)
                val textureId = textureManager.get(item.id)

                if (textureId != 0) {
                    drawTexturedQuad(left, top, cellSize, textureId)
                } else {
                    drawPlaceholderQuad(left, top, cellSize)
                    if (requestedLoads.add(item.id)) {
                        onRequestLoad?.invoke(item.id)
                    }
                }
            }
        }

        GLES20.glUseProgram(0)
    }

    // ── Private helpers ─────────────────────────────────────────────

    private fun updateScroller() {
        if (scroller.computeScrollOffset()) {
            scrollY = scroller.currY.toFloat().coerceIn(0f, maxScrollY)
            requestRender()
        }
    }

    private fun drawTexturedQuad(x: Float, y: Float, size: Float, textureId: Int) {
        setQuadVertices(x, y, size)

        GLES20.glUniform1f(useTextureHandle, 1f)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureHandle, 0)

        bindAttributesAndDraw()
    }

    private fun drawPlaceholderQuad(x: Float, y: Float, size: Float) {
        setQuadVertices(x, y, size)

        GLES20.glUniform1f(useTextureHandle, 0f)
        GLES20.glUniform4f(colorHandle, 0.90f, 0.90f, 0.90f, 1f)

        bindAttributesAndDraw()
    }

    private fun setQuadVertices(x: Float, y: Float, size: Float) {
        vertexBuffer.clear()
        vertexBuffer.put(
            floatArrayOf(
                x, y, 0f,
                x, y + size, 0f,
                x + size, y, 0f,
                x + size, y + size, 0f,
            )
        )
    }

    private fun bindAttributesAndDraw() {
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)

        texCoordBuffer.position(0)
        GLES20.glVertexAttribPointer(texCoordHandle, TEX_COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTICES_PER_QUAD)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun hitTest(touchX: Float, touchY: Float): Long? {
        val list = items
        if (list.isEmpty()) return null

        val col = (touchX / (cellSize + GAP)).toInt()
        if (col < 0 || col >= columns) return null

        val row = (touchY / (cellSize + GAP)).toInt()
        if (row < 0) return null

        val cellX = col * (cellSize + GAP)
        val cellY = row * (cellSize + GAP)
        if (touchX < cellX || touchX > cellX + cellSize || touchY < cellY || touchY > cellY + cellSize) {
            return null
        }

        val index = row * columns + col
        return if (index < list.size) list[index].id else null
    }

    private fun rowCount(list: List<MediaItem>): Int {
        if (list.isEmpty()) return 0
        return ceil(list.size.toFloat() / columns).toInt()
    }
}
