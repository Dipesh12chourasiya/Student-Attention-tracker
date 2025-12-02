package com.example.irlstudentattentiontracker.fragments



import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.irlstudentattentiontracker.MainActivity

class StartSessionFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start MainActivity immediately
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)

        // Optional: close the parent activity if needed
//        activity?.finish()
    }

    // No need to inflate any layout
    override fun onCreateView(
        inflater: android.view.LayoutInflater, container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        return null
    }
}
