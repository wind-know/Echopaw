package com.example.echopaw.callback

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector

class EarthGLSurfaceView(context: Context) : GLSurfaceView(context) {
    // 暴露renderer为public，供Activity调用
    val renderer: EarthRenderer  // 仅修改此处：将private改为val（保持可访问性）
    private val gestureDetector: GestureDetector
    private val scaleDetector: ScaleGestureDetector
    private var previousX: Float = 0f
    private var previousY: Float = 0f
    private var isDragging: Boolean = false
    private var scaleFactor: Float = 1f

    init {
        // 关键修改1：配置EGL支持透明（必须在setRenderer前设置）
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)  // 8位RGBA + 16位深度缓冲
        setZOrderOnTop(true)  // 将GLSurfaceView置于顶层，确保透明区域显示下层内容
        holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)  // 设置为透明格式

        setEGLContextClientVersion(2)
        renderer = EarthRenderer(context)  // 初始化保持不变
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY

        // 保留原有手势处理逻辑...
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e1 != null) {
                    handleDrag(e2.x, e2.y)
                }
                return true
            }
        })

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 3.0f))
                queueEvent {
                    renderer.setScale(scaleFactor)
                }
                requestRender()
                return true
            }
        })

        setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            if (!scaleDetector.isInProgress) {
                gestureDetector.onTouchEvent(event)
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    previousX = event.x
                    previousY = event.y
                    isDragging = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                }
            }
            true
        }
    }

    private fun handleDrag(currentX: Float, currentY: Float) {
        val deltaX = currentX - previousX
        val deltaY = currentY - previousY
        queueEvent {
            renderer.addRotation(deltaY * 0.1f, deltaX * 0.1f)
        }
        previousX = currentX
        previousY = currentY
        requestRender()
    }
}