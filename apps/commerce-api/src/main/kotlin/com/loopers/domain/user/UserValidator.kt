package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class UserValidator {

    fun validatePasswordNotContainsBirthDate(password: Password, birthDate: BirthDate) {
        val fullBirthDate = birthDate.value.replace("-", "") // "20020101"
        val shortBirthDate = fullBirthDate.substring(2) // "020101"

        if (password.value.contains(fullBirthDate) || password.value.contains(shortBirthDate)) {
            throw CoreException(
                errorType = ErrorType.BAD_REQUEST,
                customMessage = "비밀번호에 생년월일(8자리 또는 6자리)을 포함할 수 없습니다.",
            )
        }
    }
}
