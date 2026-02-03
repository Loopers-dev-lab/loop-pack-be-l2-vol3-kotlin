package com.loopers.interfaces.api.user

import com.loopers.application.user.UserInfo
import com.loopers.domain.user.UserCommand
import java.time.LocalDate

class UserV1Dto {
    data class SignUpRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        fun toCommand(): UserCommand.SignUp {
            return UserCommand.SignUp(
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )
        }
    }

    data class UserResponse(
        val loginId: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        companion object {
            fun from(info: UserInfo): UserResponse {
                return UserResponse(
                    loginId = info.loginId,
                    name = info.name,
                    birthDate = info.birthDate,
                    email = info.email,
                )
            }
        }
    }
}
