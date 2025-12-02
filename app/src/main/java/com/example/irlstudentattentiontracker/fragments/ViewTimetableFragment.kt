package com.example.irlstudentattentiontracker.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.irlstudentattentiontracker.databinding.FragmentViewTimetableBinding
import io.noties.markwon.Markwon

class ViewTimetableFragment : Fragment() {

    private var _binding: FragmentViewTimetableBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewTimetableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button
        binding.topAppBar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val prefs = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val savedText = prefs.getString("ai_response", "No timetable generated yet")

        // Display markdown
        val markwon = Markwon.create(requireContext())
        binding.tvAiResponse.visibility = View.VISIBLE
        binding.etAiResponse.visibility = View.GONE
        markwon.setMarkdown(binding.tvAiResponse, savedText!!)

        // Edit button
        binding.btnEdit.setOnClickListener {
            binding.etAiResponse.setText(savedText)
            binding.etAiResponse.visibility = View.VISIBLE
            binding.tvAiResponse.visibility = View.GONE
            binding.etAiResponse.requestFocus()
        }

        // Save button
        binding.btnSave.setOnClickListener {
            val editedText = binding.etAiResponse.text.toString()
            prefs.edit().putString("ai_response", editedText).apply()

            binding.etAiResponse.visibility = View.GONE
            binding.tvAiResponse.visibility = View.VISIBLE
            val markwon2 = Markwon.create(requireContext())
            markwon2.setMarkdown(binding.tvAiResponse, editedText)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
