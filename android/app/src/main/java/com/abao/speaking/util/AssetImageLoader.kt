package com.abao.speaking.util

import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

data class LoadedAssetImage(
    val width: Int,
    val height: Int
)

/** 与参考页 index.html 一致，优先 ./assets/ 路径 */
object AssetImageLoader {
    fun load(
        activity: AppCompatActivity,
        fileName: String,
        imageView: ImageView,
        fallback: TextView? = null,
        onResult: ((LoadedAssetImage?) -> Unit)? = null
    ) {
        val candidates = listOf(
            "www/assets/$fileName",
            "images/$fileName",
            fileName
        )
        for (path in candidates) {
            runCatching {
                activity.assets.open(path).use { stream ->
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, bounds)
                    val width = bounds.outWidth
                    val height = bounds.outHeight
                    if (width <= 0 || height <= 0) return

                    activity.assets.open(path).use { decodeStream ->
                        val bitmap = BitmapFactory.decodeStream(decodeStream) ?: return
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                        fallback?.visibility = View.GONE
                        imageView.requestLayout()
                        onResult?.invoke(LoadedAssetImage(width, height))
                        return
                    }
                }
            }
        }
        onResult?.invoke(null)
    }
}
