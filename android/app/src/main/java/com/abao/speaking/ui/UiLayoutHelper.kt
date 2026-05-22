package com.abao.speaking.ui

import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abao.speaking.databinding.ActivityMainBinding

/** 按屏幕宽度适配网页 CSS 中的栅格与反馈区布局 */
object UiLayoutHelper {
    fun applyMainLayout(binding: ActivityMainBinding, config: Configuration) {
        applyShellLayout(binding, config.screenWidthDp)
        applyFeedbackLayout(binding, config.screenWidthDp)
        applyRecordingLayout(binding, config.screenWidthDp)
        applyWarmupSpan(binding.wordGrid, config.screenWidthDp)
    }

    private fun applyShellLayout(binding: ActivityMainBinding, widthDp: Int) {
        val params = binding.agentPanel.layoutParams as? ConstraintLayout.LayoutParams ?: return
        params.matchConstraintPercentWidth = when {
            widthDp >= 1024 -> 0.265f
            widthDp >= 920 -> 0.244f
            else -> 0.28f
        }
        binding.agentPanel.layoutParams = params
    }

    private fun applyFeedbackLayout(binding: ActivityMainBinding, widthDp: Int) {
        val fb = binding.feedbackPanel
        val horizontal = widthDp >= 600
        fb.feedbackContent.orientation = if (horizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

        setColumnLayout(fb.feedbackScoreColumn, horizontal, 220, 0f)
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

    private fun applyRecordingLayout(binding: ActivityMainBinding, widthDp: Int) {
        val horizontal = widthDp >= 720
        binding.recordingArea.orientation = if (horizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        val transcriptLp = binding.transcriptBox.layoutParams as LinearLayout.LayoutParams
        val controlsLp = binding.recordingControls.layoutParams as LinearLayout.LayoutParams
        if (horizontal) {
            transcriptLp.width = 0
            transcriptLp.weight = 1f
            transcriptLp.bottomMargin = 0
            transcriptLp.marginEnd = dp(binding, 18)
            controlsLp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            controlsLp.weight = 0f
        } else {
            transcriptLp.width = ViewGroup.LayoutParams.MATCH_PARENT
            transcriptLp.weight = 0f
            transcriptLp.marginEnd = 0
            transcriptLp.bottomMargin = dp(binding, 12)
            controlsLp.width = ViewGroup.LayoutParams.MATCH_PARENT
            controlsLp.weight = 0f
        }
        binding.transcriptBox.layoutParams = transcriptLp
        binding.recordingControls.layoutParams = controlsLp
    }

    private fun applyWarmupSpan(grid: RecyclerView, widthDp: Int) {
        val span = when {
            widthDp >= 920 -> 3
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
