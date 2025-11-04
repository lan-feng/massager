package com.massager.app.core.logging

private const val MASSAGER_LOG_PREFIX = "MassagerApp/"

fun logTag(component: String): String = MASSAGER_LOG_PREFIX + component

