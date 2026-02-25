package com.loopers.domain.user.model

import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZonedDateTime

class User(
    val id: Long = 0,
    val loginId: LoginId,
    password: String,
    val name: Name,
    val birthDate: LocalDate,
    val email: Email,
    deletedAt: ZonedDateTime? = null,
) {

    var password: String = password
        private set

    var deletedAt: ZonedDateTime? = deletedAt
        private set

    fun isDeleted(): Boolean = deletedAt != null

    fun getMaskedName(): String = name.masked()

    fun changePassword(currentPassword: String, newPassword: String) {
        if (!verifyPassword(currentPassword)) {
            throw CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다.")
        }
        if (verifyPassword(newPassword)) {
            throw CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.")
        }
        Password(newPassword, birthDate)
        this.password = encodePassword(newPassword)
    }

    fun verifyPassword(rawPassword: String): Boolean {
        return this.password == encodePassword(rawPassword)
    }

    companion object {
        fun create(
            loginId: String,
            password: String,
            name: String,
            birthDate: LocalDate,
            email: String,
        ): User {
            Password(password, birthDate)
            return User(
                loginId = LoginId(loginId),
                password = encodePassword(password),
                name = Name(name),
                birthDate = birthDate,
                email = Email(email),
            )
        }

        fun encodePassword(rawPassword: String): String {
            val bytes = rawPassword.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }
    }
}
