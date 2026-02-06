package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object PasswordValidator {

    fun validate(password: String, birthday: LocalDate) {
        if (password.length !in 8..16) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8~16자여야 합니다.")
        }
        if (!password.matches(Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{};':\",./<>?]+$"))) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 영문 대소문자, 숫자, 특수문자만 가능합니다.")
        }

        val birthdayStr = birthday.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        if (password.contains(birthdayStr)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다.")
        }
    }
}
