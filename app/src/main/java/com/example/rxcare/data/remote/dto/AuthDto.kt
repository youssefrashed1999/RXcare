package com.example.rxcare.data.remote.dto

import com.google.gson.annotations.SerializedName

// Request Models
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterPharmacistRequest(
    val email: String,
    val password: String,
    val name: String
)

// Response Models
data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("user_id")
    val userId: String,
    val role: String,
    val name: String
)

data class PharmacistCreatedResponse(
    val message: String,
    @SerializedName("user_id")
    val userId: String
)

// Chat & Request Models
data class CreatePrescriptionRequest(
    @SerializedName("image_url")
    val imageUrl: String?,
    val notes: String?
)

data class PrescriptionResponse(
    @SerializedName("chat_id")
    val chatId: String,
    val status: String
)

data class ChatStatusUpdateRequest(
    val status: String
)

data class ChatStatusResponse(
    @SerializedName("chat_id")
    val chatId: String,
    val status: String
)

// Chat List Response
data class ChatListResponse(
    @SerializedName("chat_id")
    val chatId: String,
    @SerializedName("patient_name")
    val patientName: String,
    val status: String,
    val timestamp: String,
    @SerializedName("latest_message")
    val latestMessage: MessageResponse?
)

// Message Response
data class MessageResponse(
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("sender_name")
    val senderName: String,
    val content: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    val timestamp: String
)

// Chat History Response
data class ChatHistoryResponse(
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("sender_name")
    val senderName: String,
    val content: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    val timestamp: String
)
