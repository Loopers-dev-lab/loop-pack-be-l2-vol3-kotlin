package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class User private constructor(
    val id: Long?,
    val loginId: LoginId,
    val password: EncodedPassword,
    val name: UserName,
    val birthDate: LocalDate,
    val email: Email,
) {
    val maskedName: String
        get() = name.masked

    fun changePassword(currentPassword: String, newPassword: String, passwordHasher: UserPasswordHasher): User {
        val current = RawPassword(currentPassword)
        val new = RawPassword(newPassword)

        if (!passwordHasher.matches(current, password)) {
            throw CoreException(ErrorType.USER_INVALID_PASSWORD, "현재 비밀번호가 일치하지 않습니다.")
        }
        if (passwordHasher.matches(new, password)) {
            throw CoreException(ErrorType.USER_INVALID_PASSWORD, "새 비밀번호는 현재 비밀번호와 달라야 합니다.")
        }
        validatePasswordNotContainsBirthDate(new, birthDate)

        return User(
            id = id,
            loginId = loginId,
            password = passwordHasher.encode(new),
            name = name,
            birthDate = birthDate,
            email = email,
        )
    }

    companion object {
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
                loginId = LoginId(loginId),
                password = EncodedPassword(password),
                name = UserName(name),
                birthDate = birthDate,
                email = Email(email),
            )
        }

        fun register(
            loginId: String,
            rawPassword: String,
            name: String,
            birthDate: LocalDate,
            email: String,
            passwordHasher: UserPasswordHasher,
        ): User {
            val loginIdVo = LoginId(loginId)
            val rawPasswordVo = RawPassword(rawPassword)
            val nameVo = UserName(name)
            val emailVo = Email(email)

            validatePasswordNotContainsBirthDate(rawPasswordVo, birthDate)

            return User(
                id = null,
                loginId = loginIdVo,
                password = passwordHasher.encode(rawPasswordVo),
                name = nameVo,
                birthDate = birthDate,
                email = emailVo,
            )
        }

        private fun validatePasswordNotContainsBirthDate(password: RawPassword, birthDate: LocalDate) {
            val compactDate = birthDate.format(BIRTH_DATE_COMPACT)
            val dashedDate = birthDate.toString()
            if (password.value.contains(compactDate) || password.value.contains(dashedDate)) {
                throw CoreException(ErrorType.USER_INVALID_PASSWORD, "비밀번호에 생년월일을 포함할 수 없습니다.")
            }
        }
    }
}
