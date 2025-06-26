package com.example.irlstudentattentiontracker.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.irlstudentattentiontracker.HomeActivity
import com.example.irlstudentattentiontracker.MainActivity

import com.example.irlstudentattentiontracker.databinding.ActivityLoginBinding
import com.example.irlstudentattentiontracker.viewmodel.UserViewModel


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.loginUser(email, password)
        }

        authViewModel.authResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                // intent lagao to home screen
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        }

        authViewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        binding.signupText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        binding.forgotPasswordText.setOnClickListener {
            Toast.makeText(this, "Reset password feature not implemented", Toast.LENGTH_SHORT).show()
            // You can add reset password logic here
        }
    }

}
