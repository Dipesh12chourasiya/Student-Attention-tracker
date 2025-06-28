package com.example.irlstudentattentiontracker

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.detectfaceandexpression.models.SessionData
import com.example.irlstudentattentiontracker.databinding.ActivityStatsBinding
import com.example.irlstudentattentiontracker.roomDB.SessionEntity
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel


import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.database.FirebaseDatabase


class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private val viewModel: UserViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("Track", "Stats started")

        // Toolbar back button
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val totalFaces = intent.getIntExtra("totalFaces", 0)
        val attentiveCount = intent.getIntExtra("attentiveCount", 0)
        val attentionPercent = intent.getIntExtra("attentionPercent", 0)
        val sessionDuration = intent.getStringExtra("sessionDuration") ?: "0s"
        val sessionDate = intent.getStringExtra("sessionTimestamp") ?: "N/A"
        val totalFrames = intent.getIntExtra("totalFrames",0)

        val startTime = intent.getStringExtra("startTime")
        val endTime = intent.getStringExtra("endTime")
        val sessionTimestamp = intent.getStringExtra("sessionTimestamp")


        binding.tvTotalFaces.text = "Total Faces: $totalFaces"
        binding.tvAttentivePercent.text = "Attentive: $attentionPercent%"
        binding.tvSessionDuration.text = "Duration: $sessionDuration"
        binding.tvSessionDate.text = " $sessionDate"
        binding.tvStartTime.text = "Start Time: $startTime"
        binding.tvEndTime.text = "End Time: $endTime"

        setupPieChart(attentiveCount, totalFrames - attentiveCount)


        binding.btnSave.setOnClickListener {
            val sessionName = binding.etSessionName.text.toString().trim()
            val note = binding.etAddNote.text.toString().trim()

            if (sessionName.isEmpty()) {
                Toast.makeText(this, "Please enter session name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val session = SessionData(
                userId = viewModel.getCurrentUserID(),
                title = sessionName,
                dateTime = sessionDate,
                duration = sessionDuration,
                startTime = startTime!!, // You'll need to pass this from MainActivity
                endTime = endTime!!, // Add helper to get current time if needed
                attentionPercent = attentionPercent,
                totalFaces = totalFaces,
                totalFrames = totalFrames,
                inattentiveCount = totalFrames - attentiveCount,
                attentiveCount = attentiveCount,
                notes = if (note.isNotEmpty()) note else null
            )

            viewModel.saveSessionToFirebase(session)

            binding.etSessionName.setText("")
            binding.etAddNote.setText("")

            Toast.makeText(this, "Session saved!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, HomeActivity::class.java))
        }

    }



    private fun setupPieChart(attentive: Int, inattentive: Int) {

        // 1. Data Entries
        val entries = listOf(
            PieEntry(attentive.toFloat(), "Attentive"), // Simplified labels, emojis can sometimes be inconsistent
            PieEntry(inattentive.toFloat(), "Inattentive")
        )

        // 2. DataSet Configuration
        val dataSet = PieDataSet(entries, "") // Legend label for dataSet can be empty if legend shows slice labels directly
        dataSet.sliceSpace = 2f // Slightly reduced slice space for a more compact look
        dataSet.selectionShift = 5f // How much a slice moves out when selected (optional, but can add interaction)


        val colors = listOf(
            resources.getColor(R.color.chart_acolor_attentive), // e.g., #4CAF50 (Green)
            resources.getColor(R.color.chart_color_inattentive) // e.g., #F44336 (Red)

        )
        dataSet.colors = colors

        // 3. PieData Configuration
        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter()) // Format values as percentages
        pieData.setValueTextSize(12f) // Slightly smaller value text for less clutter
        pieData.setValueTextColor(Color.WHITE) // White text for better contrast on colored slices
        pieData.setValueTypeface(Typeface.DEFAULT_BOLD) // Bold values for emphasis

        // 4. PieChart Customization
        val pieChart = binding.attentionPieChart

        pieChart.data = pieData

        // General Chart Settings
        pieChart.description.isEnabled = false // Keep description disabled for a cleaner look
        pieChart.setUsePercentValues(true) // Display values as percentages
        pieChart.setExtraOffsets(20f, 20f, 20f, 20f) // Add extra space around the chart

        // Hole and Center Text
        pieChart.isDrawHoleEnabled = true // Enable the hole
        pieChart.holeRadius = 58f // Size of the inner hole (e.g., 58%)
        pieChart.transparentCircleRadius = 61f // Size of the transparent circle outside the hole (adds a subtle glow)
        pieChart.setHoleColor(Color.TRANSPARENT) // Make the hole transparent or a light background color

        // Center text customization
        pieChart.setDrawCenterText(true)
        pieChart.centerText = "Attention\nStatus" // Multi-line center text for better aesthetics
        pieChart.setCenterTextSize(16f)
        pieChart.setCenterTextColor(Color.DKGRAY) // Darker gray for center text
        pieChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD)

        // Entry Label Customization (labels on the slices)
        pieChart.setDrawEntryLabels(false) // Often, the legend is sufficient for labels, or you can draw them if space allows
        // If you do want entry labels:
        // pieChart.setEntryLabelColor(Color.BLACK)
        // pieChart.setEntryLabelTextSize(10f)
        // pieChart.setEntryLabelTypeface(Typeface.DEFAULT)

        // Animation
        pieChart.animateY(1400) // Slightly longer animation for a smoother feel

        // 5. Legend Customization
        val legend = pieChart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false) // Draw legend outside the chart
        legend.xEntrySpace = 8f // Space between legend entries
        legend.yEntrySpace = 0f // No vertical space between entries
        legend.yOffset = 10f // Offset from the bottom of the chart
        legend.textSize = 13f
        legend.textColor = Color.DKGRAY // Darker gray for legend text
        legend.isWordWrapEnabled = true // Enable word wrap for longer labels
        legend.form = Legend.LegendForm.CIRCLE // Use circles as legend forms

        // 6. Interaction (optional, but adds to "professionalism")
        pieChart.setTouchEnabled(true)
        pieChart.isHighlightPerTapEnabled = true // Enable highlighting on tap

        // 7. Refresh
        pieChart.invalidate() // Refresh the chart to apply all changes
    }

}
