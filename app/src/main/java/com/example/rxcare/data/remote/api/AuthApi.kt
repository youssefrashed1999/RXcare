package com.example.rxcare.data.remote.api

import com.example.rxcare.data.remote.dto.*
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("pharmacists/register")
    suspend fun registerPharmacist(@Body request: RegisterPharmacistRequest): Response<PharmacistCreatedResponse>
}
