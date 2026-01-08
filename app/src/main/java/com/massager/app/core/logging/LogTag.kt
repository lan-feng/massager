package com.massager.app.core.logging

// 文件说明：生成统一前缀且符合长度限制的日志标签。
private const val APP_LOG_PREFIX = "TheraLink/"
private const val MAX_TAG_LENGTH = 23
private val NON_ALPHANUMERIC_REGEX = Regex("[^A-Za-z0-9_]")

fun logTag(component: String): String {
    val sanitized = component.replace(NON_ALPHANUMERIC_REGEX, "_")
    val maxComponentLength = (MAX_TAG_LENGTH - APP_LOG_PREFIX.length).coerceAtLeast(0)
    val truncatedComponent = sanitized.take(maxComponentLength)
    return (APP_LOG_PREFIX + truncatedComponent).take(MAX_TAG_LENGTH)
}
