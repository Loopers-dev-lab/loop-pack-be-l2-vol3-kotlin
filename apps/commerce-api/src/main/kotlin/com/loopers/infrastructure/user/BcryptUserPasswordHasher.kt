package com.loopers.infrastructure.user

import com.loopers.domain.user.UserPasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class BcryptUserPasswordHasher(
    private val passwordEncoder: PasswordEncoder,
) : UserPasswordHasher {
    override fun encode(rawPassword: String): String = passwordEncoder.encode(rawPassword)

    override fun matches(rawPassword: String, encodedPassword: String): Boolean =
        passwordEncoder.matches(rawPassword, encodedPassword)
}
