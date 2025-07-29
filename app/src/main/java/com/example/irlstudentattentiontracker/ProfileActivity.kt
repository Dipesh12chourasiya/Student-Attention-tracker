package com.example.irlstudentattentiontracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.detectfaceandexpression.adapters.SessionAdapter
import com.example.irlstudentattentiontracker.databinding.ActivityProfileBinding
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.lifecycle.lifecycleScope

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: SessionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        this.window.statusBarColor = Color.BLUE
        setContentView(binding.root)

        // Back button
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Fetch and show user data
        viewModel.getUserProfile { username, email ->
            binding.usernameTextView.text = username ?: "No name"
            binding.emailTextView.text = email ?: "No email"
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        adapter = SessionAdapter(
            onItemClick = { session ->
                val intent = Intent(this, SessionDetailActivity::class.java)
                intent.putExtra("session_data", session)
                startActivity(intent)
            },
            onItemLongClick = { /* No need for now */ }
        )

        binding.rvSessions.layoutManager = LinearLayoutManager(this)
        binding.rvSessions.adapter = adapter

        viewModel.getAllSessionsForUser().onEach { sessions ->
            binding.tvEmptyState.visibility =
                if (sessions.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            adapter.differ.submitList(sessions.reversed())
        }.launchIn(lifecycleScope)
    }
}

