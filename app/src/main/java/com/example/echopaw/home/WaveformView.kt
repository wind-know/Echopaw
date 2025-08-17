package com.example.echopaw.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT // 透明背景，可根据需求修改
        style = Paint.Style.FILL
    }
    private val waveformData = mutableListOf<Float>()
    private val maxPoints = 80  // 减少点数，增大间距
    private val barWidthRatio = 0.4f  // 减小比例，让竖条更窄
    private val spacingRatio = 0.6f   // 间距比例，控制竖条间空隙

    // 添加新的振幅点
    fun addAmplitude(amplitude: Float) {
        if (waveformData.size >= maxPoints) {
            waveformData.removeAt(0)
        }
        waveformData.add(amplitude)
        invalidate()
    }

    // 更新波形列表
    fun updateWaveform(data: List<Float>) {
        waveformData.clear()
        waveformData.addAll(data.takeLast(maxPoints))
        invalidate()
    }

    // 清空波形
    fun clear() {
        waveformData.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (waveformData.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2f  // 水平中心轴
        val barCount = waveformData.size
        // 单格宽度（包含竖条和间距）
        val singleBarWidth = width / barCount
        // 竖条实际宽度
        val barActualWidth = singleBarWidth * barWidthRatio
        // 竖条之间的间距
        val spacing = singleBarWidth * spacingRatio

        // 绘制背景（无虚影，透明背景可根据需求修改颜色）
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        waveformData.forEachIndexed { index, amplitude ->
            val startX = index * singleBarWidth
            // 计算竖条左右坐标，预留间距
            val left = startX + spacing / 2
            val right = startX + singleBarWidth - spacing / 2

            // 振幅映射到 0~centerY（上下对称）
            val barHalfHeight = amplitude.coerceIn(0f, centerY)

            // 绘制上半部分竖条
            canvas.drawRect(
                left,
                centerY - barHalfHeight,
                right,
                centerY,
                barPaint
            )
            // 绘制下半部分竖条（轴对称）
            canvas.drawRect(
                left,
                centerY,
                right,
                centerY + barHalfHeight,
                barPaint
            )
        }
    }
}