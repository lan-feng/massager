package com.massager.app.domain.model

data class UserProfile(
    val id: Long,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val cacheSize: String?
)
