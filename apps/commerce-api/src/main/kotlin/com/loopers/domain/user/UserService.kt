package com.loopers.domain.user

import com.loopers.domain.user.dto.UserInfo
import com.loopers.domain.user.dto.SignUpCommand
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun signUp(command: SignUpCommand) {
        if (userRepository.existsByLoginId(command.loginId)) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 로그인ID 입니다.")
        }

        validateNewPassword(command.password, command.birthDate)
        val encodedPassword = passwordEncoder.encode(command.password)
        val user = User.create(
            loginId = command.loginId,
            password = encodedPassword,
            name = command.name,
            birthDate = command.birthDate,
            email = command.email,
        )

        userRepository.save(user)
    }

    fun findUserInfo(id: Long): UserInfo {
        val findUser = userRepository.findUserById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "사용자 정보가 없습니다")
        return UserInfo.from(findUser)
    }

    @Transactional
    fun changePassword(id: Long, currentPassword: String, newPassword: String) {
        val findUser = userRepository.findUserById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "사용자 정보가 없습니다")

        if (!passwordEncoder.matches(currentPassword, findUser.password)) {
            throw CoreException(ErrorType.BAD_REQUEST, "기존 비밀번호가 일치하지 않습니다")
        }

        if (passwordEncoder.matches(newPassword, findUser.password)) {
            throw CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다")
        }

        validateNewPassword(newPassword, findUser.birthDate)
        findUser.changePassword(passwordEncoder.encode(newPassword))
    }

    private fun validateNewPassword(password: String, birthDate: String) {
        if (password.length !in 8..16) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8자 이상 16자 이하여야 합니다")
        }

        if (!password.matches(Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$"))) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 영문 대/소문자, 숫자, 특수문자만 사용 가능합니다")
        }

        if (password.contains(birthDate)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다")
        }
    }
}
