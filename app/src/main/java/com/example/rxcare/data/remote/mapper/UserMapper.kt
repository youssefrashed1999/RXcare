package com.example.rxcare.data.remote.mapper

import com.example.rxcare.data.remote.dto.AuthResponse
import com.example.rxcare.domain.model.User
import com.example.rxcare.domain.model.UserRole

object UserMapper {
    
    fun mapToDomain(authResponse: AuthResponse): User {
        return User(
            id = authResponse.userId,
            name = authResponse.name,
            email = "", // Not available in auth response
            role = when (authResponse.role.uppercase()) {
                "PHARMACIST" -> UserRole.PHARMACIST
                "PATIENT" -> UserRole.CLIENT
                else -> UserRole.CLIENT
            },
            profileImageUrl = null,
            isOnline = false,
            lastSeen = System.currentTimeMillis()
        )
    }
}
