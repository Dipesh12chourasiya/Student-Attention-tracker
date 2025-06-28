package com.example.irlstudentattentiontracker.viewmodel


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.detectfaceandexpression.models.User
import com.example.irlstudentattentiontracker.roomDB.AppDatabase
import com.example.irlstudentattentiontracker.roomDB.SessionEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel(application: Application): AndroidViewModel(application) {

    // Room data base
    private val sessionDao = AppDatabase.getDatabase(application).sessionDao()

    fun saveSession(session: SessionEntity) {
        viewModelScope.launch {
            sessionDao.insertSession(session)
        }
    }

    private val _sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessions: StateFlow<List<SessionEntity>> = _sessions

    fun getAllSessions(): Flow<List<SessionEntity>> {
        return sessionDao.getAllSessions()
    }

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch {
            sessionDao.deleteSession(session)
        }
    }

    fun deleteAllSessions() {
        viewModelScope.launch {
            sessionDao.deleteAll()
        }
    }











    // firebase

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private val _authResult = MutableLiveData<Boolean>()
    val authResult: LiveData<Boolean> get() = _authResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // ✅ Register new user
    fun registerUser(username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    firebaseUser?.let {
                        saveUserToDatabase(it, username)
                    }
                } else {
                    _errorMessage.value = task.exception?.message
                    _authResult.value = false
                }
            }
    }

    // ✅ Save user to Realtime Database
    private fun saveUserToDatabase(firebaseUser: FirebaseUser, username: String) {
        val user = User(
            uid = firebaseUser.uid,
            username = username,
            email = firebaseUser.email ?: ""
        )

        database.child("Users").child(firebaseUser.uid).setValue(user)
            .addOnSuccessListener {
                _authResult.value = true
            }
            .addOnFailureListener {
                _errorMessage.value = it.message
                _authResult.value = false
            }
    }

    // ✅ Login
    fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = true
                } else {
                    _errorMessage.value = task.exception?.message
                    _authResult.value = false
                }
            }
    }

    // ✅ Logout
    fun logout() {
        auth.signOut()
    }

    // ✅ Get current user
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}
