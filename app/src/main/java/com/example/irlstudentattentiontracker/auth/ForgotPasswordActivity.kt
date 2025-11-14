package com.example.irlstudentattentiontracker.auth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.irlstudentattentiontracker.R
import com.example.irlstudentattentiontracker.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnSendReset.setOnClickListener {
            val email = binding.etForgotEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvBackToLogin.setOnClickListener {
            finish() // go back to login screen
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Reset link sent to your email!",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Failed to send reset email",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
