package com.abao.speaking

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.abao.speaking.databinding.ActivitySplashBinding
import com.abao.speaking.util.AssetImageLoader
import kotlin.math.roundToInt

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        buildWaveLine()
        AssetImageLoader.load(this, "panda-hero.jpg", binding.splashPanda, binding.splashPandaFallback)
        binding.enterButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun buildWaveLine() {
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
