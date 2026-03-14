package com.example.rxcare.domain.model

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: MessageType = MessageType.TEXT,
    val imageUrl: String? = null,
    val isRead: Boolean = false,
    val isDelivered: Boolean = false
)

enum class MessageType {
    TEXT,
    IMAGE
}
