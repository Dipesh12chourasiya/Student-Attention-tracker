package com.example.irlstudentattentiontracker.viewmodel


import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.detectfaceandexpression.models.SessionData
import com.example.detectfaceandexpression.models.User
import com.example.irlstudentattentiontracker.models.ChatRequest
import com.example.irlstudentattentiontracker.models.ChatResponse
import com.example.irlstudentattentiontracker.models.Message
import com.example.irlstudentattentiontracker.retrofit.RetrofitClient
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserViewModel(application: Application) : AndroidViewModel(application) {

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

    fun fetchUsername(onResult: (String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(null)

        val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)

        dbRef.get().addOnSuccessListener { dataSnapshot ->
            val user = dataSnapshot.getValue(User::class.java)
            onResult(user?.username)
        }.addOnFailureListener {
            onResult(null)
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

        val sessionRef = db.child("AllSessions").child(userId)
            .push() // Generate a unique session ID under the user
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


    //retrofit and chatbot logic


    val isLoading = MutableLiveData<Boolean>()
    val aiResponse = MutableLiveData<String>()


    fun fetchRespponse(subjects: String, wakeUpTime:String, sleepTime:String) {
        isLoading.value = true

//        val prompt = "Generate today's timetable for whole day in markdown, " +
//                "Wake-up time: $wakeUpTime Sleep time: $sleepTime Subjects: $subjects" +
//                "Use this format:\n" +
//                "**Morning**\n" +
//                "- 9:00 AM – 10:00 AM – Subject Name" +
//                "- take break" +
//                "Only show the timetable. Use 12-hour format. No extra text."

        val prompt = """
Make a simple full day timetable in Markdown using these subjects: $subjects
My wakeup time is $wakeUpTime, sleep time is $sleepTime.
Use four parts: Morning, Noon, Evening, Night.
Format:
**Morning**
- 9:00 AM to 10 Am – subject1
- break
No intro or conclusion. 12-hour format.
""".trimIndent()


//        Message("user", "Generate a Whole Day timetable for today in points. Subjects: $userInput")

        val request = ChatRequest(
            messages = listOf(
//                Message("user", prompt)
                Message("user", "Generate a Whole Day timetable for today in points. i wake up at:$wakeUpTime, sleep at $sleepTime Subjects: $subjects, dont add intro or conclusion. 12-hour format.")
            )
        )

        RetrofitClient.api.getChatCompletion(request).enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                isLoading.value = false
                if (response.isSuccessful) {
                    val reply = response.body()?.choices?.firstOrNull()?.message?.content
                    aiResponse.value = reply ?: "No response received."
                } else {
                    aiResponse.value = "Error: ${response.errorBody()?.string()}"
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                isLoading.value = false
                aiResponse.value = "Failed: ${t.message}"
            }
        })
    }




}
