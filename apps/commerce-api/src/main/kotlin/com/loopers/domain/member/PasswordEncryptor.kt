package com.loopers.domain.member

interface PasswordEncryptor {
    fun encode(rawPassword: String): String
    fun matches(rawPassword: String, encodedPassword: String): Boolean
}
