package com.loopers.domain.member

class NoOpPasswordEncoder : PasswordEncoder {
    override fun encode(rawPassword: String): String = rawPassword
    override fun matches(rawPassword: String, encodedPassword: String): Boolean = rawPassword == encodedPassword
}
