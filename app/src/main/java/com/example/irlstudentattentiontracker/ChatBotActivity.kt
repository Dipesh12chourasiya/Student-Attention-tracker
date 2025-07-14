package com.example.irlstudentattentiontracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.irlstudentattentiontracker.databinding.ActivityChatBotBinding
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel
import io.noties.markwon.Markwon

class ChatBotActivity : AppCompatActivity() {

    private val viewModel: UserViewModel by viewModels()
    private lateinit var binding: ActivityChatBotBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var username: String? = ""

        // Back button
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.fetchUsername {
            username = it ?: "User"
        }


        viewModel.aiResponse.observe(this) { response ->
            binding.tvAiResponse.visibility = View.VISIBLE

            val fullResponse = "Hey $username 👋\n\n$response"

            // Save to SharedPreferences
            val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            prefs.edit().putString("ai_response", fullResponse).apply()

            // Display in current activity
            val markwon = Markwon.create(this)
            markwon.setMarkdown(binding.tvAiResponse, fullResponse)
        }

        binding.tvAiResponse.setOnClickListener {
            startActivity(Intent(this, TimeTableActivity::class.java))
        }


        // Button click to send user prompt to API
        binding.btnGenerate.setOnClickListener {
            val prompt = binding.etUserPrompt.text.toString().trim()
            if (prompt.isNotEmpty()) {

                viewModel.fetchRespponse(prompt)
            } else {
                Toast.makeText(this, "Enter the Prompt.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}