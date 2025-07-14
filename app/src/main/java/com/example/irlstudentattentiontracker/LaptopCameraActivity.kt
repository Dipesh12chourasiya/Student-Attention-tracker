package com.example.irlstudentattentiontracker

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.irlstudentattentiontracker.databinding.ActivityLaptopCameraBinding
import com.example.irlstudentattentiontracker.mjpegcode.MjpegInputStream
import com.example.irlstudentattentiontracker.utils.Utils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import java.net.HttpURLConnection
import java.net.URL

class LaptopCameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLaptopCameraBinding
    private lateinit var faceDetector: FaceDetector
    private var isSessionRunning = false
    private var sessionStartTime = 0L
    private val handler = Handler()
    private val uiHandler = Handler()

    private var attentiveCount = 0
    private var inattentiveCount = 0
    private var totalAnalyzedFrames = 0
    private var percentAttentive = 100
    private var sessionTimestamp = ""

    private var mediaPlayer: MediaPlayer? = null
    private val eyeClosedFrames = mutableMapOf<Int, Int>()
    private val attentionMap = mutableMapOf<Int, Boolean>()
    private val MIN_FACE_WIDTH = 100
    private val MIN_FACE_HEIGHT = 100

    private var lastSoundTime = 0L
    private val SOUND_INTERVAL = 5000L

    private var frameCounter = 0
    private val FRAME_PROCESS_INTERVAL = 3
    private var isProcessing = false

    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = System.currentTimeMillis() - sessionStartTime
            binding.tvTimer.text = " " + Utils.formatDuration(elapsed)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaptopCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFaceDetector()

        var ipAddress = ""  // Real-time updated value , //eg: 192.161.22.111

        binding.etIPAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                ipAddress = s.toString()  // Update variable in real-time
            }

            override fun afterTextChanged(s: Editable?) {}
        })


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
            binding.btnStopSound.visibility = View.VISIBLE
                                       //  http://192.168.29.114:5000/video
            startMjpegAnalysisStream("http://${ipAddress.trim()}:5000/video")
        }

        binding.btnEndSession.setOnClickListener {
            isSessionRunning = false
            handler.removeCallbacks(timerRunnable)
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
            binding.btnStopSound.visibility = View.GONE
        }

        binding.btnStopSound.setOnClickListener {
            stopAlertSound()
        }
    }

    private fun setupFaceDetector() {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
        faceDetector = FaceDetection.getClient(options)
    }

    private fun startMjpegAnalysisStream(url: String) {
        Thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()
                val inputStream = connection.inputStream
                val reader = MjpegInputStream(inputStream)

                while (isSessionRunning) {
                    val frameBytes = reader.readMjpegFrame() ?: continue
                    val bitmap = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size)

                    bitmap?.let {
                        frameCounter++
                        if (frameCounter % FRAME_PROCESS_INTERVAL == 0 && !isProcessing) {
                            runOnUiThread { binding.imagePreview.setImageBitmap(it) }
                            processFaceDetection(it)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MJPEG", "Stream error: ${e.message}")
            }
        }.start()
    }

    private fun processFaceDetection(bitmap: android.graphics.Bitmap) {
        if (isProcessing) return
        isProcessing = true

        val image = InputImage.fromBitmap(bitmap, 0)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                isProcessing = false
                if (!isSessionRunning) return@addOnSuccessListener

                totalAnalyzedFrames++
                if (faces.isEmpty()) {
                    inattentiveCount++
                    stopAlertSound()
                    runOnUiThread { binding.tvStatsFaceEye.text = "No face detected" }
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
                    val isFaceVisible = faceBox.width() > MIN_FACE_WIDTH && faceBox.height() > MIN_FACE_HEIGHT

                    val attentionScore = (if (isEyeOpen) 1 else 0) + (if (isFaceVisible) 1 else 0)
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
                        binding.faceOverlay.updateFacesForBackCamera(
                            faces, image.width, image.height, image.rotationDegrees
                        )
                    }
                }
                updateAttentionStats()
            }
            .addOnFailureListener {
                isProcessing = false
                Log.e("FaceDetection", "Detection error: ${it.message}")
            }
    }

    private fun updateAttentionStats() {
        percentAttentive = if (totalAnalyzedFrames > 0)
            attentiveCount * 100 / totalAnalyzedFrames else 100

        runOnUiThread {
            if(percentAttentive > 100){
                percentAttentive = 100;
            }

            val color = when {
                percentAttentive >= 80 -> "#8FE693"
                percentAttentive >= 50 -> "#FF5722"
                else -> "#F44336"
            }
            binding.tvStats.text = "Frames: $totalAnalyzedFrames"
            binding.tvAttentivePercents.text = "$percentAttentive%"
            binding.tvAttentivePercents.setTextColor(android.graphics.Color.parseColor(color))
        }
    }

    private fun playAlertSound() {
        val now = System.currentTimeMillis()
        if (now - lastSoundTime < SOUND_INTERVAL) return
        lastSoundTime = now

        stopAlertSound()
        mediaPlayer = MediaPlayer.create(this, R.raw.attention_soundd)
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
