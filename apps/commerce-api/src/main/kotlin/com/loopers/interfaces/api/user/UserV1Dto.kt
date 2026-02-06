package com.loopers.interfaces.api.user

import com.loopers.application.user.UserInfo
import com.loopers.domain.user.UserService
import java.time.LocalDate

class UserV1Dto {

    data class SignUpRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val birthday: LocalDate,
        val email: String,
    ) {
        fun toCommand(): UserService.SignUpCommand {
            return UserService.SignUpCommand(
                loginId = loginId,
                password = password,
                name = name,
                birthday = birthday,
                email = email,
            )
        }
    }

    data class UserResponse(
        val id: Long,
        val loginId: String,
        val name: String,
        val birthday: LocalDate,
        val email: String,
    ) {
        companion object {
            fun from(info: UserInfo): UserResponse {
                return UserResponse(
                    id = info.id,
                    loginId = info.loginId,
                    name = info.name,
                    birthday = info.birthday,
                    email = info.email,
                )
            }
        }
    }
}
