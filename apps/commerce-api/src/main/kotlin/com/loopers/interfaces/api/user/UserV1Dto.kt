package com.loopers.interfaces.api.user

import com.loopers.application.user.UserInfo
import com.loopers.domain.user.ChangePasswordCommand
import com.loopers.domain.user.SignUpCommand
import java.time.LocalDate

class UserV1Dto {

    data class SignUpRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val birthday: LocalDate,
        val email: String,
    ) {
        fun toCommand(): SignUpCommand {
            return SignUpCommand(
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

    data class ChangePasswordRequest(
        val newPassword: String,
    ) {
        fun toCommand(): ChangePasswordCommand {
            return ChangePasswordCommand(
                newPassword = newPassword,
            )
        }
    }
}
