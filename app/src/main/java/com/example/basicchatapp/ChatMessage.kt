package com.example.basicchatapp

data class ChatMessage(
    val text: String = "",
    val sender: String = "",
    val receiver: String = "",
    var senderName: String = "",
    val timestamp: Long = 0
)
