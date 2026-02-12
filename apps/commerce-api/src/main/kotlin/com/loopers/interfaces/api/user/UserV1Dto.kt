package com.loopers.interfaces.api.user

import com.loopers.application.user.UserInfo

class UserV1Dto {
    data class UserRegisterRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val birthDate: String,
        val email: String,
    )

    data class UserRegisterResponse(
        val id: Long,
        val loginId: String,
        val name: String,
        val birthDate: String,
        val email: String,
    ) {
        companion object {
            fun from(info: UserInfo): UserRegisterResponse {
                return UserRegisterResponse(
                    id = info.id,
                    loginId = info.loginId,
                    name = info.name,
                    birthDate = info.birthDate,
                    email = info.email,
                )
            }
        }
    }

    data class UserInfoResponse(
        val id: Long,
        val loginId: String,
        val name: String,
        val birthDate: String,
        val email: String,
    ) {
        companion object {
            fun from(info: UserInfo): UserInfoResponse {
                return UserInfoResponse(
                    id = info.id,
                    loginId = info.loginId,
                    name = info.name,
                    birthDate = info.birthDate,
                    email = info.email,
                )
            }
        }
    }

    data class ChangePasswordRequest(
        val newPassword: String,
    )
}
