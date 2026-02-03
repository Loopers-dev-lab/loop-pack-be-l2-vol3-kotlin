package com.loopers.application.user

data class RegisterUserCommand(
    val loginId: String,
    val password: String,
    val name: String,
    val birthDate: String,
    val email: String,
    val gender: String,
)
