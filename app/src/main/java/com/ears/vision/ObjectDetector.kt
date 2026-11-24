package com.ears.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class ObjectDetector(context: Context, modelPath: String, labelPath: String) {
    private val interpreter: Interpreter
    private val labels = context.assets.open(labelPath).bufferedReader().readLines()
    private val numElements = 8400
    private val numChannels = 1207
    private val output = Array(1) { Array(numChannels) { FloatArray(numElements) } }

    @Volatile
    private var latestDetections: List<Detection> = emptyList()

    init {
        val assetFile = context.assets.openFd(modelPath)
        val modelBuffer = FileInputStream(assetFile.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, assetFile.startOffset, assetFile.declaredLength
        )
        val options = Interpreter.Options().apply {
            try { addDelegate(GpuDelegate()) } catch (e: Exception) {}
        }
        interpreter = Interpreter(modelBuffer, options)
    }

    fun getLatestDetections(): List<Detection> = latestDetections

    fun detect(bitmap: Bitmap): List<Detection> {
        val resized = Bitmap.createScaledBitmap(bitmap, 640, 640, true)
        val byteBuffer = ByteBuffer.allocateDirect(1 * 640 * 640 * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }
        val pixels = IntArray(640 * 640)
        resized.getPixels(pixels, 0, 640, 0, 0, 640, 640)
        for (pixel in pixels) {
            byteBuffer.putFloat(Color.red(pixel) / 255f)
            byteBuffer.putFloat(Color.green(pixel) / 255f)
            byteBuffer.putFloat(Color.blue(pixel) / 255f)
        }
        interpreter.run(byteBuffer, output)

        val detections = mutableListOf<Detection>()
        for (i in 0 until numElements) {
            var maxConf = 0.30f
            var maxIdx = -1

            for (j in 4 until numChannels) {
                if (output[0][j][i] > maxConf) {
                    maxConf = output[0][j][i]
                    maxIdx = j - 4
                }
            }

            if (maxIdx != -1) {
                val cx = output[0][0][i]
                val cy = output[0][1][i]
                val w = output[0][2][i]
                val h = output[0][3][i]
                val multiplier = if (cx <= 1.01f) 640f else 1f

                // CLEANING LOGIC: Remove "/" and "," synonyms
                val rawLabel = labels[maxIdx]
                val cleanLabel = rawLabel.split("/")[0].split(",")[0].trim()

                detections.add(Detection(
                    RectF((cx-w/2)*multiplier, (cy-h/2)*multiplier, (cx+w/2)*multiplier, (cy+h/2)*multiplier),
                    cleanLabel,
                    maxConf
                ))
            }
        }
        val finalResults = applyNMS(detections)
        latestDetections = finalResults
        return finalResults
    }

    private fun applyNMS(detections: List<Detection>): List<Detection> {
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<Detection>()
        while (sorted.isNotEmpty()) {
            val first = sorted.removeAt(0)
            selected.add(first)
            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (calculateIoU(first.box, next.box) > 0.45f) iterator.remove()
            }
        }
        return selected
    }

    private fun calculateIoU(b1: RectF, b2: RectF): Float {
        val intersect = RectF()
        return if (intersect.setIntersect(b1, b2)) {
            val iArea = intersect.width() * intersect.height()
            val uArea = (b1.width() * b1.height()) + (b2.width() * b2.height()) - iArea
            iArea / uArea
        } else 0f
    }

    data class Detection(val box: RectF, val label: String, val confidence: Float)
}