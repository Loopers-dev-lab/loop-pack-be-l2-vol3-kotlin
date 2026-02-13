package com.loopers.domain.user

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "users")
class User(
    loginId: String,
    password: String,
    name: String,
    birthDate: LocalDate,
    email: String,
) : BaseEntity() {

    @Column(name = "login_id", nullable = false, unique = true, length = 16)
    var loginId: String = loginId
        protected set

    @Column(name = "password", nullable = false)
    var password: String = password
        protected set

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Column(name = "birth_date", nullable = false)
    var birthDate: LocalDate = birthDate
        protected set

    @Column(name = "email", nullable = false)
    var email: String = email
        protected set

    init {
        LoginId(loginId)
        Name(name)
        Email(email)
        Password(password, birthDate)
        this.password = encodePassword(password)
    }

    fun getMaskedName(): String = Name(name).masked()

    fun changePassword(newPassword: String) {
        if (verifyPassword(newPassword)) {
            throw CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.")
        }
        Password(newPassword, birthDate)
        this.password = encodePassword(newPassword)
    }

    fun verifyPassword(rawPassword: String): Boolean {
        return this.password == encodePassword(rawPassword)
    }

    // TODO: 추후 security 추가 시 변경
    private fun encodePassword(rawPassword: String): String {
        val bytes = rawPassword.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
