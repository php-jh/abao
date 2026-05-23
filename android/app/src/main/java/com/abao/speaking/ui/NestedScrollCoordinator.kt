package com.abao.speaking.ui

import android.view.MotionEvent
import androidx.core.widget.NestedScrollView

/**
 * 内层对话区与外层课中演练面板双层滚动：内层优先，到顶/底后再交给外层。
 */
object NestedScrollCoordinator {
    fun attachInner(inner: NestedScrollView, outer: NestedScrollView) {
        var lastY = 0f
        inner.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastY = event.y
                    outer.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = event.y - lastY
                    lastY = event.y
                    val canScrollUp = inner.scrollY > 0
                    val canScrollDown = inner.canScrollVertically(1)
                    val innerConsumes = (dy < 0f && canScrollDown) || (dy > 0f && canScrollUp)
                    outer.requestDisallowInterceptTouchEvent(innerConsumes)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    outer.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
        // 须在布局 inflate 完成后再开，写在 XML 里会在部分系统上触发 NPE
        inner.post {
            inner.isNestedScrollingEnabled = true
            outer.isNestedScrollingEnabled = true
        }
    }
}
