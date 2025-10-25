package com.example.irlstudentattentiontracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.detectfaceandexpression.adapters.SessionAdapter
import com.example.irlstudentattentiontracker.databinding.ActivityProfileBinding
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.lifecycle.lifecycleScope
import com.example.detectfaceandexpression.models.SessionData
import com.example.irlstudentattentiontracker.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: SessionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        this.window.statusBarColor = Color.BLUE
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)

        // Back button
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Fetch and show user data
        viewModel.getUserProfile { username, email ->
            binding.usernameTextView.text = username ?: "No name"
            binding.emailTextView.text = email ?: "No email"
        }

        lifecycleScope.launch {
            viewModel.getAllSessionsForUser().collect { sessions ->
                displayAchievements(sessions)
            }
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


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                // Handle logout here
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }




    private fun displayAchievements(sessions: List<SessionData>) {
        val achievements = mutableListOf<String>()

        // 1. Goal Setter: e.g., 2+ sessions per day
        val sessionsByDate = sessions.groupBy { it.dateTime.split(",")[0] } // group by date
        sessionsByDate.forEach { (_, dailySessions) ->
            if (dailySessions.size >= 2) achievements.add("Goal Setter")
        }

        // 2. Consistency Champ: streak of daily sessions
        val sortedDates = sessionsByDate.keys.map {
            SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).parse(it)?.time ?: 0L
        }.sorted()
        var streak = 1
        var maxStreak = 1
        for (i in 1 until sortedDates.size) {
            if (sortedDates[i] - sortedDates[i - 1] <= 24 * 60 * 60 * 1000L) { // consecutive days
                streak++
                if (streak > maxStreak) maxStreak = streak
            } else streak = 1
        }
        if (maxStreak >= 3) achievements.add("Consistency Champ")

        // 3. Attention Hero: highest attention percent in a session
        val maxAttention = sessions.maxOfOrNull { it.attentionPercent ?: 0 } ?: 0
        if (maxAttention >= 80) achievements.add("Attention Hero")

        // 4. Improver: improved attention over time
        if (sessions.size >= 2) {
            val first = sessions.first().attentionPercent ?: 0
            val last = sessions.last().attentionPercent ?: 0
            if (last > first) achievements.add("Improver")
        }

        // 5. Display using ViewBinding
        binding.achievementsTextView.text = if (achievements.isNotEmpty()) {
            achievements.distinct().joinToString(", ")
        } else {
            "No achievements yet"
        }
    }


}

