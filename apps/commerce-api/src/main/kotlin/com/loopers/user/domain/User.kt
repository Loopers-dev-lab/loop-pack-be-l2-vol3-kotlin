package com.loopers.user.domain

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class User private constructor(
    val id: Long?,
    val loginId: String,
    val password: String,
    val name: String,
    val birthDate: LocalDate,
    val email: String,
) {
    val maskedName: String
        get() = name.dropLast(1) + "*"

    fun changePassword(newPassword: String): User {
        validatePassword(newPassword, birthDate)
        return User(
            id = id,
            loginId = loginId,
            password = newPassword,
            name = name,
            birthDate = birthDate,
            email = email,
        )
    }

    companion object {
        private val LOGIN_ID_PATTERN = Regex("^[a-zA-Z0-9]+$")
        private val PASSWORD_PATTERN = Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{}|;:',.<>?/]+$")
        private val NAME_PATTERN = Regex("^[가-힣]+$")
        private val BIRTH_DATE_COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd")

        fun retrieve(
            id: Long,
            loginId: String,
            password: String,
            name: String,
            birthDate: LocalDate,
            email: String,
        ): User {
            return User(
                id = id,
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )
        }

        fun register(
            loginId: String,
            password: String,
            name: String,
            birthDate: LocalDate,
            email: String,
        ): User {
            validateLoginId(loginId)
            validatePassword(password, birthDate)
            validateName(name)

            return User(
                id = null,
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )
        }

        private fun validateLoginId(loginId: String) {
            if (!LOGIN_ID_PATTERN.matches(loginId)) {
                throw CoreException(ErrorType.USER_INVALID_LOGIN_ID)
            }
        }

        fun validatePassword(password: String, birthDate: LocalDate) {
            if (!PASSWORD_PATTERN.matches(password)) {
                throw CoreException(ErrorType.USER_INVALID_PASSWORD)
            }

            val compactDate = birthDate.format(BIRTH_DATE_COMPACT)
            val dashedDate = birthDate.toString()
            if (password.contains(compactDate) || password.contains(dashedDate)) {
                throw CoreException(ErrorType.USER_INVALID_PASSWORD, "비밀번호에 생년월일을 포함할 수 없습니다.")
            }
        }

        private fun validateName(name: String) {
            if (!NAME_PATTERN.matches(name)) {
                throw CoreException(ErrorType.USER_INVALID_NAME)
            }
        }
    }
}
