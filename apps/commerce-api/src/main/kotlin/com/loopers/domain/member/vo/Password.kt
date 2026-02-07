package com.loopers.domain.member.vo

import com.loopers.domain.member.PasswordEncoder

data class Password private constructor(val value: String) {

    fun matches(rawPassword: String, encoder: PasswordEncoder): Boolean {
        return encoder.matches(rawPassword, value)
    }

    override fun toString(): String = "Password(****)"

    companion object {
        fun fromEncoded(encodedPassword: String): Password = Password(encodedPassword)
    }
}
