package com.loopers.support.util

object MaskingUtils {

    fun maskName(name: String): String {
        if (name.length <= 1) return name
        if (name.length == 2) return "${name.first()}*"
        return "${name.first()}${"*".repeat(name.length - 2)}${name.last()}"
    }

    fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex < 0) return email

        val localPart = email.substring(0, atIndex)
        val domain = email.substring(atIndex)

        val visiblePrefix = if (localPart.length >= 2) localPart.substring(0, 2) else localPart
        return "$visiblePrefix***$domain"
    }
}
