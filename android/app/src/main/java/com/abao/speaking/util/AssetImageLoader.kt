package com.abao.speaking.util

import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** 加载网页 assets 目录下的图片，兼容 HTML 路径 ./assets/xxx */
object AssetImageLoader {
    fun load(
        activity: AppCompatActivity,
        fileName: String,
        imageView: ImageView,
        fallback: TextView? = null
    ) {
        val candidates = listOf(
            "images/$fileName",
            fileName,
            "www/assets/$fileName"
        )
        for (path in candidates) {
            runCatching {
                activity.assets.open(path).use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream) ?: return
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE
                    fallback?.visibility = View.GONE
                    imageView.requestLayout()
                    return
                }
            }
        }
    }
}
