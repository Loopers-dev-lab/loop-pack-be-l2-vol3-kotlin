package com.loopers.infrastructure.user

import com.loopers.domain.user.EncodedPassword
import com.loopers.domain.user.RawPassword
import com.loopers.domain.user.UserPasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class BcryptUserPasswordHasher(
    private val passwordEncoder: PasswordEncoder,
) : UserPasswordHasher {
    override fun encode(rawPassword: RawPassword): EncodedPassword =
        EncodedPassword(passwordEncoder.encode(rawPassword.value))

    override fun matches(rawPassword: RawPassword, encodedPassword: EncodedPassword): Boolean =
        passwordEncoder.matches(rawPassword.value, encodedPassword.value)
}
