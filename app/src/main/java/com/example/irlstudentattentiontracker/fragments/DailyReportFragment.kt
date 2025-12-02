package com.example.irlstudentattentiontracker.fragments

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.detectfaceandexpression.adapters.SessionAdapter
import com.example.detectfaceandexpression.models.SessionData
import com.example.irlstudentattentiontracker.R
import com.example.irlstudentattentiontracker.databinding.FragmentDailyReportBinding
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.PercentFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DailyReportFragment : Fragment() {

    private var _binding: FragmentDailyReportBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDailyReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button in toolbar
        binding.topAppBar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val todayDateOnly = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())

        lifecycleScope.launch {
            viewModel.getAllSessionsForUser().collect { allSessions ->
                val todaySessions = allSessions.filter { session ->
                    session.dateTime?.substringBefore(",") == todayDateOnly
                }

                if (todaySessions.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                    binding.lineChart.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.lineChart.visibility = View.VISIBLE

                    displayHeaderStats(todaySessions)
                    setupLineChart(todaySessions)
                    setupRecyclerView(todaySessions)
                }
            }
        }
    }

    private fun displayHeaderStats(sessions: List<SessionData>) {
        binding.tvDate.text =
            "Date: " + SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())

        var totalSeconds = 0
        var totalAttention = 0f

        for (session in sessions) {
            val durationStr = session.duration ?: continue
            val regex = Regex("(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?")
            val match = regex.find(durationStr)

            if (match != null) {
                val hours = match.groupValues[1].toIntOrNull() ?: 0
                val minutes = match.groupValues[2].toIntOrNull() ?: 0
                val seconds = match.groupValues[3].toIntOrNull() ?: 0
                totalSeconds += hours * 3600 + minutes * 60 + seconds
            }

            totalAttention += session.attentionPercent?.toFloat() ?: 0f
        }

        val totalHours = totalSeconds / 3600
        val remainingSeconds = totalSeconds % 3600
        val totalMinutes = remainingSeconds / 60
        val totalRemSeconds = remainingSeconds % 60

        val durationText = when {
            totalHours > 0 -> "${totalHours}h ${totalMinutes}m ${totalRemSeconds}s"
            totalMinutes > 0 -> "${totalMinutes}m ${totalRemSeconds}s"
            else -> "${totalRemSeconds}s"
        }

        val avgAttention = if (sessions.isNotEmpty()) totalAttention / sessions.size else 0f

        binding.tvTotalDuration.text = "Total Duration: $durationText"
        binding.tvAttentionPercent.text = "Overall Attention: ${"%.1f".format(avgAttention)}%"
    }

    private fun setupLineChart(sessions: List<SessionData>) {
        // Map sessions to chart entries
        val entries = sessions.mapIndexed { index, session ->
            Log.d("LineChartDebug", "title=${session.title}, attentionPercent=${session.attentionPercent}")
            Entry(index.toFloat(), session.attentionPercent?.toFloat() ?: 0f)
        }

        // Create dataset
        val dataSet = LineDataSet(entries, "Attention %").apply {
            setDrawFilled(true)
            fillAlpha = 150
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_gradient)
            color = Color.BLUE
            valueTextColor = Color.BLACK
            lineWidth = 2f
            setCircleColor(Color.BLUE)
            circleRadius = 4f
            valueFormatter = PercentFormatter()
        }

        // Detect dark mode
        val isDarkMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isDarkMode) Color.WHITE else Color.BLACK

        // Apply chart settings
        binding.lineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false

            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 10f
                this.textColor = textColor
            }

            axisRight.isEnabled = false

            xAxis.apply {
                granularity = 1f
                setDrawLabels(false)
                this.textColor = textColor
            }

            legend.textColor = textColor

            invalidate()
        }

        // Set dataset value text color
        dataSet.valueTextColor = textColor
    }


    private fun setupRecyclerView(sessions: List<SessionData>) {
        val adapter = SessionAdapter(
            onItemClick = { session ->
                val intent = android.content.Intent(requireContext(), com.example.irlstudentattentiontracker.SessionDetailActivity::class.java)
                intent.putExtra("session_data", session)
                startActivity(intent)
            },
            onItemLongClick = { session ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Session")
                    .setMessage("Are you sure you want to delete \"${session.title}\"?")
                    .setPositiveButton("Yes") { _, _ ->
                        viewModel.deleteSessionFromFb(session)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        adapter.differ.submitList(sessions.reversed())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
