package com.example.echopaw.callback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils

object TextureHelper {
    // 加载纹理并返回纹理ID（必须在GL线程中调用）
    fun loadTexture(context: Context, resourceId: Int): Int {
        val textureObjectIds = IntArray(1)
        GLES20.glGenTextures(1, textureObjectIds, 0)

        if (textureObjectIds[0] == 0) {
            throw RuntimeException("无法生成纹理对象ID")
        }

        // 加载位图
        val options = BitmapFactory.Options()
        options.inScaled = false // 不进行缩放
        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

        if (bitmap == null) {
            GLES20.glDeleteTextures(1, textureObjectIds, 0)
            throw RuntimeException("无法加载纹理资源: $resourceId")
        }

        // 绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureObjectIds[0])

        // 设置纹理过滤参数
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR_MIPMAP_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )

        // 加载纹理数据
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        // 释放位图资源
        bitmap.recycle()

        // 生成MIP映射
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

        // 解除纹理绑定
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        return textureObjectIds[0]
    }
}
