package com.example.rxcare.data.repository

import android.content.Context
import com.example.rxcare.data.local.PreferencesManager
import com.example.rxcare.data.remote.mapper.ChatMapper
import com.example.rxcare.data.remote.websocket.WebSocketClient
import com.example.rxcare.domain.model.Chat
import com.example.rxcare.domain.model.Message
import com.example.rxcare.domain.repository.ChatRepository as DomainChatRepository
import com.example.rxcare.utils.UriUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

class ChatRepositoryImpl(
    private val apiChatRepository: ChatRepository,
    private val preferencesManager: PreferencesManager,
    private val context: Context,
    private val webSocketClient: WebSocketClient = WebSocketClient()
) : DomainChatRepository {
    
    override suspend fun createPrescriptionRequest(imageUrl: String, notes: String): Result<com.example.rxcare.data.remote.dto.PrescriptionResponse> {
        return apiChatRepository.createPrescriptionRequest(imageUrl, notes)
    }
    
    override suspend fun createPrescriptionRequestWithImage(imageUri: String, notes: String): Result<com.example.rxcare.data.remote.dto.PrescriptionResponse> {
        var tempFile: File? = null
        return try {
            // Convert URI to File using UriUtils
            tempFile = UriUtils.uriToFile(context, imageUri)
                ?: return Result.failure(Exception("Failed to process image URI"))
            
            // Upload the image first
            val uploadResult = uploadImage(tempFile)
            uploadResult.fold(
                onSuccess = { imageUrl ->
                    // Create prescription request with uploaded image URL
                    createPrescriptionRequest(imageUrl, notes)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // Clean up temporary file
            tempFile?.let { file ->
                try {
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    override suspend fun getRequests(status: String?): Result<List<Chat>> {
        return apiChatRepository.getRequests(status).map { chatListResponses ->
            ChatMapper.mapToDomain(chatListResponses)
        }
    }
    
    override suspend fun getChatHistory(chatId: String): Result<List<Message>> {
        return apiChatRepository.getChatHistory(chatId).map { messageResponses ->
            ChatMapper.mapMessagesToDomain(messageResponses, chatId)
        }
    }
    
    override suspend fun updateChatStatus(chatId: String, status: String): Result<Unit> {
        return apiChatRepository.updateChatStatus(chatId, status).map { }
    }
    
    override suspend fun uploadImage(imageFile: File): Result<String> {
        return apiChatRepository.uploadImage(imageFile)
    }
    
    override fun getWebSocketEvents(): Flow<com.example.rxcare.data.remote.websocket.WebSocketEvent> {
        return webSocketClient.events
    }
    
    override fun getWebSocketConnectionStatus(): Flow<com.example.rxcare.data.remote.websocket.WebSocketClient.ConnectionStatus> {
        return webSocketClient.connectionStatus
    }
    
    override fun connectWebSockets(userId: String, userRole: String, token: String) {
        if (userRole == "PHARMACIST") {
            webSocketClient.connectGlobalPharmacistChannel(token)
        }
        webSocketClient.connectUserChannel(userId, token)
    }
    
    override fun disconnectWebSockets() {
        webSocketClient.disconnect()
    }
    
    override fun reconnectWebSockets(userId: String, userRole: String, token: String) {
        webSocketClient.reconnect(userId, userRole, token)
    }
    
    override fun claimChat(chatId: String, pharmacistId: String) {
        webSocketClient.claimChat(chatId, pharmacistId)
    }
    
    override fun sendMessage(chatId: String, senderId: String, content: String, imageUrl: String?) {
        webSocketClient.sendMessage(chatId, senderId, content, imageUrl)
    }
    
    override suspend fun getAuthToken(): String {
        return preferencesManager.accessToken.first() ?: ""
    }
    
    override suspend fun getUserRole(): String {
        return preferencesManager.userRole.first() ?: ""
    }
}
