package com.example.echopaw.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.echopaw.R
import com.example.echopaw.databinding.FragmentRecordBinding
import kotlinx.coroutines.launch

class RecordFragment : Fragment() {

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecordViewModel by viewModels()

    private val REQUEST_RECORD_PERMISSIONS = 101
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()

        // 处理 EditText 的 hint 样式
        val editText: EditText = binding.inputBox
        val hintText = "输入分享你此刻的..."
        val spannableString = SpannableString(hintText)

        // 设置 hint 文字颜色，和布局中 textColorHint 一致
        spannableString.setSpan(
            ForegroundColorSpan(0x80FFFFFF.toInt()),
            0,
            hintText.length,
            0
        )
        // 放大 hint 文字，比如放大为原来的 1.5 倍，可按需调整
        spannableString.setSpan(
            RelativeSizeSpan(1.5f),
            0,
            hintText.length,
            0
        )
        // 可选：设置为粗体
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            hintText.length,
            0
        )
        editText.hint = spannableString

        binding.recordBtn.setOnClickListener {
            when (viewModel.recordingState.value) {
                is RecordViewModel.RecordingState.Recording -> viewModel.stopRecording()
                else -> startRecording()
            }
        }
    }

    private fun setupViews() {
        binding.bubble.post {
            binding.bubble.pivotX = binding.bubble.width / 2f
            binding.bubble.pivotY = binding.bubble.height / 2f
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recordingState.collect { state ->
                        updateUIState(state)
                    }
                }
                launch {
                    viewModel.recordingTime.collect { time ->
                        binding.recordingTime.text = time
                    }
                }
                launch {
                    viewModel.waveformData.collect { data ->
                        binding.waveform.updateWaveform(data)
                    }
                }
            }
        }
    }

    private fun startRecording() {
        if (hasRequiredPermissions()) {
            viewModel.startRecording(requireContext())
        } else {
            requestPermissions(requiredPermissions, REQUEST_RECORD_PERMISSIONS)
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startRecording()
            } else {
                Toast.makeText(
                    context,
                    "需要录音权限才能使用录音功能",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateUIState(state: RecordViewModel.RecordingState) {
        when (state) {
            is RecordViewModel.RecordingState.Recording -> {
                binding.bubble.visibility = View.VISIBLE
                binding.waveform.visibility = View.VISIBLE
                binding.recordingTime.visibility = View.VISIBLE
                binding.recordBtn.setImageResource(R.drawable.ic_microphoneing)
                binding.recordingStatus.text = "录音中"
                startBubbleAnimation()
            }
            is RecordViewModel.RecordingState.Idle,
            is RecordViewModel.RecordingState.Completed -> {
                binding.bubble.visibility = View.INVISIBLE
                binding.waveform.visibility = View.INVISIBLE
                binding.recordingTime.visibility = View.INVISIBLE
                binding.recordBtn.setImageResource(R.drawable.ic_microphone)
                binding.recordingStatus.text = "点击录音"
                binding.bubble.clearAnimation()
                if (state is RecordViewModel.RecordingState.Completed) {
                    Toast.makeText(
                        context,
                        "录音已保存: ${state.file.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            is RecordViewModel.RecordingState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                binding.bubble.visibility = View.INVISIBLE
                binding.waveform.visibility = View.INVISIBLE
                binding.recordingTime.visibility = View.INVISIBLE
                binding.recordBtn.setImageResource(R.drawable.ic_microphone)
                binding.recordingStatus.text = "点击录音"
                binding.bubble.clearAnimation()
            }
        }
    }

    private fun startBubbleAnimation() {
        binding.bubble.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(600)
            .withEndAction {
                binding.bubble.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(600)
                    .withEndAction { startBubbleAnimation() }
                    .start()
            }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (viewModel.recordingState.value is RecordViewModel.RecordingState.Recording) {
            viewModel.cancelRecording()
        }
        _binding = null
    }
}
