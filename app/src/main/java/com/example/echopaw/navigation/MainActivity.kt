package com.example.echopaw.navigation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Outline
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import com.example.echopaw.R
import com.example.echopaw.databinding.ActivityMainBinding
import com.example.echopaw.home.RecordActivity
import com.example.echopaw.navigation.contract.INavigationContract
import com.example.echopaw.navigation.model.NavigationInfo
import com.example.echopaw.navigation.model.NavigationModel
import com.example.echopaw.navigation.presenter.NavigationPresenter
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton

class MainActivity : AppCompatActivity(), INavigationContract.INavigationView {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mPresenter: INavigationContract.INavigationPresenter

    override fun setPresenter(presenter: INavigationContract.INavigationPresenter?) {
        presenter?.let { mPresenter = it }
    }

    private var floatingActionMenu: FloatingActionMenu? = null
    private var firstBackPressedTime: Long = 0
    private lateinit var fragmentManager: FragmentManager
    private lateinit var handler: Handler

    companion object {
        private const val DOUBLE_CLICK_TIME_DELAY = 2000L
        private const val ANIMATION_DELAY = 200L
        private const val SUB_BUTTON_SIZE_DP = 100
        // 动画时长优化
        private const val EXIT_ANIM_DURATION = 500L       // fcvNavigation 上滑
        private const val COORDINATOR_ANIM_DURATION = 500L // coordinatorLayout 下滑
        private const val RETURN_ANIM_DURATION = 700L     // 回弹动画稍慢
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handler = Handler(Looper.getMainLooper())
        setupWindowInsets()
        setupStatusBar()
        setupNavigation()
        setupBackPressHandler()
        setupFabBaseListener()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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

    private fun setupNavigation() {
        binding.bnvNavigation.itemIconTintList = null
        val navigationModel = NavigationModel()
        setPresenter(NavigationPresenter(navigationModel, this))
        mPresenter.getNavigationInfo("开！")
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleDoubleBackPress()
            }
        })
    }

    private fun handleDoubleBackPress() {
        val currentTime = System.currentTimeMillis()
        if (firstBackPressedTime == 0L) {
            firstBackPressedTime = currentTime
            showToast("再按一次退出")
        } else {
            if (currentTime - firstBackPressedTime < DOUBLE_CLICK_TIME_DELAY) {
                finish()
            } else {
                firstBackPressedTime = 0
                showToast("再按一次退出")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showNavigationInfomation(navigationInfos: List<NavigationInfo?>?) {
        fragmentManager = supportFragmentManager
        val initialTransaction = fragmentManager.beginTransaction()
        navigationInfos?.forEach { info ->
            info?.let {
                initialTransaction.add(binding.fcvNavigation.id, it.fragment)
                initialTransaction.hide(it.fragment)
            }
        }
        initialTransaction.setCustomAnimations(
            R.anim.slide_in_right,
            R.anim.slide_out_left,
            R.anim.slide_in_left,
            R.anim.slide_out_right
        ).commit()

        setupBottomNavigation(navigationInfos)
        if (floatingActionMenu == null) initFloatActionButton()
    }

    private fun setupBottomNavigation(navigationInfos: List<NavigationInfo?>?) {
        val nonNullInfos = navigationInfos?.filterNotNull() ?: emptyList()
        binding.bnvNavigation.setOnItemSelectedListener { menuItem ->
            handleMenuItemSelection(menuItem, nonNullInfos)
            true
        }
        if (binding.bnvNavigation.menu.size() > 0) {
            binding.bnvNavigation.selectedItemId = binding.bnvNavigation.menu.getItem(0).itemId
        }
    }

    private fun handleMenuItemSelection(menuItem: MenuItem, navigationInfos: List<NavigationInfo>) {
        showSelectedFragment(menuItem.title.toString(), navigationInfos)
        floatingActionMenu?.takeIf { it.isOpen }?.close(true)
    }

    private fun showSelectedFragment(title: String, navigationInfos: List<NavigationInfo>) {
        fragmentManager.beginTransaction().apply {
            navigationInfos.forEach { info ->
                if (title == info.fragmentName) show(info.fragment) else hide(info.fragment)
            }
            commit()
        }
    }

    private fun setupFabBaseListener() {
        binding.fabNavigation.setOnClickListener {
            startFabScaleAnimation(1f, 1.2f) // 放大
            handler.postDelayed({ startFabScaleAnimation(1.2f, 1f) }, 150L) // 缩回原始大小
            floatingActionMenu?.let { menu ->
                if (menu.isOpen) menu.close(true) else menu.open(true)
            } ?: run { initFloatActionButton() }
        }
    }

    private fun initFloatActionButton() {
        if (floatingActionMenu != null) return
        val builder = SubActionButton.Builder(this)


        val recordButton = createCircleImageSubActionButton(builder, R.drawable.ic_r_record) {
            handleActionButtonClick("心情录")
        }
        val bottleButton = createCircleImageSubActionButton(builder, R.drawable.ic_r_bottle) {
            handleActionButtonClick("留声瓶")
        }
        val wishingButton = createCircleImageSubActionButton(builder, R.drawable.ic_r_wishing) {
            handleActionButtonClick("许愿")
        }
        floatingActionMenu = FloatingActionMenu.Builder(this)
            .setStartAngle(220)
            .setEndAngle(320)
            .setRadius(dpToPx(120))
            .addSubActionView(wishingButton)
            .addSubActionView(recordButton)
            .addSubActionView(bottleButton)
            .attachTo(binding.fabNavigation)
            .build()
    }

    private fun createCircleImageSubActionButton(
        builder: SubActionButton.Builder,
        imageRes: Int,
        clickListener: () -> Unit
    ): SubActionButton {
        val imageView = ImageView(this).apply {
            setImageResource(imageRes)
            scaleType = ImageView.ScaleType.FIT_XY
            background = null
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            clipToOutline = true
        }
        return builder.setContentView(imageView)
            .setLayoutParams(FrameLayout.LayoutParams(dpToPx(SUB_BUTTON_SIZE_DP), dpToPx(SUB_BUTTON_SIZE_DP)))
            .setBackgroundDrawable(null)
            .build()
            .apply {
                setOnClickListener { clickListener() }
                setOnTouchListener { view, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> view.alpha = 0.7f
                        MotionEvent.ACTION_UP -> view.alpha = 1f
                    }
                    false
                }
            }
    }

    private fun handleActionButtonClick(actionName: String) {
        floatingActionMenu?.close(true)
        startExitAnimation {
            when (actionName) {
                "心情录" -> startActivity(Intent(this, RecordActivity::class.java)).also {
                    overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out)
                }
                "许愿" -> { /* TODO */ }
                "留声瓶" -> { /* TODO */ }
            }
            handler.postDelayed({ startReturnAnimation() }, ANIMATION_DELAY)
        }
    }

//    private fun startExitAnimation(onEnd: (() -> Unit)? = null) {
//        ObjectAnimator.ofFloat(binding.fcvNavigation, "translationY", 0f, -binding.fcvNavigation.height.toFloat()).apply {
//            duration = EXIT_ANIM_DURATION
//            interpolator = AccelerateDecelerateInterpolator()
//            addListener(object : AnimatorListenerAdapter() {
//                override fun onAnimationEnd(animation: Animator) {
//                    startCoordinatorExitAnimation()
//                    onEnd?.invoke()
//                }
//            })
//            start()
//        }
//    }


    private fun startExitAnimation(onEnd: (() -> Unit)? = null) {
        // fcvNavigation 上滑
        val fcvAnim = ObjectAnimator.ofFloat(
            binding.fcvNavigation,
            "translationY",
            0f,
            -binding.fcvNavigation.height.toFloat()
        ).apply {
            duration = EXIT_ANIM_DURATION
            interpolator = AccelerateDecelerateInterpolator()
        }

        // coordinatorLayout 下滑
        val coordinatorAnim = ObjectAnimator.ofFloat(
            binding.coordinatorLayout,
            "translationY",
            0f,
            binding.coordinatorLayout.height.toFloat()
        ).apply {
            duration = COORDINATOR_ANIM_DURATION
            interpolator = AccelerateDecelerateInterpolator()
        }

        fcvAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd?.invoke()
            }
        })

        // 同时开始动画
        fcvAnim.start()
        coordinatorAnim.start()
    }

    private fun startReturnAnimation() {
        // coordinatorLayout 回弹
        val coordinatorReturn = ObjectAnimator.ofFloat(
            binding.coordinatorLayout,
            "translationY",
            binding.coordinatorLayout.height.toFloat(),
            0f
        ).apply {
            duration = RETURN_ANIM_DURATION
            interpolator = OvershootInterpolator(1.2f) // 弹性效果
        }

        coordinatorReturn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                startFcvReturnAnimation()
            }
        })

        coordinatorReturn.start()
    }

    private fun startFcvReturnAnimation() {
        // fcvNavigation 回弹
        ObjectAnimator.ofFloat(
            binding.fcvNavigation,
            "translationY",
            -binding.fcvNavigation.height.toFloat(),
            0f
        ).apply {
            duration = RETURN_ANIM_DURATION
            interpolator = OvershootInterpolator(1.2f)
            start()
        }
    }

    private fun startCoordinatorExitAnimation() {
        ObjectAnimator.ofFloat(binding.coordinatorLayout, "translationY", 0f, binding.coordinatorLayout.height.toFloat()).apply {
            duration = COORDINATOR_ANIM_DURATION
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
//
//    private fun startReturnAnimation() {
//        ObjectAnimator.ofFloat(binding.coordinatorLayout, "translationY", binding.coordinatorLayout.height.toFloat(), 0f).apply {
//            duration = RETURN_ANIM_DURATION
//            interpolator = AccelerateDecelerateInterpolator()
//            addListener(object : AnimatorListenerAdapter() {
//                override fun onAnimationEnd(animation: Animator) {
//                    startFcvReturnAnimation()
//                }
//            })
//            start()
//        }
//    }
//
//    private fun startFcvReturnAnimation() {
//        ObjectAnimator.ofFloat(binding.fcvNavigation, "translationY", -binding.fcvNavigation.height.toFloat(), 0f).apply {
//            duration = RETURN_ANIM_DURATION
//            interpolator = AccelerateDecelerateInterpolator()
//            start()
//        }
//    }

    private fun startFabScaleAnimation(fromScale: Float, toScale: Float) {
        ValueAnimator.ofFloat(fromScale, toScale).apply {
            duration = 300L  // FAB 缩放动画稍微慢一点
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val scale = animation.animatedValue as Float
                binding.fabNavigation.scaleX = scale
                binding.fabNavigation.scaleY = scale
            }
            start()
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density + 0.5f).toInt()

    override fun showError() {
        Toast.makeText(this, "加载导航失败，请重试", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
