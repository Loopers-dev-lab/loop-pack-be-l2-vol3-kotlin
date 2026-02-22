package com.loopers.application.user

import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.Name
import com.loopers.domain.user.Password
import com.loopers.domain.user.PasswordEncryptor
import com.loopers.domain.user.UserModel
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncryptor: PasswordEncryptor,
) {

    @Transactional
    fun createUser(
        loginId: String,
        rawPassword: String,
        name: String,
        birthDate: String,
        email: String,
    ): UserModel {
        // 0. 중복 검사
        if (userRepository.existsByLoginId(LoginId(loginId))) {
            throw CoreException(
                errorType = ErrorType.CONFLICT,
                customMessage = "이미 존재하는 로그인 ID입니다.",
            )
        }

        // 1. 패스워드 검증 (생년월일 포함 X)
        Password(rawPassword).validateNotContainsBirthDate(BirthDate(birthDate))

        // 2. 패스워드 암호화
        val encryptedPassword = passwordEncryptor.encrypt(rawPassword)

        // 3. 엔티티 생성 (암호화된 패스워드 사용)
        val user = UserModel(
            loginId = LoginId(loginId),
            encryptedPassword = encryptedPassword,
            name = Name(name),
            birthDate = BirthDate(birthDate),
            email = Email(email),
        )

        // 4. 저장
        return userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun getUserByLoginId(loginId: String): UserModel {
        return userRepository.findByLoginId(LoginId(loginId))
            ?: throw CoreException(
                errorType = ErrorType.NOT_FOUND,
                customMessage = "[loginId = ${loginId}] User를 찾을 수 없습니다.",
            )
    }

    @Transactional
    fun updatePassword(loginId: String, newRawPassword: String, birthDate: String) {
        val user = getUserByLoginId(loginId)

        // 새 패스워드 검증
        Password(newRawPassword).validateNotContainsBirthDate(BirthDate(birthDate))

        // 암호화 및 업데이트
        val encryptedPassword = passwordEncryptor.encrypt(newRawPassword)
        user.updatePassword(encryptedPassword)
    }
}
