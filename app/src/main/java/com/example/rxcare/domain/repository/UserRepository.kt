package com.example.rxcare.domain.repository

import com.example.rxcare.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun insertUser(user: User)
    suspend fun updateUser(user: User)
    suspend fun deleteUser(userId: String)
    suspend fun getUserById(userId: String): User?
    fun getAllUsers(): Flow<List<User>>
    suspend fun searchUsers(query: String): List<User>
    suspend fun getUsersByRole(role: com.example.rxcare.domain.model.UserRole): List<User>
}
