package com.example.rxcare.data.remote.dto

import com.google.gson.annotations.SerializedName

// WebSocket Events

data class NewPrescriptionRequestEvent(
    val event: String = "new_prescription_request",
    @SerializedName("chat_id")
    val chatId: String,
    @SerializedName("patient_name")
    val patientName: String,
    val notes: String,
    @SerializedName("image_url")
    val imageUrl: String,
    val timestamp: String
)

data class RequestClaimedEvent(
    val event: String = "request_claimed",
    @SerializedName("chat_id")
    val chatId: String,
    @SerializedName("patient_name")
    val patientName: String,
    @SerializedName("claimed_by_name")
    val claimedByName: String,
    val timestamp: String
)

data class PharmacistJoinedEvent(
    val event: String = "pharmacist_joined",
    @SerializedName("chat_id")
    val chatId: String,
    @SerializedName("pharmacist_name")
    val pharmacistName: String,
    val timestamp: String
)

data class NewMessageEvent(
    val event: String = "new_message",
    @SerializedName("chat_id")
    val chatId: String,
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

data class ChatCompletedEvent(
    val event: String = "chat_completed",
    @SerializedName("chat_id")
    val chatId: String,
    val message: String,
    val timestamp: String
)

// WebSocket Actions (Client -> Server)

data class ClaimChatAction(
    val action: String = "claim_chat",
    @SerializedName("chat_id")
    val chatId: String,
    @SerializedName("pharmacist_id")
    val pharmacistId: String,
    val timestamp: String
)

data class SendMessageAction(
    val action: String = "send_message",
    @SerializedName("chat_id")
    val chatId: String,
    @SerializedName("sender_id")
    val senderId: String,
    val content: String,
    @SerializedName("image_url")
    val imageUrl: String?,
    val timestamp: String
)
