package com.example.rxcare.domain.repository

import com.example.rxcare.domain.model.Chat
import com.example.rxcare.domain.model.Message
import com.example.rxcare.data.remote.dto.PrescriptionResponse
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ChatRepository {
    suspend fun createPrescriptionRequest(imageUrl: String, notes: String): Result<PrescriptionResponse>
    suspend fun createPrescriptionRequestWithImage(imageUri: String, notes: String): Result<PrescriptionResponse>
    suspend fun getRequests(status: String? = null): Result<List<Chat>>
    suspend fun getChatHistory(chatId: String): Result<List<Message>>
    suspend fun updateChatStatus(chatId: String, status: String): Result<Unit>
    suspend fun uploadImage(imageFile: File): Result<String> // Returns imageUrl
    fun getWebSocketEvents(): Flow<com.example.rxcare.data.remote.websocket.WebSocketEvent>
    fun getWebSocketConnectionStatus(): Flow<com.example.rxcare.data.remote.websocket.WebSocketClient.ConnectionStatus>
    fun connectWebSockets(userId: String, userRole: String, token: String)
    fun disconnectWebSockets()
    fun reconnectWebSockets(userId: String, userRole: String, token: String)
    fun claimChat(chatId: String, pharmacistId: String)
    fun sendMessage(chatId: String, senderId: String, content: String, imageUrl: String? = null)
    suspend fun getAuthToken(): String
    suspend fun getUserRole(): String
}
