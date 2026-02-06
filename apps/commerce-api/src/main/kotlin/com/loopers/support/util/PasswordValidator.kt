package com.loopers.support.util

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object PasswordValidator {

    private val ALLOWED_CHARS_REGEX = Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{}|;':\",./<>?]+$")
    private const val MIN_LENGTH = 8
    private const val MAX_LENGTH = 16

    fun validatePassword(
        password: String,
        birthDate: LocalDate,
        loginId: String? = null,
    ) {
        validateLength(password)
        validateAllowedCharacters(password)
        validateMinimumCharacterTypes(password)
        validateNoConsecutiveRepeatingChars(password)
        validateNoConsecutiveSequentialChars(password)
        validateNoBirthDate(password, birthDate)
        if (loginId != null) {
            validateNoLoginId(password, loginId)
        }
    }

    fun containsBirthDate(password: String, birthDate: LocalDate): Boolean {
        val yyyymmdd = birthDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val yymmdd = birthDate.format(DateTimeFormatter.ofPattern("yyMMdd"))
        val mmdd = birthDate.format(DateTimeFormatter.ofPattern("MMdd"))

        return password.contains(yyyymmdd) ||
            password.contains(yymmdd) ||
            password.contains(mmdd)
    }

    private fun validateLength(password: String) {
        if (password.length < MIN_LENGTH || password.length > MAX_LENGTH) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8~16자여야 합니다.")
        }
    }

    private fun validateAllowedCharacters(password: String) {
        if (!password.matches(ALLOWED_CHARS_REGEX)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 영문 대소문자, 숫자, 특수문자만 허용됩니다.")
        }
    }

    private fun validateMinimumCharacterTypes(password: String) {
        var typeCount = 0
        if (password.any { it.isLetter() }) typeCount++
        if (password.any { it.isDigit() }) typeCount++
        if (password.any { !it.isLetterOrDigit() }) typeCount++

        if (typeCount < 2) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 영문, 숫자, 특수문자 중 2종류 이상 조합해야 합니다.")
        }
    }

    private fun validateNoConsecutiveRepeatingChars(password: String) {
        for (i in 0..password.length - 3) {
            if (password[i] == password[i + 1] && password[i + 1] == password[i + 2]) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 연속 동일문자를 3개 이상 사용할 수 없습니다.")
            }
        }
    }

    private fun validateNoConsecutiveSequentialChars(password: String) {
        for (i in 0..password.length - 3) {
            val first = password[i].code
            val second = password[i + 1].code
            val third = password[i + 2].code

            if (second - first == 1 && third - second == 1) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 연속 순서문자를 3개 이상 사용할 수 없습니다.")
            }
            if (first - second == 1 && second - third == 1) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 연속 순서문자를 3개 이상 사용할 수 없습니다.")
            }
        }
    }

    private fun validateNoBirthDate(password: String, birthDate: LocalDate) {
        if (containsBirthDate(password, birthDate)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다.")
        }
    }

    private fun validateNoLoginId(password: String, loginId: String) {
        if (password.contains(loginId)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 로그인 ID를 포함할 수 없습니다.")
        }
    }
}
