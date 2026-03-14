package com.example.rxcare.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatListItem(
    @SerializedName("chat_id")
    val chatId: String,
    @SerializedName("patient_name")
    val patientName: String,
    val status: String,
    val timestamp: String,
    @SerializedName("latest_message")
    val latestMessage: MessageDto?
)

data class MessageDto(
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("sender_name")
    val senderName: String,
    val content: String,
    @SerializedName("image_url")
    val imageUrl: String?,
    val timestamp: String
)

data class ImageUploadResponse(
    @SerializedName("image_url")
    val imageUrl: String
)
