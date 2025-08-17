package com.example.echopaw.home


import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.echopaw.R

class RecordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)
        // 如果第一次进入，添加 RecordFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, RecordFragment())
                .commit()
        }
        setupStatusBar()
    }
    private fun setupStatusBar() {
        // Android 5.0（API 21）及以上支持设置状态栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.transparent)
            // Android 6.0（API 23）及以上支持设置状态栏文字颜色（深色/浅色）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 如果状态栏背景是浅色（如透明），建议将文字设置为深色，避免看不清
                // 反之，若背景是深色，可设置为浅色文字（View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 不设置即可）
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
}
