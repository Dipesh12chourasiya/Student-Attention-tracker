package com.example.irlstudentattentiontracker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.irlstudentattentiontracker.databinding.ActivityMainBinding
import com.example.irlstudentattentiontracker.utils.Utils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    private var isSessionRunning = false
    private var sessionStartTime: Long = 0L
    private val handler = Handler()

    private var attentiveCount = 0
    private var inattentiveCount = 0
    private var totalAnalyzedFrames = 0
    private var percentAttentive = 100

    private val CAMERA_PERMISSION_CODE = 101
    var sessionTimestamp = ""

    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = System.currentTimeMillis() - sessionStartTime
            binding.tvTimer.text = " " + Utils.formatDuration(elapsed)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestCameraPermission()

        binding.btnSwitchCamera.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                CameraSelector.LENS_FACING_BACK
            else
                CameraSelector.LENS_FACING_FRONT
            bindCameraUseCases()
        }


        binding.btnStartSession.setOnClickListener {
            isSessionRunning = true
            sessionStartTime = System.currentTimeMillis()
            sessionTimestamp = Utils.formatFullDateTime(sessionStartTime)
            handler.post(timerRunnable)

            attentiveCount = 0
            inattentiveCount = 0
            totalAnalyzedFrames = 0
            percentAttentive = 100

            binding.btnStartSession.visibility = View.GONE
            binding.btnEndSession.visibility = View.VISIBLE
            Toast.makeText(this, "📸 Session Started", Toast.LENGTH_SHORT).show()
        }

        binding.btnEndSession.setOnClickListener {
            isSessionRunning = false
            val sessionEndTime = System.currentTimeMillis()
            val durationMillis = sessionEndTime - sessionStartTime

            val intent = Intent(this, StatsActivity::class.java).apply {
                putExtra("attentiveCount", attentiveCount)
                putExtra("attentionPercent", percentAttentive)
                putExtra("sessionDuration", Utils.formatDuration(durationMillis))
                putExtra("startTime", Utils.formatTimeOnly(sessionStartTime))
                putExtra("endTime", Utils.formatTimeOnly(sessionEndTime))
                putExtra("sessionTimestamp", sessionTimestamp)
                putExtra("totalFrames", totalAnalyzedFrames)
            }
            startActivity(intent)

            binding.btnStartSession.visibility = View.VISIBLE
            binding.btnEndSession.visibility = View.GONE
        }
    }

    private fun checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }

        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this), FaceAnalyzer(this))
            }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, analyzer)
    }

    inner class FaceAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {
        private val detector by lazy {
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build()
            FaceDetection.getClient(options)
        }

        private val eyeClosedFrames = mutableMapOf<Int, Int>()
        private val attentionMap = mutableMapOf<Int, Boolean>()
        private var mediaPlayer: MediaPlayer? = null

        private val MIN_FACE_WIDTH = 100
        private val MIN_FACE_HEIGHT = 100

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image ?: return imageProxy.close()
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (!isSessionRunning) {
                        imageProxy.close()
                        return@addOnSuccessListener
                    }

                    totalAnalyzedFrames++

                    if (faces.isEmpty()) {
                        inattentiveCount++
                        stopAlertSound()
                        updateAttentionStats()
                        runOnUiThread { binding.tvStatsFaceEye.text = "No face detected" }
                        runOnUiThread {
                            binding.faceOverlay.clearFaces()
                        }
                        imageProxy.close()
                        return@addOnSuccessListener
                    }

                    for (face in faces) {
                        val id = face.trackingId ?: continue

                        val leftEye = face.leftEyeOpenProbability ?: 0f
                        val rightEye = face.rightEyeOpenProbability ?: 0f
                        val avgEye = (leftEye + rightEye) / 2
                        val isEyeOpen = avgEye > 0.4f

                        val smile = face.smilingProbability ?: 0f

                        val faceBox = face.boundingBox
                        val isFaceVisible =
                            faceBox.width() > MIN_FACE_WIDTH && faceBox.height() > MIN_FACE_HEIGHT

                        var attentionScore = 0
                        if (isEyeOpen) attentionScore++
                        if (isFaceVisible) attentionScore++

                        val isAttentive = attentionScore >= 2

                        if (isAttentive) attentiveCount++ else inattentiveCount++

                        if (!isEyeOpen) {
                            eyeClosedFrames[id] = eyeClosedFrames.getOrDefault(id, 0) + 1
                        } else {
                            eyeClosedFrames[id] = 0
                        }

                        val isEyeClosedTooLong = eyeClosedFrames.getOrDefault(id, 0) > 15

                        val wasAttentive = attentionMap[id] != true
                        if (isEyeClosedTooLong && wasAttentive) {
                            playAlertSound()
                            attentionMap[id] = true
                        }
                        if (!isEyeClosedTooLong) {
                            attentionMap[id] = false
                            stopAlertSound()
                        }

                        runOnUiThread {
                            binding.tvStatsFaceEye.text = """
Eye Open: ${(avgEye * 100).toInt()}%
Smile: ${(smile * 100).toInt()}%
Face Visible: ${if (isFaceVisible) "✅" else "❌"}
Attentive Score: $attentionScore/2
""".trimIndent()

                            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                binding.faceOverlay.updateFaces(
                                    faces,
                                    image.width,
                                    image.height,
                                    image.rotationDegrees
                                )
                            } else {
                                binding.faceOverlay.updateFacesForBackCamera(
                                    faces,
                                    image.width,
                                    image.height,
                                    image.rotationDegrees
                                )
                            }

                        }
                    }

                    updateAttentionStats()
                    imageProxy.close()
                }
                .addOnFailureListener { imageProxy.close() }
        }

        private fun updateAttentionStats() {
            percentAttentive =
                if (totalAnalyzedFrames > 0) attentiveCount * 100 / totalAnalyzedFrames else 100
            runOnUiThread {
                val color = when {
                    percentAttentive >= 80 -> "#8FE693"
                    percentAttentive >= 50 -> "#FF5722"
                    else -> "#F44336"
                }
                binding.tvStats.text = "Frames: $totalAnalyzedFrames"
                binding.tvAttentivePercents.text = "$percentAttentive%"
                binding.tvAttentivePercents.setTextColor(Color.parseColor(color))
            }
        }

        private fun playAlertSound() {
            stopAlertSound()
            mediaPlayer = MediaPlayer.create(context, R.raw.attention_soundd)
            mediaPlayer?.start()
        }

        private fun stopAlertSound() {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
        }
    }
}


// API deepseek
// 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