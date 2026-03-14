package com.example.rxcare.data.repository

import android.util.Log
import com.example.rxcare.data.local.PreferencesManager
import com.example.rxcare.data.remote.NetworkClient
import com.example.rxcare.data.remote.api.ChatApi
import com.example.rxcare.data.remote.dto.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Response
import java.io.File

class ChatRepository(
    private val preferencesManager: PreferencesManager
) {
    
    private var cachedToken: String? = null
    
    private val chatApi: ChatApi by lazy {
        NetworkClient.createChatApi { cachedToken }
    }
    
    suspend fun createPrescriptionRequest(imageUrl: String, notes: String): Result<PrescriptionResponse> {
        refreshAuthToken()
        return try {
            val request = CreatePrescriptionRequest(imageUrl, notes)
            val response = chatApi.createPrescriptionRequest(request)
            
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to create prescription request: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRequests(status: String? = null): Result<List<ChatListResponse>> {
        refreshAuthToken()
        return try {
            val response = if (status != null) {
                chatApi.getRequests(status)
            } else {
                chatApi.getRequests()
            }
            
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to get requests: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getChatHistory(chatId: String): Result<List<ChatHistoryResponse>> {
        refreshAuthToken()
        return try {
            val response = chatApi.getChatHistory(chatId)
            
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to get chat history: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateChatStatus(chatId: String, status: String): Result<ChatStatusResponse> {
        refreshAuthToken()
        return try {
            val request = ChatStatusUpdateRequest(status)
            val response = chatApi.updateChatStatus(chatId, request)
            
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to update chat status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadImage(imageFile: File): Result<String> {
        refreshAuthToken()
        
        // Debug logging
        Log.d("ImageUpload", "Uploading file: ${imageFile.name}, size: ${imageFile.length()} bytes")
        
        return try {
            val multipartBody = createMultipartBody(imageFile)
            val response = chatApi.uploadImage(multipartBody)
            
            Log.d("ImageUpload", "Response code: ${response.code()}")
            if (response.isSuccessful) {
                response.body()?.let { 
                    Log.d("ImageUpload", "Upload successful: ${it.imageUrl}")
                    Result.success(it.imageUrl)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ImageUpload", "Upload failed: ${response.code()} - $errorBody")
                Result.failure(Exception("Failed to upload image: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ImageUpload", "Upload exception", e)
            Result.failure(e)
        }
    }
    
    private fun handleUploadResponse(response: Response<ImageUploadResponse>): Result<String> {
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            Log.e("ImageUpload", "Upload failed: ${response.code()} - $errorBody")
        }
        
        return if (response.isSuccessful) {
            response.body()?.let { 
                Log.d("ImageUpload", "Upload successful: ${it.imageUrl}")
                Result.success(it.imageUrl)
            } ?: Result.failure(Exception("Empty response body"))
        } else {
            val errorBody = response.errorBody()?.string()
            Result.failure(Exception("Failed to upload image: ${response.code()} - $errorBody"))
        }
    }
    
    private fun createMultipartBody(file: File): okhttp3.MultipartBody.Part {
        val mimeType = getMimeType(file) ?: "image/jpeg"
        val requestBody = okhttp3.RequestBody.create(
            mimeType.toMediaType(),
            file
        )
        
        // Create multipart with explicit filename
        val filename = file.name.ifEmpty { "image.jpg" }
        return okhttp3.MultipartBody.Part.createFormData("file", filename, requestBody)
    }
    
    private fun getMimeType(file: File): String? {
        return when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> null
        }
    }
    
    private suspend fun getAuthToken(): String? {
        cachedToken = preferencesManager.accessToken.first()
        return cachedToken
    }
    
    suspend fun refreshAuthToken() {
        cachedToken = preferencesManager.accessToken.first()
    }
}
