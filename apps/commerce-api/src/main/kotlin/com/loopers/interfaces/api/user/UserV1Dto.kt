package com.loopers.interfaces.api.user

import com.loopers.application.user.ChangePasswordCriteria
import com.loopers.application.user.SignUpCriteria
import com.loopers.application.user.UserResult
import java.time.LocalDate

class UserV1Dto {

    data class SignUpRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val birthday: LocalDate,
        val email: String,
    ) {
        fun toCriteria(): SignUpCriteria {
            return SignUpCriteria(
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
            fun from(result: UserResult): UserResponse {
                return UserResponse(
                    id = result.id,
                    loginId = result.loginId,
                    name = result.name,
                    birthday = result.birthday,
                    email = result.email,
                )
            }
        }
    }

    data class ChangePasswordRequest(
        val newPassword: String,
    ) {
        fun toCriteria(): ChangePasswordCriteria {
            return ChangePasswordCriteria(
                newPassword = newPassword,
            )
        }
    }
}
