package com.massager.app.domain.model

// 文件说明：描述用户资料的领域模型，用于展示与更新。
data class UserProfile(
    val id: Long,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val cacheSize: String?,
    val firebaseUid: String? = null,
    val appleUserId: String? = null,
    val facebookUid: String? = null,
    val thirdPartyProfiles: Map<String, ThirdPartyProfile> = emptyMap(),
    val hasPassword: Boolean = false
)

data class ThirdPartyProfile(
    val name: String? = null,
    val email: String? = null
)
