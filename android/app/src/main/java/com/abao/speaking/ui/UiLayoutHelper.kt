package com.abao.speaking.ui

import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.LinearLayout
import com.abao.speaking.R
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abao.speaking.databinding.ActivityMainBinding

/** Pad 横屏：按屏宽自适应左侧智能体栏与反馈区栅格 */
object UiLayoutHelper {
    fun applyMainLayout(binding: ActivityMainBinding, config: Configuration) {
        val widthDp = config.screenWidthDp
        val isTablet = config.smallestScreenWidthDp >= 600
        applyShellLayout(binding, widthDp, isTablet)
        applyFeedbackLayout(binding, widthDp, isTablet)
        applyRecordingLayout(binding)
        applyWarmupSpan(binding.wordGrid, widthDp, isTablet)
    }

    private fun applyShellLayout(binding: ActivityMainBinding, widthDp: Int, isTablet: Boolean) {
        val params = binding.agentPanel.layoutParams as? ConstraintLayout.LayoutParams ?: return
        // 原型 grid 左栏固定 340px
        params.matchConstraintPercentWidth = when {
            isTablet && widthDp >= 1200 -> 0.30f
            isTablet && widthDp >= 960 -> 0.305f
            isTablet -> 0.31f
            widthDp >= 920 -> 0.29f
            else -> 0.32f
        }
        binding.agentPanel.layoutParams = params
    }

    private fun applyFeedbackLayout(binding: ActivityMainBinding, widthDp: Int, isTablet: Boolean) {
        val fb = binding.feedbackPanel
        val horizontal = isTablet || widthDp >= 600
        fb.feedbackContent.orientation = if (horizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

        val scoreWidth = if (isTablet && widthDp >= 1200) 280 else 260
        setColumnLayout(fb.feedbackScoreColumn, horizontal, scoreWidth, 0f)
        setColumnLayout(fb.feedbackMetricColumn, horizontal, 0, 1f)
        setColumnLayout(fb.feedbackAnalysisColumn, horizontal, 0, 1.15f)

        if (!horizontal) {
            (fb.feedbackScoreColumn.layoutParams as LinearLayout.LayoutParams).apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                bottomMargin = dp(binding, 12)
            }
            (fb.feedbackMetricColumn.layoutParams as LinearLayout.LayoutParams).apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                bottomMargin = dp(binding, 12)
            }
            (fb.feedbackAnalysisColumn.layoutParams as LinearLayout.LayoutParams).width =
                ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    private fun setColumnLayout(
        column: LinearLayout,
        horizontal: Boolean,
        widthPx: Int,
        weight: Float
    ) {
        val lp = column.layoutParams as LinearLayout.LayoutParams
        if (horizontal) {
            lp.width = if (weight > 0f) 0 else dp(column, widthPx)
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.weight = weight
            lp.bottomMargin = 0
        } else {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.weight = 0f
        }
        column.layoutParams = lp
    }

    private fun applyRecordingLayout(binding: ActivityMainBinding) {
        binding.recordingArea.orientation = LinearLayout.VERTICAL
        binding.recordingControls.orientation = LinearLayout.HORIZONTAL

        val transcriptLp = binding.transcriptBox.layoutParams as LinearLayout.LayoutParams
        transcriptLp.width = ViewGroup.LayoutParams.MATCH_PARENT
        transcriptLp.weight = 0f
        binding.transcriptBox.layoutParams = transcriptLp

        val controlsLp = binding.recordingControls.layoutParams as LinearLayout.LayoutParams
        controlsLp.width = ViewGroup.LayoutParams.MATCH_PARENT
        controlsLp.weight = 0f
        controlsLp.topMargin = dp(binding, 12)
        binding.recordingControls.layoutParams = controlsLp

        val recordLp = binding.recordButton.layoutParams as LinearLayout.LayoutParams
        recordLp.width = 0
        recordLp.weight = 1f
        binding.recordButton.layoutParams = recordLp

        val finishLp = binding.finishButton.layoutParams as LinearLayout.LayoutParams
        finishLp.width = 0
        finishLp.weight = 1f
        binding.finishButton.layoutParams = finishLp
    }

    private fun applyWarmupSpan(grid: RecyclerView, widthDp: Int, isTablet: Boolean) {
        val span = when {
            isTablet || widthDp >= 920 -> 3
            widthDp >= 640 -> 2
            else -> 1
        }
        (grid.layoutManager as? GridLayoutManager)?.spanCount = span
    }

    private fun dp(view: android.view.View, value: Int): Int =
        (value * view.resources.displayMetrics.density).toInt()

    private fun dp(binding: ActivityMainBinding, value: Int): Int =
        (value * binding.root.resources.displayMetrics.density).toInt()
}
