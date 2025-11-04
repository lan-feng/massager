package com.massager.app.core.logging

private const val MASSAGER_LOG_PREFIX = "MassagerApp/"
private const val MAX_TAG_LENGTH = 23
private val NON_ALPHANUMERIC_REGEX = Regex("[^A-Za-z0-9_]")

fun logTag(component: String): String {
    val sanitized = component.replace(NON_ALPHANUMERIC_REGEX, "_")
    val maxComponentLength = (MAX_TAG_LENGTH - MASSAGER_LOG_PREFIX.length).coerceAtLeast(0)
    val truncatedComponent = sanitized.take(maxComponentLength)
    return (MASSAGER_LOG_PREFIX + truncatedComponent).take(MAX_TAG_LENGTH)
}
