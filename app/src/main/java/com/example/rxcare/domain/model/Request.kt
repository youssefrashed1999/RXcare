package com.example.rxcare.domain.model

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    COMPLETED
}

data class Request(
    val id: String,
    val clientId: String,
    val clientName: String,
    val pharmacistId: String? = null,
    val pharmacistName: String? = null,
    val description: String,
    val status: RequestStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
