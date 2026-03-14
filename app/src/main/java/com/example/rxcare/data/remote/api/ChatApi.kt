package com.example.rxcare.data.remote.api

import com.example.rxcare.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ChatApi {
    
    @POST("requests")
    suspend fun createPrescriptionRequest(@Body request: CreatePrescriptionRequest): Response<PrescriptionResponse>
    
    @GET("requests")
    suspend fun getRequests(@Query("status") status: String = "PENDING"): Response<List<ChatListResponse>>
    
    @GET("chats/{chat_id}/history")
    suspend fun getChatHistory(@Path("chat_id") chatId: String): Response<List<ChatHistoryResponse>>
    
    @PATCH("chats/{chat_id}/status")
    suspend fun updateChatStatus(
        @Path("chat_id") chatId: String,
        @Body request: ChatStatusUpdateRequest
    ): Response<ChatStatusResponse>
    
    @POST("upload/image")
    @Multipart
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): Response<ImageUploadResponse>
}
