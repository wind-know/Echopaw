package com.example.echopaw.callback

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.example.echopaw.R

class WorldActivity : Activity() {
    private lateinit var glSurfaceView: EarthGLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化自定义GLSurfaceView（保持原有逻辑）
        glSurfaceView = EarthGLSurfaceView(this)
        setContentView(glSurfaceView)
    }

    // 新增：创建菜单
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_earth, menu)
        return true
    }

    // 新增：处理菜单点击
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // 原有：切换自动旋转
            R.id.action_toggle_rotation -> {
                glSurfaceView.queueEvent {
                    glSurfaceView.renderer.toggleAutoRotation()
                }
                glSurfaceView.requestRender()
                true
            }
            // 新增：定位到中国
            R.id.action_locate_china -> {
                glSurfaceView.queueEvent {
                    glSurfaceView.renderer.locateToChina()
                }
                glSurfaceView.requestRender()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 保留原有生命周期方法
    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }
}