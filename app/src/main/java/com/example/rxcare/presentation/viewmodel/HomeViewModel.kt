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
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeFormatter

class HomeViewModel(
    private val chatRepository: ChatRepository,
    private val currentUserId: String
) : ViewModel() {
    
    // Handle new WebSocket messages immediately
    private fun handleNewMessage(event: WebSocketEvent.NewMessage) {
        val currentChats = _chats.value
        val updatedChats = mutableListOf<Chat>()
        var updatedChat: Chat? = null
        
        currentChats.forEach { chat ->
            if (chat.id == event.event.chatId) {
                val receiverId = if (event.event.senderId == currentUserId) {
                    chat.participant2Id
                } else {
                    currentUserId
                }
                updatedChat = chat.copy(
                    lastMessage = Message(
                        id = event.event.messageId,
                        chatId = event.event.chatId,
                        senderId = event.event.senderId,
                        receiverId = receiverId,
                        content = event.event.content,
                        timestamp = parseTimestamp(event.event.timestamp),
                        imageUrl = event.event.imageUrl
                    ),
                    lastMessageTime = parseTimestamp(event.event.timestamp)
                )
            } else {
                // Only add chats that don't match the updated chat
                updatedChats.add(chat)
            }
        }
        
        // Add the updated chat to the top if it was found
        updatedChat?.let { updatedChats.add(0, it) }
        
        _chats.value = updatedChats
    }
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()
    
    private val _userRole = MutableStateFlow<String>("")
    val userRole: StateFlow<String> = _userRole.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow<com.example.rxcare.data.remote.websocket.WebSocketClient.ConnectionStatus>(
        com.example.rxcare.data.remote.websocket.WebSocketClient.ConnectionStatus.DISCONNECTED
    )
    val connectionStatus: StateFlow<com.example.rxcare.data.remote.websocket.WebSocketClient.ConnectionStatus> = _connectionStatus.asStateFlow()

    private fun parseTimestamp(timestamp: String): Long {
        return try {
            Instant.parse(timestamp).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    init {
        initializeWebSocketConnection()
        loadRequests()
        listenToWebSocketEvents()
        listenToConnectionStatus()
        startPingTimer()
    }
    
    private fun startPingTimer() {
        viewModelScope.launch {
            while (true) {
                delay(60000) // 1 minute
                val currentStatus = connectionStatus.value
                if (currentStatus == com.example.rxcare.data.remote.websocket.WebSocketClient.ConnectionStatus.CONNECTED) {
                    chatRepository.ping()
                }
            }
        }
    }
    
    private fun initializeWebSocketConnection() {
        viewModelScope.launch {
            try {
                // Get auth token and user role from repository
                val token = chatRepository.getAuthToken()
                val role = chatRepository.getUserRole()
                _userRole.value = role
                
                if (token.isNotEmpty() && role.isNotEmpty()) {
                    connectWebSockets(role, token)
                } else {
                    val errorMsg = if (token.isEmpty()) "Authentication token not found" else "User role not found"
                    _uiState.value = _uiState.value.copy(
                        error = "$errorMsg. Please login again."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to initialize WebSocket connection: ${e.message}"
                )
            }
        }
    }
    
    fun connectWebSockets(userRole: String, token: String) {
        chatRepository.connectWebSockets(currentUserId, userRole, token)
    }
    
    fun reconnectWebSockets(userRole: String, token: String) {
        chatRepository.reconnectWebSockets(currentUserId, userRole, token)
    }
    
    fun claimChat(chatId: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.updateChatStatus(chatId, "CLAIMED")
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            error = "Request claimed successfully"
                        )
                        // Refresh the chat list to show updated status
                        loadRequests()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to claim request: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to claim request: ${e.message}"
                )
            }
        }
    }

    private fun listenToConnectionStatus() {
        viewModelScope.launch {
            chatRepository.getWebSocketConnectionStatus().collect { status ->
                _connectionStatus.value = status
                when (status) {
                    com.example.rxcare.data.remote.websocket.WebSocketClient.ConnectionStatus.ERROR,
                    com.example.rxcare.data.remote.websocket.WebSocketClient.ConnectionStatus.DISCONNECTED -> {
                        // Silent reconnection - don't notify UI
                        attemptReconnection()
                    }
                    com.example.rxcare.data.remote.websocket.WebSocketClient.ConnectionStatus.CONNECTED -> {
                        // Clear any previous errors silently
                        _uiState.value = _uiState.value.copy(error = null)
                    }
                    else -> { /* Other statuses */ }
                }
            }
        }
    }
    
    private fun attemptReconnection() {
        viewModelScope.launch {
            try {
                // Wait a bit before reconnecting to avoid rapid reconnections
                kotlinx.coroutines.delay(3000)
                val token = chatRepository.getAuthToken()
                val role = chatRepository.getUserRole()
                
                if (token.isNotEmpty() && role.isNotEmpty()) {
                    reconnectWebSockets(role, token)
                }
            } catch (e: Exception) {
                // Silent failure - don't notify UI
            }
        }
    }

    fun disconnectWebSockets() {
        chatRepository.disconnectWebSockets()
    }

    suspend fun getAuthToken(): String {
        return chatRepository.getAuthToken()
    }

    suspend fun getUserRole(): String {
        return chatRepository.getUserRole()
    }

    private fun listenToWebSocketEvents() {
        viewModelScope.launch {
            chatRepository.getWebSocketEvents().collect { event ->
                when (event) {
                    is WebSocketEvent.NewMessage -> {
                        handleNewMessage(event)
                    }
                    is WebSocketEvent.NewPrescriptionRequest -> {
                        // Add new request to the list
                        val newChat = Chat(
                            id = event.event.chatId,
                            participant1Id = currentUserId,
                            participant2Id = event.event.patientName, // Generate ID since patientId not available
                            lastMessage = Message(
                                id = event.event.chatId,
                                senderId = event.event.patientName,
                                content = event.event.notes,
                                imageUrl = event.event.imageUrl,
                                timestamp = parseTimestamp(event.event.timestamp),
                                chatId = event.event.chatId,
                                receiverId = event.event.chatId
                            ),
                            lastMessageTime = parseTimestamp(event.event.timestamp),
                            status = "PENDING"
                        )
                        val currentChats = _chats.value
                        val existingChat = currentChats.find { it.id == event.event.chatId }
                        
                        if (existingChat != null) {
                            // Update existing chat
                            _chats.value = currentChats.map { chat ->
                                if (chat.id == event.event.chatId) {
                                    newChat
                                } else {
                                    chat.copy(
                                        participant1Id = if (chat.participant1Id == currentUserId) currentUserId else chat.participant1Id,
                                        participant2Id = if (chat.participant2Id == currentUserId) currentUserId else chat.participant2Id
                                    )
                                }
                            }
                        } else {
                            // Add new chat to the list
                            _chats.value = listOf(newChat) + currentChats
                        }
                    }
                    is WebSocketEvent.RequestClaimed -> {
                        // Update chat when claimed
                        loadRequests()
                    }
                    is WebSocketEvent.ChatCompleted -> {
                        _chats.value = _chats.value.map { chat ->
                            if (chat.id == event.event.chatId) {
                                chat.copy(
                                    isArchived = true,
                                    lastMessageTime = parseTimestamp(event.event.timestamp),
                                    status = "DONE"
                                )
                            } else chat
                        }
                    }
                    is WebSocketEvent.PharmacistJoined -> {
                        _chats.value = _chats.value.map { chat ->
                            if (chat.id == event.event.chatId) {
                                chat.copy(
                                    lastMessageTime = parseTimestamp(event.event.timestamp),
                                    participant2Id = event.event.pharmacistName,
                                    status = "CLAIMED"
                                )
                            } else chat
                        }
                    }
                    else -> {
                        // Handle other events if needed
                    }
                }
            }
        }
    }

    fun loadRequests(status: String? = null, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            try {
                if (_userRole.value == "PHARMACIST") {
                    // For pharmacists, load all 3 request types separately
                    val allRequests = mutableListOf<Chat>()
                    
                    // Load PENDING requests
                    val pendingResult = chatRepository.getRequests("PENDING")
                    pendingResult.onSuccess { allRequests.addAll(it) }
                    
                    // Load CLAIMED requests  
                    val claimedResult = chatRepository.getRequests("CLAIMED")
                    claimedResult.onSuccess { allRequests.addAll(it) }
                    
                    // Load DONE requests
                    val doneResult = chatRepository.getRequests("DONE")
                    doneResult.onSuccess { allRequests.addAll(it) }
                    
                    // Sort by timestamp (newest first)
                    _chats.value = allRequests.sortedByDescending { it.lastMessageTime }
                    if (!silent) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                } else {
                    // For patients, use the provided status
                    val result = chatRepository.getRequests(status)
                    result.fold(
                        onSuccess = { chatList ->
                            _chats.value = chatList
                            if (!silent) {
                                _uiState.value = _uiState.value.copy(isLoading = false)
                            }
                        },
                        onFailure = { error ->
                            if (!silent) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = error.message
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                if (!silent) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun searchChats(query: String) {
        _searchQuery.value = query
        
        if (query.isBlank()) {
            loadRequests()
            return
        }
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                val filteredChats = _chats.value.filter { chat ->
                    chat.lastMessage?.content?.contains(query, ignoreCase = true) == true
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    searchResults = filteredChats
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.value = _uiState.value.copy(searchResults = emptyList())
    }

    fun createPrescriptionRequest(description: String, imageUri: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val result = if (imageUri != null) {
                    // Use the new method that handles content URIs properly
                    chatRepository.createPrescriptionRequestWithImage(imageUri, description)
                } else {
                    // Create request without image
                    chatRepository.createPrescriptionRequest("", description)
                }
                
                result.fold(
                    onSuccess = { prescriptionResponse ->
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        
                        // Just refresh the chat list to get the proper timestamp from server
                        loadRequests()
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchResults: List<Chat> = emptyList()
)
