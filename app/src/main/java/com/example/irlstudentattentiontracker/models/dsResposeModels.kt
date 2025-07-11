package com.example.irlstudentattentiontracker.models

data class Message(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String = "deepseek/deepseek-r1-0528:free",
    val messages: List<Message>
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
