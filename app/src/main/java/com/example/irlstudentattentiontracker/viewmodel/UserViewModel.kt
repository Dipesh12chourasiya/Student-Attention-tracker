package com.example.irlstudentattentiontracker.viewmodel


import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.detectfaceandexpression.models.SessionData
import com.example.detectfaceandexpression.models.User
import com.example.irlstudentattentiontracker.roomDB.AppDatabase
import com.example.irlstudentattentiontracker.roomDB.SessionEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
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

    // get current user ID
    fun getCurrentUserID(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

 // save session in /AllSessions path
 fun saveSessionToFirebase(session: SessionData) {
     val userId = getCurrentUserID() ?: return // Get the currently logged-in user's ID
     val db = FirebaseDatabase.getInstance().reference

     val sessionRef = db.child("AllSessions").child(userId).push() // Generate a unique session ID under the user
     val sessionId = sessionRef.key ?: return

     val sessionWithId = session.copy(sessionId = sessionId, userId = userId)

     sessionRef.setValue(sessionWithId)
         .addOnSuccessListener {
             Log.d("Firebase", "Session saved successfully")
         }
         .addOnFailureListener { e ->
             Log.e("Firebase", "Failed to save session: ${e.message}")
         }
 }


    // to get all sessions of Current User
    fun getAllSessionsForUser(): Flow<List<SessionData>> = callbackFlow {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val sessionsRef = FirebaseDatabase.getInstance()
            .getReference("AllSessions")
            .child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessions = mutableListOf<SessionData>()
                for (child in snapshot.children) {
                    val session = child.getValue(SessionData::class.java)
                    session?.let { sessions.add(it) }
                }
                trySend(sessions)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        sessionsRef.addValueEventListener(listener)

        awaitClose {
            sessionsRef.removeEventListener(listener)
        }
    }


    // delete session
    fun deleteSessionFromFb(session: SessionData) {
        val userId = getCurrentUserID() ?: return
        val sessionId = session.sessionId ?: return

        FirebaseDatabase.getInstance()
            .getReference("AllSessions")
            .child(userId)
            .child(sessionId)
            .removeValue()
    }

// delete all
    fun deleteAllSessionsFromFirebase() {
        val userId = getCurrentUserID() ?: return

        val dbRef = FirebaseDatabase.getInstance()
            .getReference("AllSessions")
            .child(userId)

        dbRef.removeValue()
    }


    // UserViewModel.kt
    fun getUserProfile(onResult: (username: String?, email: String?) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)
                onResult(username, email)
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(null, null) // Optionally handle errors better
            }
        })
    }


}
