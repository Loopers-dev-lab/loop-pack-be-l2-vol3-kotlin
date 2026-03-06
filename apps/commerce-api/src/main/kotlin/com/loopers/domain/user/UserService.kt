package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserService(
    private val userRepository: UserRepository,
) {

    @Transactional
    fun createUser(
        loginId: String,
        encryptedPassword: String,
        name: String,
        birthDate: String,
        email: String,
    ): UserModel {
        if (userRepository.existsByLoginId(LoginId(loginId))) {
            throw CoreException(
                errorType = ErrorType.CONFLICT,
                customMessage = "이미 존재하는 로그인 ID입니다.",
            )
        }

        val user = UserModel(
            loginId = LoginId(loginId),
            encryptedPassword = encryptedPassword,
            name = Name(name),
            birthDate = BirthDate(birthDate),
            email = Email(email),
        )

        return try {
            userRepository.save(user)
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.")
        }
    }

    @Transactional(readOnly = true)
    fun getUserByLoginId(loginId: String): UserModel {
        return userRepository.findByLoginId(LoginId(loginId))
            ?: throw CoreException(
                errorType = ErrorType.NOT_FOUND,
                customMessage = "[loginId = $loginId] User를 찾을 수 없습니다.",
            )
    }

    @Transactional
    fun updatePassword(loginId: String, encryptedPassword: String) {
        val user = getUserByLoginId(loginId)
        user.updatePassword(encryptedPassword)
    }
}
