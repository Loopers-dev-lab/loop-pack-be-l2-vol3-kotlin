package com.loopers.interfaces.api.member

import com.loopers.application.member.MemberInfo
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MemberV1Dto {
    data class SignUpRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        init {
            validatePassword(password, birthDate)
        }

        private fun validatePassword(password: String, birthDate: LocalDate) {
            if (password.length < 8 || password.length > 16) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8~16자여야 합니다.")
            }
            if (!password.matches(Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{}|;':\",./<>?]+$"))) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 영문 대소문자, 숫자, 특수문자만 허용됩니다.")
            }
            if (containsBirthDate(password, birthDate)) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다.")
            }
        }

        private fun containsBirthDate(password: String, birthDate: LocalDate): Boolean {
            val yyyymmdd = birthDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val yymmdd = birthDate.format(DateTimeFormatter.ofPattern("yyMMdd"))
            val mmdd = birthDate.format(DateTimeFormatter.ofPattern("MMdd"))

            return password.contains(yyyymmdd) ||
                password.contains(yymmdd) ||
                password.contains(mmdd)
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
}
