package com.example.irlstudentattentiontracker.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import com.example.irlstudentattentiontracker.databinding.ActivitySignupBinding
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel



class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private val authViewModel: UserViewModel by viewModels() // ViewModel scoped to this Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createAccountButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.registerUser(username, email, password)
        }

        // Observe ViewModel LiveData
        authViewModel.authResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                // Redirect to dashboard or login
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        authViewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginText.setOnClickListener {
            // Navigate to Login screen
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
