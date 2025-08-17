package com.example.echopaw.home

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordViewModel : ViewModel() {
    // 录音状态
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    // 波形数据
    private val _waveformData = MutableStateFlow<List<Float>>(emptyList())
    val waveformData: StateFlow<List<Float>> = _waveformData.asStateFlow()

    // 录音时长
    private val _recordingTime = MutableStateFlow("00:00.00")
    val recordingTime: StateFlow<String> = _recordingTime.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var recordingJob: Job? = null
    private var waveformJob: Job? = null
    private var currentFile: File? = null
    private var startTime: Long = 0

    // 录音状态密封类
    sealed class RecordingState {
        object Idle : RecordingState()
        object Recording : RecordingState()
        data class Error(val message: String) : RecordingState()
        data class Completed(val file: File, val duration: Long) : RecordingState()
    }
    // 封装一个创建 MediaRecorder 的方法
    fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31（Android 12）及以上使用新构造方法
            MediaRecorder()
        } else {
            // 低版本使用旧的无参构造（虽然标记过时，但低版本可用）
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
    // 开始录音
//    fun startRecording(context: Context) {
//        if (_recordingState.value is RecordingState.Recording) return
//
//        try {
//            // 创建录音文件
//            currentFile = createRecordingFile(context)
//
//            // 初始化MediaRecorder
//            mediaRecorder = createMediaRecorder().apply {
//                setAudioSource(MediaRecorder.AudioSource.MIC)
//                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
//                setOutputFile(createTempFile().absolutePath)
//                prepare()
//                start()
//            }
//
//            startTime = System.currentTimeMillis()
//            _recordingState.value = RecordingState.Recording
//
//            // 启动计时器
//            startRecordingTimer()
//
//            // 启动波形数据采集
//            startWaveformCollection()
//
//        } catch (e: Exception) {
//            _recordingState.value = RecordingState.Error("录音启动失败: ${e.message ?: "未知错误"}")
//            cleanUpRecording()
//        }
//    }

    // 停止录音
    fun stopRecording() {
        if (_recordingState.value !is RecordingState.Recording) return

        try {
            mediaRecorder?.stop()
            val duration = System.currentTimeMillis() - startTime
            _recordingState.value = RecordingState.Completed(currentFile!!, duration)
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error("录音停止失败: ${e.message ?: "未知错误"}")
        } finally {
            cleanUpRecording()
        }
    }

    // 创建录音文件
    private fun createRecordingFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return File.createTempFile(
            "REC_${timeStamp}_",
            ".mp4",
            storageDir
        )
    }

    // 启动录音计时器
    private fun startRecordingTimer() {
        recordingJob =  viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                _recordingTime.value = formatTime(elapsed)
                delay(50) // 每50ms更新一次
            }
        }
    }

//    private fun startWaveformCollection() {
//        waveformJob = viewModelScope.launch(Dispatchers.IO) {
//            val maxPoints = 50 // 波形点数
//            val waveformList = MutableList(maxPoints) { 0f } // 初始化全 0
//
//            while (isActive && mediaRecorder != null) {
//                try {
//                    val rawAmplitude = mediaRecorder?.maxAmplitude?.toFloat() ?: 0f
//                    val amplitude = (rawAmplitude / 32767f * 50f).coerceIn(0f, 50f)
//
//                    // 滚动波形：移除最左边的点，加入新振幅
//                    waveformList.removeAt(0)
//                    waveformList.add(amplitude * (0.5f + Math.random().toFloat() * 0.5f)) // 微抖动
//
//                    _waveformData.value = waveformList.toList() // 发送给 UI
//                    Log.d("Waveform", "Amplitude: $amplitude")
//                } catch (e: Exception) {
//                    _waveformData.value = List(maxPoints) { 0f }
//                }
//                delay(50)
//            }
//        }
//    }
    fun startRecording(context: Context) {
        if (_recordingState.value is RecordingState.Recording) return

        try {
            // 创建录音文件
            val file = createRecordingFile(context)
            currentFile = file

            // 初始化 MediaRecorder
            mediaRecorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath) // 重要：使用可写文件路径
                prepare()
                start()
            }

            Log.d("Waveform", "Recording started: ${file.absolutePath}")

            startTime = System.currentTimeMillis()
            _recordingState.value = RecordingState.Recording

            // 启动计时器
            startRecordingTimer()

            // 启动波形数据采集
            startWaveformCollection()

        } catch (e: Exception) {
            Log.e("Waveform", "Recording failed: ${e.message}")
            _recordingState.value = RecordingState.Error("录音启动失败: ${e.message ?: "未知错误"}")
            cleanUpRecording()
        }
    }
//    private fun startWaveformCollection() {
//        waveformJob = viewModelScope.launch(Dispatchers.IO) {
//            val maxPoints = 50
//            val waveformList = MutableList(maxPoints) { 0f }
//
//            delay(500) // 给 MediaRecorder 启动缓冲时间
//
//            while (isActive && mediaRecorder != null) {
//                try {
//                    var rawAmplitude = mediaRecorder?.maxAmplitude?.toFloat() ?: 0f
//                    if (rawAmplitude == 0f) rawAmplitude = 1f // 防止完全为 0
//
//                    val amplitude = (rawAmplitude / 32767f * 100f).coerceIn(0f, 100f)
//
//                    waveformList.removeAt(0)
//                    waveformList.add(amplitude)
//
//                    _waveformData.value = waveformList.toList()
//                } catch (e: Exception) {
//                    _waveformData.value = List(maxPoints) { 0f }
//                }
//                delay(50)
//            }
//        }
//    }

//
    private fun startWaveformCollection() {
        waveformJob = viewModelScope.launch(Dispatchers.Main) { // 主线程更新 UI
            val maxPoints = 50
            val waveformList = MutableList(maxPoints) { 0f }

            while (isActive && mediaRecorder != null) {
                try {
                    // 获取原始振幅
                    val rawAmplitude = mediaRecorder?.maxAmplitude?.toFloat() ?: 0f

                    val amplitude = if (rawAmplitude > 0f) {
                        (rawAmplitude / 32767f * 150f).coerceIn(0f, 150f) // 放大到150，波动更大
                    } else {
                        5f + Math.random().toFloat() * 30f // 没有声音时也给点随机波动
                    }
                    // 滚动波形
                    waveformList.removeAt(0)
                    waveformList.add(amplitude)
                    // 更新 StateFlow
                    _waveformData.value = waveformList.toList()
                    // 打印日志调试
                    Log.d("Waveform", "Amplitude raw: $rawAmplitude, scaled: $amplitude")
                } catch (e: Exception) {
                    Log.e("Waveform", "采集波形出错: ${e.message}")
                    _waveformData.value = List(maxPoints) { 0f }
                }
                delay(50) // 每50ms采样一次
            }
        }
    }



    // 格式化时间显示
    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / 60000) % 60
        val centis = (millis / 10) % 100
        return String.format("%02d:%02d.%02d", minutes, seconds, centis)
    }

    // 清理录音资源
    private fun cleanUpRecording() {
        recordingJob?.cancel()
        waveformJob?.cancel()
        mediaRecorder?.release()
        mediaRecorder = null
    }

    // 取消录音
    fun cancelRecording() {
        if (_recordingState.value is RecordingState.Recording) {
            mediaRecorder?.stop()
            currentFile?.delete() // 删除未完成的录音文件
        }
        cleanUpRecording()
        _recordingState.value = RecordingState.Idle
    }

    // ViewModel销毁时清理资源
    override fun onCleared() {
        super.onCleared()
        cleanUpRecording()
    }
}
