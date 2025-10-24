package com.massager.app.domain.model

data class User(
    val id: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String? = null,
    val appId: String? = null
)
