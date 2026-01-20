package com.massager.app.core.avatar

// Default avatar selection shared across modules.
const val DEFAULT_AVATAR_NAME = "baseballCap"

/**
 * For Google-linked accounts, ignore remote avatar URLs (e.g. Google profile photos)
 * and fall back to the local default name. Non-Google accounts return the original value.
 */
fun sanitizeAvatarForGoogle(avatarUrl: String?, hasGoogleAccount: Boolean): String? {
    if (!hasGoogleAccount) return avatarUrl
    val trimmed = avatarUrl?.trim()
    if (trimmed.isNullOrBlank()) return DEFAULT_AVATAR_NAME
    return if (trimmed.startsWith("http", ignoreCase = true)) DEFAULT_AVATAR_NAME else trimmed
}
