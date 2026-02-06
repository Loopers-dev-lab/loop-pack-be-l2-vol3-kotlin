package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional(readOnly = true)
    fun authenticate(loginId: String, loginPw: String): User {
        val user = userRepository.findByLoginId(loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")

        if (!passwordEncoder.matches(loginPw, user.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")
        }

        return user
    }

    @Transactional
    fun signUp(command: SignUpCommand): User {
        // 1. loginId 중복 체크
        if (userRepository.findByLoginId(command.loginId) != null) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.")
        }

        // 2. 비밀번호 규칙 검증 (평문 상태에서)
        PasswordValidator.validate(command.password, command.birthday)

        // 3. 비밀번호 암호화
        val encodedPassword = passwordEncoder.encode(command.password)

        // 4. 유저 생성 + 저장
        val user = User(
            loginId = command.loginId,
            password = encodedPassword,
            name = command.name,
            birthday = command.birthday,
            email = command.email,
        )
        return userRepository.save(user)
    }

    @Transactional
    fun changePassword(user: User, command: ChangePasswordCommand) {
        // 1. 새 비밀번호 규칙 검증
        PasswordValidator.validate(command.newPassword, user.birthday)

        // 2. 비밀번호 변경
        val encodedPassword = passwordEncoder.encode(command.newPassword)
        user.changePassword(encodedPassword)
    }

    data class SignUpCommand(
        val loginId: String,
        val password: String,
        val name: String,
        val birthday: LocalDate,
        val email: String,
    )

    data class ChangePasswordCommand(
        val newPassword: String,
    )
}
