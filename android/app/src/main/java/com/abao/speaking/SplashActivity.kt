package com.abao.speaking

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import com.abao.speaking.databinding.ActivitySplashBinding
import com.abao.speaking.util.AssetImageLoader
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 首页 = 整页设计图 panda-hero.jpg（1315×672）。
 * 平板屏宽比与设计稿不一致时，无法同时「铺满裁切」又「左右都不丢」：
 * 默认完整显示（fitCenter），仅当屏宽比接近设计稿时才铺满裁切。
 */
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private var safeInsetLeft = 0
    private var safeInsetTop = 0
    private var safeInsetRight = 0
    private var safeInsetBottom = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupFullscreenWindow()
        applyDisplayInsets()
        bindEnterAction(binding.splashTapOverlay)
        bindEnterAction(binding.enterButton)
        loadSplashHero()
    }

    private fun setupFullscreenWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, binding.root).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun applyDisplayInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.splashRoot) { _, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            safeInsetLeft = safe.left
            safeInsetTop = safe.top
            safeInsetRight = safe.right
            safeInsetBottom = safe.bottom
            applyHeroImageLayout()
            insets
        }
        ViewCompat.requestApplyInsets(binding.splashRoot)
    }

    private fun loadSplashHero() {
        val candidates = listOf(
            "panda-hero.jpg",
            "panda-hero.png",
            "splash-screen.jpg",
            "splash-screen.png"
        )
        for (fileName in candidates) {
            if (tryLoadHeroImage(fileName)) return
        }
        showFallbackLayout()
    }

    private fun tryLoadHeroImage(fileName: String): Boolean {
        var ok = false
        AssetImageLoader.load(this, fileName, binding.splashPanda, binding.splashPandaFallback) {
            ok = it != null
        }
        if (!ok) return false
        showImageOnlySplash()
        return true
    }

    private fun showImageOnlySplash() {
        binding.splashPanda.visibility = View.VISIBLE
        binding.splashTapOverlay.visibility = View.VISIBLE
        binding.splashLogo.visibility = View.GONE
        binding.splashLeftPanel.visibility = View.GONE
        binding.splashOverlay.visibility = View.GONE
        binding.splashPandaFallback.visibility = View.GONE
        binding.splashPanda.post { applyHeroImageLayout() }
    }

    private fun applyHeroImageLayout() {
        val drawable = binding.splashPanda.drawable
        val viewWidth = binding.splashPanda.width
        val viewHeight = binding.splashPanda.height
        if (drawable == null || viewWidth <= 0 || viewHeight <= 0) return

        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        if (imageWidth <= 0f || imageHeight <= 0f) return

        val viewRatio = viewWidth.toFloat() / viewHeight
        val imageRatio = imageWidth / imageHeight
        val ratioGap = abs(viewRatio - imageRatio) / imageRatio

        binding.splashPanda.updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginStart = safeInsetLeft
            topMargin = safeInsetTop
            marginEnd = safeInsetRight
            bottomMargin = safeInsetBottom
        }

        // 屏宽比与设计稿接近（约 16:10）时可铺满；否则完整显示，左右内容都在
        binding.splashPanda.scaleType = if (ratioGap < 0.12f) {
            ImageView.ScaleType.CENTER_CROP
        } else {
            ImageView.ScaleType.FIT_CENTER
        }
    }

    private fun showFallbackLayout() {
        binding.splashPanda.visibility = View.GONE
        binding.splashTapOverlay.visibility = View.GONE
        binding.splashLogo.visibility = View.VISIBLE
        binding.splashLeftPanel.visibility = View.VISIBLE
        binding.splashOverlay.visibility = View.VISIBLE
        binding.splashPandaFallback.visibility = View.VISIBLE
        applyFallbackInsets()
        buildWaveLine()
    }

    private fun applyFallbackInsets() {
        val logoStart = resources.getDimensionPixelSize(R.dimen.splash_logo_margin_start)
        val logoTop = resources.getDimensionPixelSize(R.dimen.splash_logo_margin_top)
        val copyStart = resources.getDimensionPixelSize(R.dimen.splash_copy_margin_start)
        binding.splashLogo.updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginStart = logoStart + safeInsetLeft
            topMargin = logoTop + safeInsetTop
        }
        binding.splashLeftPanel.updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginStart = copyStart + safeInsetLeft
        }
    }

    private fun bindEnterAction(view: View) {
        view.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun buildWaveLine() {
        binding.waveLine.removeAllViews()
        val heights = listOf(24, 42, 32, 50, 24, 42, 32, 50, 24, 42, 32, 50)
        val gap = (8 * resources.displayMetrics.density).roundToInt()
        val barWidth = (6 * resources.displayMetrics.density).roundToInt()
        heights.forEach { heightDp ->
            val bar = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(barWidth, dp(heightDp)).apply {
                    marginEnd = gap
                }
                setBackgroundColor(0xFF9FC7FF.toInt())
                alpha = 0.52f
            }
            binding.waveLine.addView(bar)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (binding.splashPanda.visibility == View.VISIBLE) {
            binding.splashPanda.post { applyHeroImageLayout() }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
