package com.ears.vision

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var detector: ObjectDetector? = null
    private var gemmaManager: GemmaManager? = null
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false
    private var isGemmaRunning = false
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                isTtsReady = true
                setupTtsListener()
            }
        }

        executor.execute {
            detector = ObjectDetector(this, "yolov8m-worldv2.tflite", "labels.txt")
            gemmaManager = GemmaManager(this)
            runOnUiThread { startCamera() }
        }

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (isTtsReady && !isGemmaRunning) {
                    isGemmaRunning = true

                    // Capture all detected items for Gemma
                    val detections = detector?.getLatestDetections() ?: emptyList()
                    val objectsText = detections.map { it.label }.distinct().joinToString(", ")

                    findViewById<OverlayView>(R.id.overlayView).setData(emptyList())

                    // Gemma Feedback
                    tts.speak("Analyzing", TextToSpeech.QUEUE_FLUSH, null, "WAIT")

                    val prompt = "The camera sees: $objectsText. Briefly describe this scene for a blind person."

                    executor.execute {
                        gemmaManager?.describeFrame(prompt) { response ->
                            runOnUiThread {
                                tts.speak(response, TextToSpeech.QUEUE_FLUSH, null, "GEMMA_DONE")
                            }
                        }
                    }
                }
                return true
            }
        })

        findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder).setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            v.performClick()
            true
        }
    }

    private fun setupTtsListener() {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onError(utteranceId: String?) { isGemmaRunning = false }
            override fun onDone(utteranceId: String?) {
                if (utteranceId == "GEMMA_DONE") {
                    runOnUiThread { isGemmaRunning = false }
                }
            }
        })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<androidx.camera.view.PreviewView>(R.id.viewFinder).surfaceProvider)
            }

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(executor) { imageProxy ->
                        if (isGemmaRunning) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val bitmap = imageProxy.toBitmap()
                        val matrix = Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
                        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                        val detections = detector?.detect(rotatedBitmap) ?: emptyList()

                        runOnUiThread {
                            findViewById<OverlayView>(R.id.overlayView).setData(detections)

                            // YOLO Real-time voice: Speak all unique items
                            if (detections.isNotEmpty() && isTtsReady && !tts.isSpeaking) {
                                val distinctLabels = detections.map { it.label }.distinct().joinToString(", ")
                                tts.speak(distinctLabels, TextToSpeech.QUEUE_ADD, null, "YOLO")
                            }
                        }
                        imageProxy.close()
                    }
                }
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
        }, ContextCompat.getMainExecutor(this))
    }
}