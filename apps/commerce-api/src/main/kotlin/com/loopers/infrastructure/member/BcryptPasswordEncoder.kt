package com.loopers.infrastructure.member

import com.loopers.domain.member.PasswordEncoder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BcryptPasswordEncoder : PasswordEncoder {

    private val bcrypt = BCryptPasswordEncoder()

    override fun encode(rawPassword: String): String {
        return bcrypt.encode(rawPassword)
    }

    override fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return bcrypt.matches(rawPassword, encodedPassword)
    }
}
