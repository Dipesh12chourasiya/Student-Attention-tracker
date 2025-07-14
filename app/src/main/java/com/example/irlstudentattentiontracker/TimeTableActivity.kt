package com.example.irlstudentattentiontracker

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.irlstudentattentiontracker.databinding.ActivityTimeTableBinding
import io.noties.markwon.Markwon

class TimeTableActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimeTableBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimeTableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val response = prefs.getString("ai_response", "No timetable generated yet")

        val markwon = Markwon.create(this)
        markwon.setMarkdown(binding.tvAiResponse, response ?: "")
    }
}