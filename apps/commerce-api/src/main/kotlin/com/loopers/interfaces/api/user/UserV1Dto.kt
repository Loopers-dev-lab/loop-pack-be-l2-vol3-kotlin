package com.loopers.interfaces.api.user

import com.loopers.application.user.UserInfo

class UserV1Dto {
    data class RegisterUserRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val birth: String,
        val email: String,
    )

    data class UserResponse(
        val loginId: String,
        val name: String,
        val birth: String,
        val email: String,
    ) {
        companion object {
            fun from(user: UserInfo): UserResponse {
                return UserResponse(
                    loginId = user.loginId,
                    name = user.name,
                    birth = user.birth,
                    email = user.email,
                )
            }
        }
    }
}
