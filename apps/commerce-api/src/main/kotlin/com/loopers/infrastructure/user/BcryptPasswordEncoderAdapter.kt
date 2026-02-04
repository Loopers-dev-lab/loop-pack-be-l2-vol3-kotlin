package com.loopers.infrastructure.user

import com.loopers.domain.user.PasswordEncoder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BcryptPasswordEncoderAdapter : PasswordEncoder {

    private val encoder = BCryptPasswordEncoder()

    /**
     * Encode a plain-text password using BCrypt.
     *
     * @param rawPassword The plain-text password to hash.
     * @return The BCrypt-hashed password.
    override fun encode(rawPassword: String): String {
        return encoder.encode(rawPassword)
    }

    /**
     * Verifies that a raw password corresponds to a previously encoded BCrypt password.
     *
     * @param rawPassword the plaintext password to verify.
     * @param encodedPassword the stored BCrypt-encoded password to compare against.
     * @return `true` if `rawPassword` matches `encodedPassword`, `false` otherwise.
     */
    override fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return encoder.matches(rawPassword, encodedPassword)
    }
}