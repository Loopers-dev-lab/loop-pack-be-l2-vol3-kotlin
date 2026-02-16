package com.loopers.domain.user.dto

data class SignUpCommand(
    val loginId: String,
    val password: String,
    val name: String,
    val birthDate: String,
    val email: String,
)
