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
import java.time.Instant
import java.time.format.DateTimeFormatter

class HomeViewModel(
    private val chatRepository: ChatRepository,
    private val currentUserId: String
) : ViewModel() {
    
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
        loadRequests()
        initializeWebSocketConnection()
        listenToWebSocketEvents()
        listenToConnectionStatus()
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
        chatRepository.claimChat(chatId, currentUserId)
    }

    private fun listenToConnectionStatus() {
        viewModelScope.launch {
            chatRepository.getWebSocketConnectionStatus().collect { status ->
                _connectionStatus.value = status
                if (status == com.example.rxcare.data.remote.websocket.WebSocketClient.ConnectionStatus.ERROR) {
                    _uiState.value = _uiState.value.copy(
                        error = "WebSocket connection lost. Attempting to reconnect..."
                    )
                }
            }
        }
    }

    fun disconnectWebSockets() {
        chatRepository.disconnectWebSockets()
    }

    private fun listenToWebSocketEvents() {
        viewModelScope.launch {
            chatRepository.getWebSocketEvents().collect { event ->
                when (event) {
                    is WebSocketEvent.NewPrescriptionRequest -> {
                        // Add new request to the list
                        val newChat = Chat(
                            id = event.event.chatId,
                            participant1Id = currentUserId,
                            participant2Id = "patient_${event.event.chatId}", // Generate ID since patientId not available
                            lastMessage = null,
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
                            _chats.value = currentChats + newChat
                        }
                    }
                    is WebSocketEvent.RequestClaimed -> {
                        // Update chat when claimed
                        _chats.value = _chats.value.map { chat ->
                            if (chat.id == event.event.chatId) {
                                chat.copy(
                                    lastMessageTime = parseTimestamp(event.event.timestamp),
                                    status = "CLAIMED"
                                )
                            } else chat
                        }
                    }
                    is WebSocketEvent.NewMessage -> {
                        // Update last message for the chat
                        _chats.value = _chats.value.map { chat ->
                            if (chat.id == event.event.chatId) {
                                val receiverId = if (event.event.senderId == currentUserId) {
                                    chat.participant2Id
                                } else {
                                    currentUserId
                                }
                                chat.copy(
                                    lastMessage = com.example.rxcare.domain.model.Message(
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
                            } else chat
                        }
                    }
                    is WebSocketEvent.ChatCompleted -> {
                        // Mark chat as archived when completed
                        _chats.value = _chats.value.map { chat ->
                            if (chat.id == event.event.chatId) {
                                chat.copy(
                                    isArchived = true,
                                    lastMessageTime = parseTimestamp(event.event.timestamp)
                                )
                            } else chat
                        }
                    }
                    is WebSocketEvent.PharmacistJoined -> {
                        // Update chat when pharmacist joins
                        _chats.value = _chats.value.map { chat ->
                            if (chat.id == event.event.chatId) {
                                chat.copy(
                                    lastMessageTime = parseTimestamp(event.event.timestamp)
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

    fun loadRequests(status: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Determine status based on user role
                val effectiveStatus = when (_userRole.value) {
                    "PHARMACIST" -> {
                        // Pharmacists only see PENDING requests by default
                        if (status == null) "PENDING" else status
                    }
                    "PATIENT" -> {
                        // Patients can see all their requests
                        status
                    }
                    else -> status
                }
                
                val result = chatRepository.getRequests(effectiveStatus)
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
                        
                        // Create a new chat object and add it to the list immediately
                        // The response contains both chatId and status from the API
                        val newChat = Chat(
                            id = prescriptionResponse.chatId,
                            participant1Id = currentUserId,
                            participant2Id = "New Prescription Request",
                            lastMessage = Message(
                                id = "temp_${System.currentTimeMillis()}",
                                chatId = prescriptionResponse.chatId,
                                senderId = currentUserId,
                                receiverId = "",
                                content = description,
                                timestamp = System.currentTimeMillis(),
                                messageType = MessageType.TEXT,
                                imageUrl = imageUri,
                                isRead = false,
                                isDelivered = false
                            ),
                            lastMessageTime = System.currentTimeMillis(),
                            unreadCount = 0,
                            isArchived = false,
                            status = prescriptionResponse.status
                        )
                        
                        // Add the new chat to the beginning of the list
                        _chats.value = listOf(newChat) + _chats.value
                        
                        // Also refresh the chat list to get the latest data
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
