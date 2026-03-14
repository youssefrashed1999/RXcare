package com.example.rxcare.data.repository

import com.example.rxcare.data.local.PreferencesManager
import com.example.rxcare.data.remote.NetworkClient
import com.example.rxcare.data.remote.api.AuthApi
import com.example.rxcare.data.remote.dto.AuthResponse
import com.example.rxcare.data.remote.dto.LoginRequest
import com.example.rxcare.data.remote.dto.RegisterPharmacistRequest
import com.example.rxcare.data.remote.dto.RegisterRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AuthRepository(
    private val preferencesManager: PreferencesManager
) {
    
    private var cachedToken: String? = null
    
    private val authApi: AuthApi by lazy {
        NetworkClient.createAuthApi { cachedToken }
    }
    
    suspend fun register(email: String, password: String, name: String): Result<AuthResponse> {
        refreshAuthToken()
        return try {
            val request = RegisterRequest(email, password, name)
            val response = authApi.register(request)
            
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    saveUserSession(authResponse)
                    Result.success(authResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Registration failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        refreshAuthToken()
        return try {
            val request = LoginRequest(email, password)
            val response = authApi.login(request)
            
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    saveUserSession(authResponse)
                    Result.success(authResponse)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun registerPharmacist(email: String, password: String, name: String): Result<Unit> {
        return try {
            val request = RegisterPharmacistRequest(email, password, name)
            val response = authApi.registerPharmacist(request)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Pharmacist registration failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun logout() {
        cachedToken = null
        preferencesManager.clearUserSession()
    }
    
    fun isLoggedIn(): Flow<Boolean> = preferencesManager.isLoggedIn
    
    fun getCurrentUser(): Flow<AuthResponse?> = preferencesManager.userId.map { userId ->
        if (userId != null) {
            AuthResponse(
                accessToken = preferencesManager.accessToken.first() ?: "",
                userId = userId,
                role = preferencesManager.userRole.first() ?: "",
                name = preferencesManager.userName.first() ?: ""
            )
        } else null
    }
    
    private suspend fun saveUserSession(authResponse: AuthResponse) {
        cachedToken = authResponse.accessToken
        preferencesManager.saveUserSession(
            userId = authResponse.userId,
            userName = authResponse.name,
            userEmail = "", // Not available in auth response
            userRole = authResponse.role,
            accessToken = authResponse.accessToken
        )
    }
    
    private suspend fun getAuthToken(): String? {
        cachedToken = preferencesManager.accessToken.first()
        return cachedToken
    }
    
    suspend fun refreshAuthToken() {
        cachedToken = preferencesManager.accessToken.first()
    }
}
