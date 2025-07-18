package com.example.irlstudentattentiontracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.irlstudentattentiontracker.databinding.ActivityChatBotBinding
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

        binding.etMorning.setOnClickListener {
            showTimePicker(binding.etMorning, "Select Wake Up Time")
        }

        binding.etNight.setOnClickListener {
            showTimePicker(binding.etNight, "Select Sleep Time")
        }

        val gifView = binding.gifView

        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                gifView.visibility = View.VISIBLE
                Glide.with(this)
                    .asGif()
                    .load(R.drawable.loading)  // your loading.gif file in res/drawable
                    .into(gifView)
            } else {
                gifView.visibility = View.GONE
            }
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
//            binding.tvAiResponse.text = fullResponse
        }

        binding.tvAiResponse.setOnClickListener {
            startActivity(Intent(this, TimeTableActivity::class.java))
        }


        // Button click to send user prompt to API
        binding.btnGenerate.setOnClickListener {
            val subjects = binding.etUserSubjects.text.toString().trim()
            val wakeUpTime = binding.etMorning.text.toString().trim()
            val sleepTime = binding.etNight.text.toString().trim()

            if (subjects.isNotEmpty() && wakeUpTime.isNotEmpty() && sleepTime.isNotEmpty()) {
                // method to get ai response
                viewModel.fetchRespponse(subjects, wakeUpTime, sleepTime)
            } else {
                Toast.makeText(this, "Enter the details", Toast.LENGTH_SHORT).show()
            }

            // Hide keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etUserSubjects.windowToken, 0)
        }
    }


// to show the time dialog in the edit text
    private fun showTimePicker(editText: TextInputEditText, title: String) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H) // or CLOCK_24H
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText(title)
            .build()

        picker.show(supportFragmentManager, picker.toString())

        picker.addOnPositiveButtonClickListener {
            val selectedHour = picker.hour
            val selectedMinute = picker.minute

            val formattedTime = formatTime(selectedHour, selectedMinute)
            editText.setText(formattedTime)

            // Optional: Show a snackbar for confirmation
            Snackbar.make(
                findViewById(android.R.id.content), // Root view for Snackbar
                "$title: $formattedTime",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        picker.addOnNegativeButtonClickListener {
            // Handle cancel button click if needed
            Snackbar.make(
                findViewById(android.R.id.content),
                "Time selection cancelled",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        picker.addOnCancelListener {
            // Handle dialog dismissal (e.g., by back button or tapping outside)
            Snackbar.make(
                findViewById(android.R.id.content),
                "Time selection cancelled",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)

        val format = SimpleDateFormat("h:mm a", Locale.getDefault()) // e.g., 6:00 AM, 10:30 PM
        return format.format(calendar.time)
    }

    private fun setupToolbar() {
        val topAppBar: com.google.android.material.appbar.MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        topAppBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

}
