package com.example.rxcare.domain.model

data class Chat(
    val id: String,
    val participant1Id: String,
    val participant2Id: String,
    val lastMessage: Message? = null,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isArchived: Boolean = false,
    val status: String = ""
)
