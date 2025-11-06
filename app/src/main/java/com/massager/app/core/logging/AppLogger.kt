package com.massager.app.core.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

private const val MAX_CUSTOM_KEY_LENGTH = 30

/**
 * Lightweight wrapper around Firebase Crashlytics to centralize logging behaviour.
 */
object AppLogger {

    fun info(tag: String, message: String, attributes: Map<String, Any?> = emptyMap()) {
        log(priority = Log.INFO, tag = tag, message = message, throwable = null, attributes = attributes)
    }

    fun warn(tag: String, message: String, attributes: Map<String, Any?> = emptyMap()) {
        log(priority = Log.WARN, tag = tag, message = message, throwable = null, attributes = attributes)
    }

    fun error(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        attributes: Map<String, Any?> = emptyMap()
    ) {
        log(priority = Log.ERROR, tag = tag, message = message, throwable = throwable, attributes = attributes)
    }

    fun recordException(tag: String, throwable: Throwable, attributes: Map<String, Any?> = emptyMap()) {
        log(priority = Log.ERROR, tag = tag, message = throwable.message ?: "Handled exception", throwable = throwable, attributes = attributes)
    }

    private fun log(
        priority: Int,
        tag: String,
        message: String,
        throwable: Throwable?,
        attributes: Map<String, Any?>,
    ) {
        Log.println(priority, tag, message)
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.log("$tag | $message")
        if (attributes.isNotEmpty()) {
            setCustomKeys(crashlytics, attributes)
        }
        throwable?.let(crashlytics::recordException)
    }

    private fun setCustomKeys(
        crashlytics: FirebaseCrashlytics,
        attributes: Map<String, Any?>,
    ) {
        attributes.forEach { (rawKey, value) ->
            val key = rawKey.sanitizeKey()
            when (value) {
                null -> crashlytics.setCustomKey(key, "null")
                is Boolean -> crashlytics.setCustomKey(key, value)
                is Int -> crashlytics.setCustomKey(key, value)
                is Long -> crashlytics.setCustomKey(key, value)
                is Float -> crashlytics.setCustomKey(key, value)
                is Double -> crashlytics.setCustomKey(key, value)
                else -> crashlytics.setCustomKey(key, value.toString().take(MAX_CUSTOM_VALUE_LENGTH))
            }
        }
    }

    private fun String.sanitizeKey(): String {
        val trimmed = trim().replace("[^A-Za-z0-9_]".toRegex(), "_")
        return trimmed.takeIf { it.isNotBlank() }?.take(MAX_CUSTOM_KEY_LENGTH) ?: "unknown_key"
    }

    private const val MAX_CUSTOM_VALUE_LENGTH = 1000
}
