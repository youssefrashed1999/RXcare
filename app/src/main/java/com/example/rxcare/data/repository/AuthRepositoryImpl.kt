package com.example.rxcare.data.repository

import com.example.rxcare.data.remote.mapper.UserMapper
import com.example.rxcare.data.repository.AuthRepository as ApiAuthRepository
import com.example.rxcare.domain.model.User
import com.example.rxcare.domain.repository.AuthRepository as DomainAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl(
    private val apiAuthRepository: ApiAuthRepository
) : DomainAuthRepository {
    
    override suspend fun register(email: String, password: String, name: String): Result<User> {
        return apiAuthRepository.register(email, password, name).map { authResponse ->
            UserMapper.mapToDomain(authResponse)
        }
    }
    
    override suspend fun login(email: String, password: String): Result<User> {
        return apiAuthRepository.login(email, password).map { authResponse ->
            UserMapper.mapToDomain(authResponse)
        }
    }
    
    override suspend fun registerPharmacist(email: String, password: String, name: String): Result<Unit> {
        return apiAuthRepository.registerPharmacist(email, password, name)
    }
    
    override suspend fun logout() {
        apiAuthRepository.logout()
    }
    
    override fun isLoggedIn(): Flow<Boolean> {
        return apiAuthRepository.isLoggedIn()
    }
    
    override fun getCurrentUser(): Flow<User?> {
        return apiAuthRepository.getCurrentUser().map { authResponse ->
            authResponse?.let { UserMapper.mapToDomain(it) }
        }
    }
}
