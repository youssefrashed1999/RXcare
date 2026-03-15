package com.example.rxcare.data.remote.websocket

import com.example.rxcare.data.remote.dto.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

class WebSocketClient(
    private val gson: Gson = Gson()
) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // No timeout for WebSocket
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    private var globalPharmacistSocket: WebSocket? = null
    private var userSocket: WebSocket? = null
    
    private val _events = MutableSharedFlow<WebSocketEvent>(extraBufferCapacity = 64)
    val events: Flow<WebSocketEvent> = _events.asSharedFlow()
    
    private val _connectionStatus = MutableSharedFlow<ConnectionStatus>()
    val connectionStatus: Flow<ConnectionStatus> = _connectionStatus.asSharedFlow()
    
    enum class ConnectionStatus {
        CONNECTED, DISCONNECTED, CONNECTING, ERROR
    }
    
    fun connectGlobalPharmacistChannel(token: String) {
        _connectionStatus.tryEmit(ConnectionStatus.CONNECTING)
        val url = "ws://rxcare-backend.onrender.com/ws/pharmacists/global?token=$token"
        
        val request = Request.Builder()
            .url(url)
            .build()
        
        globalPharmacistSocket = client.newWebSocket(request, createWebSocketListener("Global Pharmacist"))
    }
    
    fun connectUserChannel(userId: String, token: String) {
        _connectionStatus.tryEmit(ConnectionStatus.CONNECTING)
        val url = "ws://rxcare-backend.onrender.com/ws/users/$userId?token=$token"
        
        val request = Request.Builder()
            .url(url)
            .build()
        
        userSocket = client.newWebSocket(request, createWebSocketListener("User Channel"))
    }
    
    fun claimChat(chatId: String, pharmacistId: String) {
        val action = ClaimChatAction(
            chatId = chatId,
            pharmacistId = pharmacistId,
            timestamp = java.time.Instant.now().toString()
        )
        userSocket?.send(gson.toJson(action))
    }
    
    fun sendMessage(chatId: String, senderId: String, content: String, imageUrl: String? = null) {
        val action = SendMessageAction(
            chatId = chatId,
            senderId = senderId,
            content = content,
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis().toString()
        )
        userSocket?.send(gson.toJson(action))
    }
    
    fun ping() {
        userSocket?.send("{\"type\":\"ping\"}")
    }
    
    fun disconnect() {
        globalPharmacistSocket?.close(1000, "Client disconnect")
        userSocket?.close(1000, "Client disconnect")
        globalPharmacistSocket = null
        userSocket = null
        _connectionStatus.tryEmit(ConnectionStatus.DISCONNECTED)
    }
    
    fun reconnect(userId: String, userRole: String, token: String) {
        disconnect()
        if (userRole == "PHARMACIST") {
            connectGlobalPharmacistChannel(token)
        }
        connectUserChannel(userId, token)
    }
    
    private fun createWebSocketListener(channelName: String): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _events.tryEmit(WebSocketEvent.Connected)
                _connectionStatus.tryEmit(ConnectionStatus.CONNECTED)
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val jsonObject = gson.fromJson(text, com.google.gson.JsonObject::class.java)
                    val eventType = jsonObject.get("event")?.asString
                    
                    when (eventType) {
                        "new_prescription_request" -> {
                            val event = gson.fromJson(text, NewPrescriptionRequestEvent::class.java)
                            _events.tryEmit(WebSocketEvent.NewPrescriptionRequest(event))
                        }
                        "request_claimed" -> {
                            val event = gson.fromJson(text, RequestClaimedEvent::class.java)
                            _events.tryEmit(WebSocketEvent.RequestClaimed(event))
                        }
                        "pharmacist_joined" -> {
                            val event = gson.fromJson(text, PharmacistJoinedEvent::class.java)
                            _events.tryEmit(WebSocketEvent.PharmacistJoined(event))
                        }
                        "new_message" -> {
                            val event = gson.fromJson(text, NewMessageEvent::class.java)
                            _events.tryEmit(WebSocketEvent.NewMessage(event))
                        }
                        "chat_completed" -> {
                            val event = gson.fromJson(text, ChatCompletedEvent::class.java)
                            _events.tryEmit(WebSocketEvent.ChatCompleted(event))
                        }
                    }
                } catch (e: Exception) {
                    _events.tryEmit(WebSocketEvent.Error("Failed to parse WebSocket message: ${e.message}"))
                }
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _events.tryEmit(WebSocketEvent.Disconnecting)
                _connectionStatus.tryEmit(ConnectionStatus.DISCONNECTED)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _events.tryEmit(WebSocketEvent.Disconnected)
                _connectionStatus.tryEmit(ConnectionStatus.DISCONNECTED)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errorMessage = "WebSocket connection failed ($channelName): ${t.message ?: "Unknown error"}"
                _events.tryEmit(WebSocketEvent.Error(errorMessage))
                _connectionStatus.tryEmit(ConnectionStatus.ERROR)
            }
        }
    }
}

sealed class WebSocketEvent {
    object Connected : WebSocketEvent()
    object Disconnecting : WebSocketEvent()
    object Disconnected : WebSocketEvent()
    data class Error(val message: String) : WebSocketEvent()
    
    data class NewPrescriptionRequest(val event: NewPrescriptionRequestEvent) : WebSocketEvent()
    data class RequestClaimed(val event: RequestClaimedEvent) : WebSocketEvent()
    data class PharmacistJoined(val event: PharmacistJoinedEvent) : WebSocketEvent()
    data class NewMessage(val event: NewMessageEvent) : WebSocketEvent()
    data class ChatCompleted(val event: ChatCompletedEvent) : WebSocketEvent()
}
