package com.loopers.interfaces.api.member

import com.loopers.application.member.MemberInfo
import com.loopers.support.util.MaskingUtils
import com.loopers.support.util.PasswordValidator
import java.time.LocalDate

class MemberV1Dto {
    data class SignUpRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        init {
            PasswordValidator.validatePassword(password, birthDate, loginId)
        }
    }

    data class SignUpResponse(
        val id: Long,
        val loginId: String,
        val name: String,
        val email: String,
    ) {
        companion object {
            fun from(info: MemberInfo): SignUpResponse {
                return SignUpResponse(
                    id = info.id,
                    loginId = info.loginId,
                    name = info.name,
                    email = info.email,
                )
            }
        }
    }

    data class ChangePasswordRequest(
        val currentPassword: String,
        val newPassword: String,
    )

    data class MyInfoResponse(
        val loginId: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        companion object {
            fun from(info: MemberInfo): MyInfoResponse {
                return MyInfoResponse(
                    loginId = info.loginId,
                    name = MaskingUtils.maskName(info.name),
                    birthDate = info.birthDate,
                    email = MaskingUtils.maskEmail(info.email),
                )
            }
        }
    }
}
