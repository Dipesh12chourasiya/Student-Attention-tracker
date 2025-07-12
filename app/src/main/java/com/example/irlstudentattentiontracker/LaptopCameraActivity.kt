package com.example.irlstudentattentiontracker

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.irlstudentattentiontracker.databinding.ActivityLaptopCameraBinding
import com.github.niqdev.mjpeg.DisplayMode
import com.github.niqdev.mjpeg.Mjpeg

class LaptopCameraActivity : AppCompatActivity() {
    private lateinit var binding : ActivityLaptopCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityLaptopCameraBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val ip = Env.IP
        
        Mjpeg.newInstance()
            .open("http://$ip/video", 5000) // timeout as second argument
            .subscribe({ stream ->
                binding.mjpegView.setSource(stream)
                binding.mjpegView.setDisplayMode(DisplayMode.BEST_FIT)
                binding.mjpegView.showFps(true)
            }, { error ->
                Log.e("MJPEG", "Stream failed: $error")
            })

    }

}