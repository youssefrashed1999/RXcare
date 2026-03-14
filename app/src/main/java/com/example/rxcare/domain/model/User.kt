package com.example.rxcare.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val profileImageUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)
