package com.loopers.domain.user

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component


@Component
class UserPasswordEncoder: PasswordEncoder {

    private val encoder = BCryptPasswordEncoder()

    override fun encode(rawPassword: CharSequence?): String {
        return encoder.encode(rawPassword)
    }

    override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean {
        return encoder.matches(rawPassword, encodedPassword)
    }
}
