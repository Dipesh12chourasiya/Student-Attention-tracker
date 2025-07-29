package com.example.irlstudentattentiontracker

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.irlstudentattentiontracker.databinding.ActivityTimeTableBinding
import io.noties.markwon.Markwon

class TimeTableActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimeTableBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimeTableBinding.inflate(layoutInflater)
        this.window.statusBarColor = Color.BLUE

        setContentView(binding.root)

        // Load saved text from SharedPreferences
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val savedText = prefs.getString("ai_response", "No timetable generated yet")

//        binding.tvAiResponse.text = savedText
        binding.etAiResponse.setText(savedText)
        val markwon = Markwon.create(this)
        markwon.setMarkdown(binding.tvAiResponse, savedText!!)

        // Edit button enables EditText
        binding.btnEdit.setOnClickListener {
            binding.etAiResponse.visibility = View.VISIBLE
            binding.tvAiResponse.visibility = View.GONE
            binding.etAiResponse.requestFocus()
        }

        // Save button saves the text and disables editing
        binding.btnSave.setOnClickListener {
            val editedText = binding.etAiResponse.text.toString()

            prefs.edit().putString("ai_response", editedText).apply()

            binding.etAiResponse.visibility = View.GONE
            binding.tvAiResponse.visibility = View.VISIBLE
            val markwon = Markwon.create(this)
            markwon.setMarkdown(binding.tvAiResponse, editedText)

        }

        // Back button in TopAppBar
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}
