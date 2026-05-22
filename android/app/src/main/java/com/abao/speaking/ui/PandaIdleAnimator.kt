package com.abao.speaking.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/** 对应 styles.css 中 .panda-avatar 的 panda-idle 动画 */
object PandaIdleAnimator {
    fun start(target: View) {
        target.clearAnimation()
        ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, 0f, -18f, -8f, 0f).apply {
            duration = 3400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(target, View.ROTATION, 0f, -0.6f, 0.4f, 0f).apply {
            duration = 3400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    fun stop(target: View) {
        target.animate().cancel()
        target.translationY = 0f
        target.rotation = 0f
    }
}
