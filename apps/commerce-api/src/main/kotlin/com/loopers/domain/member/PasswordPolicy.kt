package com.loopers.domain.member

import com.loopers.domain.member.vo.Password
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PasswordPolicy(private val encoder: PasswordEncoder) {

    private val validator = PasswordValidator()

    fun createPassword(rawPassword: String, birthDate: LocalDate): Password {
        validator.validate(rawPassword, birthDate)
        return Password.fromEncoded(encoder.encode(rawPassword))
    }

    fun matches(rawPassword: String, password: Password): Boolean {
        return password.matches(rawPassword, encoder)
    }
}
