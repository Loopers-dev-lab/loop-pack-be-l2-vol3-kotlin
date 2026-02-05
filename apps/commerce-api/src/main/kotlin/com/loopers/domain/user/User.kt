package com.loopers.domain.user

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.security.MessageDigest

@Entity
@Table(name = "users")
class User(
    loginId: String,
    password: String,
    name: String,
    birth: String,
    email: String,
) : BaseEntity() {
    var loginId: String = loginId
        protected set

    var password: String = encryptPassword(password)
        protected set

    var name: String = name
        protected set

    var birth: String = birth
        protected set

    var email: String = email
        protected set

    init {
        if (!validateLoginId(loginId)) throw CoreException(ErrorType.BAD_REQUEST, "invalid login id")

        if (!validatePassword(password, birth)) throw CoreException(ErrorType.BAD_REQUEST, "invalid password")

        if (!validateName(name)) throw CoreException(ErrorType.BAD_REQUEST, "invalid name")

        if (!validateBirth(birth)) throw CoreException(ErrorType.BAD_REQUEST, "invalid birth")

        if (!validateEmail(email)) throw CoreException(ErrorType.BAD_REQUEST, "invalid email")
    }

    companion object {
        private fun encryptPassword(password: String): String {
            return MessageDigest.getInstance("SHA-256")
                .digest(password.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }

        private fun validateLoginId(loginId: String): Boolean {
            val loginIdValidator = Regex("^[A-Za-z0-9]{6,16}$")

            return loginIdValidator.matches(loginId)
        }

        private fun validatePassword(password: String, birth: String): Boolean {
            val passwordValidator = Regex("^[A-Za-z0-9!@#$%^&*+_.-]{8,16}$")
            val birthString = birth.replace("-", "")

            return passwordValidator.matches(password) && !password.contains(birthString)
        }

        private fun validateName(name: String): Boolean {
            val nameValidator = Regex("^[A-Za-z가-힣0-9]{1,16}$")

            return nameValidator.matches(name)
        }

        private fun validateBirth(birth: String): Boolean {
            val birthValidator = Regex("^\\d{4}-\\d{2}-\\d{2}$")

            return birthValidator.matches(birth)
        }

        private fun validateEmail(email: String): Boolean {
            val emailValidator = Regex("^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*\\.[a-zA-Z]{2,3}$")

            return emailValidator.matches(email)
        }
    }
}
