package com.example.rxcare.data.remote.mapper

import com.example.rxcare.data.remote.dto.*
import com.example.rxcare.domain.model.Chat
import com.example.rxcare.domain.model.Message
import com.example.rxcare.domain.model.MessageType
import java.text.SimpleDateFormat
import java.util.*

object ChatMapper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

    fun mapToDomain(chatListResponse: ChatListResponse): Chat {
        val lastMessage = chatListResponse.latestMessage?.let { mapMessageToDomain(it) }
        val timestamp = parseTimestamp(chatListResponse.timestamp)

        return Chat(
            id = chatListResponse.chatId,
            participant1Id = "", // Not available in API response, will be set by ViewModel
            participant2Id = chatListResponse.patientName, // Use patient name as participant2Id
            lastMessage = lastMessage,
            lastMessageTime = lastMessage?.timestamp ?: timestamp,
            unreadCount = 0, // Not available in API response
            isArchived = chatListResponse.status == "DONE",
            status = chatListResponse.status
        )
    }

    fun mapToDomain(chatListResponses: List<ChatListResponse>): List<Chat> {
        return chatListResponses.map { mapToDomain(it) }
    }

    fun mapMessageToDomain(messageResponse: MessageResponse): Message {
        return Message(
            id = messageResponse.messageId,
            chatId = "", // Will be set by caller
            senderId = messageResponse.senderId,
            receiverId = "", // Not available in API response
            content = messageResponse.content ?: "",
            timestamp = parseTimestamp(messageResponse.timestamp),
            messageType = when {
                !messageResponse.imageUrl.isNullOrBlank() && !messageResponse.content.isNullOrBlank() -> MessageType.TEXT_WITH_IMAGE
                !messageResponse.imageUrl.isNullOrBlank() -> MessageType.IMAGE
                else -> MessageType.TEXT
            },
            imageUrl = messageResponse.imageUrl,
            isRead = false, // Not available in API response
            isDelivered = false // Not available in API response
        )
    }

    fun mapMessagesToDomain(messageResponses: List<ChatHistoryResponse>, chatId: String): List<Message> {
        return messageResponses.map { dto ->
            Message(
                id = dto.messageId,
                chatId = chatId,
                senderId = dto.senderId,
                receiverId = "", // Not available in API response
                content = dto.content ?: "",
                timestamp = parseTimestamp(dto.timestamp),
                messageType = when {
                !dto.imageUrl.isNullOrBlank() && !dto.content.isNullOrBlank() -> MessageType.TEXT_WITH_IMAGE
                !dto.imageUrl.isNullOrBlank() -> MessageType.IMAGE
                else -> MessageType.TEXT
            },
                imageUrl = dto.imageUrl,
                isRead = false, // Not available in API response
                isDelivered = false // Not available in API response
            )
        }
    }

    private fun parseTimestamp(timestamp: String): Long {
        return try {
            // Try parsing with milliseconds and timezone first (handles format like "2026-03-12T21:14:47.322431Z")
            val tzFormatWithMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            tzFormatWithMillis.timeZone = TimeZone.getTimeZone("UTC")
            tzFormatWithMillis.parse(timestamp)?.time ?: run {
                // Try parsing without milliseconds but with timezone
                val tzFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                tzFormat.timeZone = TimeZone.getTimeZone("UTC")
                tzFormat.parse(timestamp)?.time ?: run {
                    // Fallback to parsing without timezone
                    dateFormat.parse(timestamp)?.time ?: System.currentTimeMillis()
                }
            }
        } catch (e: Exception) {
            try {
                // Fallback to parsing without timezone
                dateFormat.parse(timestamp)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
