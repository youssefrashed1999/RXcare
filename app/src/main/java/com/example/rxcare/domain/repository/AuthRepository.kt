package com.example.rxcare.domain.repository

import com.example.rxcare.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun register(email: String, password: String, name: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun registerPharmacist(email: String, password: String, name: String): Result<Unit>
    suspend fun logout()
    fun isLoggedIn(): Flow<Boolean>
    fun getCurrentUser(): Flow<User?>
}
