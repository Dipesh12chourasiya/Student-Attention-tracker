package com.example.irlstudentattentiontracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.detectfaceandexpression.adapters.SessionAdapter
import com.example.irlstudentattentiontracker.databinding.ActivitySessionsOnDateBinding
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class SessionsOnDateActivity : AppCompatActivity() {
    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: SessionAdapter
    private lateinit var binding: ActivitySessionsOnDateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionsOnDateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val selectedDate = intent.getStringExtra("selectedDate")?.trim() ?: ""

        binding.dateOfNow.text = selectedDate

        adapter = SessionAdapter(
            onItemClick = { session ->
                val intent = Intent(this, SessionDetailActivity::class.java)
                intent.putExtra("session_data", session)
                startActivity(intent)
            },
            onItemLongClick = { session ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Session")
                    .setMessage("Are you sure you want to delete \"${session.title}\"?")
                    .setPositiveButton("Yes") { _, _ ->
                        viewModel.deleteSession(session)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        )

        // Now collect the filtered list
        lifecycleScope.launch {
            viewModel.getAllSessions().collect { allSessions ->

                val formatterFull = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
                val formatterDateOnly = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

                val filtered = allSessions.filter { session ->
                    try {
                        val sessionDate = formatterFull.parse(session.dateTime)
                        val sessionDateStr = formatterDateOnly.format(sessionDate!!)
                        sessionDateStr == selectedDate
                    } catch (e: Exception) {
                        false
                    }
                }

                adapter.differ.submitList(filtered.reversed()) // ✅ Use the AsyncListDiffer's submitList
            }
        }

        binding.rvSessions.adapter = adapter
        binding.rvSessions.layoutManager = LinearLayoutManager(this)

    }
}