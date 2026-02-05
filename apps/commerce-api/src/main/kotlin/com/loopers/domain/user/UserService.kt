package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserService(
    private val userRepository: UserRepository,
    private val userValidator: UserValidator,
    private val passwordEncryptor: PasswordEncryptor,
) {

    @Transactional
    fun createUser(
        loginId: LoginId,
        rawPassword: Password,
        name: Name,
        birthDate: BirthDate,
        email: Email,
    ): UserModel {
        // 0. 중복 검사
        if (userRepository.existsByLoginId(loginId)) {
            throw CoreException(
                errorType = ErrorType.CONFLICT,
                customMessage = "이미 존재하는 로그인 ID입니다.",
            )
        }

        // 1. 비즈니스 검증
        userValidator.validatePasswordNotContainsBirthDate(rawPassword, birthDate)

        // 2. 비밀번호 암호화
        val encryptedPassword = passwordEncryptor.encrypt(rawPassword.value)

        // 3. 엔티티 생성 (암호화된 비밀번호 사용)
        val user = UserModel(
            loginId = loginId,
            password = encryptedPassword,
            name = name,
            birthDate = birthDate,
            email = email,
        )

        // 4. 저장
        return userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun getUserByLoginId(loginId: LoginId): UserModel {
        return userRepository.findByLoginId(loginId)
            ?: throw CoreException(
                errorType = ErrorType.NOT_FOUND,
                customMessage = "[loginId = ${loginId.value}] User를 찾을 수 없습니다.",
            )
    }

    @Transactional
    fun updatePassword(loginId: LoginId, newRawPassword: Password, birthDate: BirthDate) {
        val user = getUserByLoginId(loginId)

        // 새 비밀번호 검증
        userValidator.validatePasswordNotContainsBirthDate(newRawPassword, birthDate)

        // 암호화 및 업데이트
        val encryptedPassword = passwordEncryptor.encrypt(newRawPassword.value)
        user.updatePassword(encryptedPassword)
    }
}
