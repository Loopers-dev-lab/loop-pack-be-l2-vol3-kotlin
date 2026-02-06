package com.loopers.domain.user

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class PasswordEncryptor {

    private val encoder = BCryptPasswordEncoder()

    fun encrypt(rawPassword: String): String {
        return encoder.encode(rawPassword)
    }

    fun matches(rawPassword: String, encryptedPassword: String): Boolean {
        return encoder.matches(rawPassword, encryptedPassword)
    }
}
