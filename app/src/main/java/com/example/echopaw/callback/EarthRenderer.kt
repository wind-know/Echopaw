package com.example.echopaw.callback

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.echopaw.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class EarthRenderer(private val context: Context) : GLSurfaceView.Renderer {
    // 保留所有原有变量...
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)
    private lateinit var earth: Sphere
    private var textureId: Int = 0
    @Volatile
    private var xRotation: Float = 0f
    @Volatile
    private var yRotation: Float = 0f
    @Volatile
    private var scale: Float = 1f
    private var autoRotate: Boolean = true
    private val rotationSpeed: Float = 0.3f


    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
//        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 关键修改：将清屏颜色的Alpha值设为0（透明）
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)  // 最后一个参数0.0表示完全透明

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        textureId = TextureHelper.loadTexture(context, R.drawable.earth_texture)
        earth = Sphere(0.8f, 128, 128, textureId)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (autoRotate) {
            yRotation += rotationSpeed
        }
        Matrix.setIdentityM(rotationMatrix, 0)
        Matrix.rotateM(rotationMatrix, 0, xRotation, 1f, 0f, 0f)
        Matrix.rotateM(rotationMatrix, 0, yRotation, 0f, 1f, 0f)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, rotationMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        val scaledMatrix = FloatArray(16)
        Matrix.scaleM(scaledMatrix, 0, mvpMatrix, 0, scale, scale, scale)
        earth.draw(scaledMatrix)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 10f)
    }


    fun addRotation(deltaX: Float, deltaY: Float) {
        xRotation += deltaX
        yRotation += deltaY
        if (xRotation > 90) xRotation = 90f
        if (xRotation < -90) xRotation = -90f
    }

    fun setScale(scale: Float) {
        this.scale = scale
    }

    fun toggleAutoRotation() {
        autoRotate = !autoRotate
    }

    // 新增：定位到中国的方法（优化参数，确保中国区域居中）
    fun locateToChina() {
        xRotation = 30f  // 中国区域上下角度（北半球偏上）
        yRotation = -60f // 中国区域左右角度（亚洲东部居中）
        scale = 1.5f     // 适合查看中国区域的缩放比例
    }

    companion object {
        fun loadShader(type: Int, shaderCode: String): Int {
            return GLES20.glCreateShader(type).apply {
                GLES20.glShaderSource(this, shaderCode)
                GLES20.glCompileShader(this)
                val compileStatus = IntArray(1)
                GLES20.glGetShaderiv(this, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
                if (compileStatus[0] == 0) {
                    val errorMsg = GLES20.glGetShaderInfoLog(this)
                    GLES20.glDeleteShader(this)
                    throw RuntimeException("着色器编译失败: $errorMsg")
                }
            }
        }
    }
}