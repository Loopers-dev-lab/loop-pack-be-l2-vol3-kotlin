package com.loopers.interfaces.api.user

import com.loopers.application.user.UserInfo
import java.time.ZonedDateTime

class UserV1Dto {
    data class RegisterRequest(
        val username: String,
        val password: String,
        val name: String,
        val email: String,
        val birthDate: ZonedDateTime,
    )

    data class UpdatePasswordRequest(
        val currentPassword: String,
        val newPassword: String,
    )

    data class UserResponse(
        val username: String,
        val name: String,
        val email: String,
        val birthDate: ZonedDateTime,
    ) {
        companion object {
            fun from(info: UserInfo): UserResponse {
                return UserResponse(
                    username = info.username,
                    name = maskName(info.name),
                    email = info.email,
                    birthDate = info.birthDate,
                )
            }

            private fun maskName(name: String): String {
                return if (name.length > 1) {
                    name.dropLast(1) + "*"
                } else {
                    "*"
                }
            }
        }
    }
}
