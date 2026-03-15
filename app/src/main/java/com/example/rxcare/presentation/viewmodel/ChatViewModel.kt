package com.example.rxcare.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rxcare.data.remote.websocket.WebSocketEvent
import com.example.rxcare.domain.model.Chat
import com.example.rxcare.domain.model.Message
import com.example.rxcare.domain.model.MessageType
import com.example.rxcare.domain.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat: StateFlow<Chat?> = _currentChat.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    init {
        // Don't load all chats in init - ChatViewModel should focus on specific chat
        observeWebSocketEvents()
    }

    private fun loadChats(status: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val result = chatRepository.getRequests(status)
                result.fold(
                    onSuccess = { chatList ->
                        _chats.value = chatList
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun selectChat(chatId: String, initialStatus: String? = null) {
        viewModelScope.launch {
            try {
                // Set initial status if provided
                initialStatus?.let { status ->
                    _uiState.value = _uiState.value.copy(chatStatus = status)
                }
                
                // Just load messages for the given chatId
                // The chat data should be passed from the home screen navigation
                loadMessages(chatId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun setInitialChatStatus(status: String) {
        _uiState.value = _uiState.value.copy(chatStatus = status)
    }

    fun setCurrentChat(chat: Chat) {
        _currentChat.value = chat
    }

    private fun loadMessages(chatId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                _currentChat.value = Chat(id = chatId, participant1Id = "", participant2Id = "")
                val result = chatRepository.getChatHistory(chatId)
                result.fold(
                    onSuccess = { messageList ->
                        // Sort messages by timestamp to ensure the prescription request is first
                        val sortedMessages = messageList.sortedBy { it.timestamp }
                        _messages.value = sortedMessages

                        // Log first message to verify it's the prescription request
                        if (sortedMessages.isNotEmpty()) {
                            val firstMessage = sortedMessages.first()
                            println("First message in chat $chatId: ${firstMessage.content} (has image: ${firstMessage.imageUrl != null})")
                        }

                        _uiState.value = _uiState.value.copy(isLoading = false)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isBlank() || _currentChat.value == null) return

        viewModelScope.launch {
            try {
                chatRepository.sendMessage(
                    chatId = _currentChat.value!!.id,
                    senderId = currentUserId,
                    content = text,
                    imageUrl = null
                )
                _messageText.value = ""
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    fun sendImage(imageFile: File) {
        if (_currentChat.value == null) return

        viewModelScope.launch {
            try {
                _isUploadingImage.value = true
                val uploadResult = chatRepository.uploadImage(imageFile)
                uploadResult.fold(
                    onSuccess = { imageUrl ->
                        chatRepository.sendMessage(
                            chatId = _currentChat.value!!.id,
                            senderId = currentUserId,
                            content = "",
                            imageUrl = imageUrl
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _isUploadingImage.value = false
            }
        }
    }

    fun sendMessageWithImage(imageFile: File, text: String) {
        if (_currentChat.value == null) return

        viewModelScope.launch {
            try {
                _isUploadingImage.value = true
                val uploadResult = chatRepository.uploadImage(imageFile)
                uploadResult.fold(
                    onSuccess = { imageUrl ->
                        chatRepository.sendMessage(
                            chatId = _currentChat.value!!.id,
                            senderId = currentUserId,
                            content = text,
                            imageUrl = imageUrl
                        )
                        _messageText.value = ""
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _isUploadingImage.value = false
            }
        }
    }

    fun claimChat(chatId: String) {
        viewModelScope.launch {
            try {
                chatRepository.claimChat(chatId, currentUserId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateChatStatus(chatId: String, status: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.updateChatStatus(chatId, status)
                result.fold(
                    onSuccess = {
                        // Refresh chat list
                        loadChats()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(error = error.message)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            chatRepository.getWebSocketEvents().collect { event ->
                when (event) {
                    is WebSocketEvent.NewMessage -> {
                        if (_currentChat.value?.id == event.event.chatId) {
                            val newMessage = Message(
                                id = event.event.messageId,
                                chatId = event.event.chatId,
                                senderId = event.event.senderId,
                                receiverId = "", // Not available in WebSocket event
                                content = event.event.content,
                                timestamp = System.currentTimeMillis(), // Use current time for real-time messages
                                messageType = when {
                                    !event.event.imageUrl.isNullOrBlank() && event.event.content.isNotBlank() -> MessageType.TEXT_WITH_IMAGE
                                    !event.event.imageUrl.isNullOrBlank() -> MessageType.IMAGE
                                    else -> MessageType.TEXT
                                },
                                imageUrl = event.event.imageUrl
                            )
                            // Add new message and resort to maintain chronological order
                            val updatedMessages = (_messages.value + newMessage).sortedBy { it.timestamp }
                            _messages.value = updatedMessages
                        }
                    }

                    is WebSocketEvent.ChatCompleted -> {
                        if (_currentChat.value?.id == event.event.chatId) {
                            _uiState.value = _uiState.value.copy(chatStatus = "COMPLETED",)
                            // Refresh current chat messages
                            loadMessages(event.event.chatId)
                        }
                    }

                    is WebSocketEvent.RequestClaimed -> {
                        // Refresh current chat if it's the one being claimed
                        if (_currentChat.value?.id == event.event.chatId) {
                            _uiState.value = _uiState.value.copy(chatStatus = "CLAIMED")
                            loadMessages(event.event.chatId)
                        }
                    }

                    is WebSocketEvent.PharmacistJoined -> {
                        // Refresh current chat if it's the one pharmacist joined
                        if (_currentChat.value?.id == event.event.chatId) {
                            _uiState.value = _uiState.value.copy(chatStatus = "CLAIMED")
                            loadMessages(event.event.chatId)
                        }
                    }

                    is WebSocketEvent.Error -> {}

                    else -> {
                        // Handle other events as needed
                    }
                }
            }
        }
    }

    fun onMessageTextChanged(text: String) {
        _messageText.value = text
    }

    fun searchChats(query: String) {
        if (query.isBlank()) {
            // Reset to show all chats - don't reload, just clear filter
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val filteredChats = _chats.value.filter { chat ->
                    chat.lastMessage?.content?.contains(query, ignoreCase = true) == true
                }
                _chats.value = filteredChats
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearCurrentChat() {
        _currentChat.value = null
        _messages.value = emptyList()
    }

    fun connectWebSockets(userRole: String, token: String) {
        chatRepository.connectWebSockets(currentUserId, userRole, token)
    }

    suspend fun getAuthToken(): String {
        return chatRepository.getAuthToken()
    }

    fun ensureWebSocketConnection(userRole: String, token: String) {
        // Check WebSocket connection status and reconnect if needed
        viewModelScope.launch {
            chatRepository.getWebSocketConnectionStatus().collect { status ->
                if (status == com.example.rxcare.data.remote.websocket.WebSocketClient.ConnectionStatus.DISCONNECTED ||
                    status == com.example.rxcare.data.remote.websocket.WebSocketClient.ConnectionStatus.ERROR
                ) {
                    chatRepository.reconnectWebSockets(currentUserId, userRole, token)
                }
            }
        }
    }

    fun disconnectWebSockets() {
        chatRepository.disconnectWebSockets()
    }

    fun completeChat(chatId: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.updateChatStatus(chatId, "DONE")
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            chatStatus = "DONE"
                        )
                    },
                    onFailure = { error -> }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to complete chat: ${e.message}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Don't disconnect WebSocket here - it should be managed at application level
        // WebSocket should remain connected for real-time updates across the app
    }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val chatStatus: String? = null
)
