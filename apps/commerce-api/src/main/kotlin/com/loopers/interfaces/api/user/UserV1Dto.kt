package com.loopers.interfaces.api.user

class UserV1Dto {

    data class UserInfo(
        val loginId: String,
        val name: String,
        val birthDate: String,
        val email: String,
    )

    data class SignUpRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val birthDate: String,
        val email: String,
    )

    data class PasswordChangeRequest(
        val currentPassword: String,
        val newPassword: String,
    )
}
