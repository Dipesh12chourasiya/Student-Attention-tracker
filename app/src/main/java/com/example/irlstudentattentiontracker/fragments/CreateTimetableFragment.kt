package com.example.irlstudentattentiontracker.fragments




import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.irlstudentattentiontracker.R
import com.example.irlstudentattentiontracker.SessionDetailActivity
import com.example.irlstudentattentiontracker.TimeTableActivity
import com.example.irlstudentattentiontracker.databinding.FragmentCreateTimetableBinding
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateTimetableFragment : Fragment() {

    private var _binding: FragmentCreateTimetableBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()

    private var username: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateTimetableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().window.statusBarColor = Color.BLUE

        setupToolbar()
        setupTimePickers()
        setupObservers()
        setupGenerateButton()
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupTimePickers() {
        binding.etMorning.setOnClickListener {
            showTimePicker(binding.etMorning, "Select Wake Up Time")
        }

        binding.etNight.setOnClickListener {
            showTimePicker(binding.etNight, "Select Sleep Time")
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val gifView = binding.gifView
            if (isLoading) {
                gifView.visibility = View.VISIBLE
                Glide.with(requireContext())
                    .asGif()
                    .load(R.drawable.loading)
                    .into(gifView)
            } else {
                gifView.visibility = View.GONE
            }
        }

        viewModel.fetchUsername { user ->
            username = user ?: "User"
        }

        viewModel.aiResponse.observe(viewLifecycleOwner) { response ->
            binding.tvAiResponse.visibility = View.VISIBLE
            val fullResponse = "Hey $username 👋\n\n$response"

            // Save to SharedPreferences
            val prefs = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            prefs.edit().putString("ai_response", fullResponse).apply()

            // Display using Markwon
            val markwon = Markwon.create(requireContext())
            markwon.setMarkdown(binding.tvAiResponse, fullResponse)
        }

        binding.tvAiResponse.setOnClickListener {
            startActivity(Intent(requireContext(), TimeTableActivity::class.java))
        }
    }

    private fun setupGenerateButton() {
        binding.btnGenerate.setOnClickListener {
            val subjects = binding.etUserSubjects.text.toString().trim()
            val wakeUpTime = binding.etMorning.text.toString().trim()
            val sleepTime = binding.etNight.text.toString().trim()

            if (subjects.isNotEmpty() && wakeUpTime.isNotEmpty() && sleepTime.isNotEmpty()) {
                viewModel.fetchRespponse(subjects, wakeUpTime, sleepTime)
            } else {
                Toast.makeText(requireContext(), "Enter the details", Toast.LENGTH_SHORT).show()
            }

            // Hide keyboard
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etUserSubjects.windowToken, 0)
        }
    }

    private fun showTimePicker(editText: EditText, title: String) {
        val calendar = Calendar.getInstance()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText(title)
            .build()

        picker.show(childFragmentManager, picker.toString())

        picker.addOnPositiveButtonClickListener {
            val formattedTime = formatTime(picker.hour, picker.minute)
            editText.setText(formattedTime)

            Snackbar.make(binding.root, "$title: $formattedTime", Snackbar.LENGTH_SHORT).show()
        }

        picker.addOnNegativeButtonClickListener {
            Snackbar.make(binding.root, "Time selection cancelled", Snackbar.LENGTH_SHORT).show()
        }

        picker.addOnCancelListener {
            Snackbar.make(binding.root, "Time selection cancelled", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
