package com.loopers.infrastructure.member

import com.loopers.domain.member.PasswordEncoder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptPasswordEncoderAdapter : PasswordEncoder {

    private val delegate = BCryptPasswordEncoder()

    override fun encode(rawPassword: String): String {
        return delegate.encode(rawPassword)
    }

    override fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return delegate.matches(rawPassword, encodedPassword)
    }
}
