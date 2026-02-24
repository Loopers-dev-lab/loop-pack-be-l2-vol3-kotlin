package com.loopers.infrastructure.user

import com.loopers.domain.user.PasswordEncoder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BcryptPasswordEncoder : PasswordEncoder {
    private val bcrypt = BCryptPasswordEncoder()
    override fun encode(rawPassword: String): String = bcrypt.encode(rawPassword)
    override fun matches(rawPassword: String, encodedPassword: String): Boolean = bcrypt.matches(rawPassword, encodedPassword)
}
