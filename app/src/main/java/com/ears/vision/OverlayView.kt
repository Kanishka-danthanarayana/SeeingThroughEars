package com.ears.vision

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var results: List<ObjectDetector.Detection> = emptyList()
    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 42f
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(8f, 0f, 0f, Color.BLACK)
    }

    fun setData(newResults: List<ObjectDetector.Detection>) {
        results = newResults
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scaleX = width.toFloat() / 640f
        val scaleY = height.toFloat() / 640f

        results.forEach { detection ->
            val left = detection.box.left * scaleX
            val top = detection.box.top * scaleY
            val right = detection.box.right * scaleX
            val bottom = detection.box.bottom * scaleY

            canvas.drawRect(left, top, right, bottom, boxPaint)

            val percent = (detection.confidence * 100).toInt()
            canvas.drawText("${detection.label} $percent%", left, top - 10f, textPaint)
        }
    }
}