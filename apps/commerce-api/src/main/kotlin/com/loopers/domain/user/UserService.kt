package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    fun signUp(loginId: String, password: String, name: String, email: String, birthday: LocalDate): User {
        val loginIdVo = LoginId.of(loginId)
        val emailVo = Email.of(email)

        if (userRepository.existsByLoginId(loginIdVo)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID 입니다.")
        }

        val encodedPassword = encodePassword(password, birthday)

        val user = User(
            loginId = loginIdVo,
            password = encodedPassword,
            name = name,
            email = emailVo,
            birthday = birthday,
        )
        return userRepository.save(user)
    }

    fun authenticate(loginId: String, password: String): User {
        val loginIdVo = LoginId.of(loginId)
        val user = userRepository.findByLoginId(loginIdVo)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "존재하지 않는 사용자입니다.")

        verifyPassword(password, user.password)

        return user
    }

    fun changePassword(userId: Long, currentPassword: String, newPassword: String) {
        val user = userRepository.findById(userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.")

        verifyPassword(currentPassword, user.password)

        if (currentPassword == newPassword) {
            throw CoreException(ErrorType.BAD_REQUEST, "새 비밀번호는 현재 비밀번호와 달라야 합니다.")
        }

        val encodedPassword = encodePassword(newPassword, user.birthday)
        user.changePassword(encodedPassword)
    }

    private fun encodePassword(rawPassword: String, birthday: LocalDate): String {
        val validatedPassword = Password.of(rawPassword, birthday)
        return passwordEncoder.encode(validatedPassword.value)
    }

    private fun verifyPassword(rawPassword: String, encodedPassword: String) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.")
        }
    }
}
