package com.massager.app.domain.model

// 文件说明：封装用户身份的基础数据模型。
data class User(
    val id: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String? = null,
    val appId: String? = null
)
