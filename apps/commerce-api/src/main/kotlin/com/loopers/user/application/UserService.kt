package com.loopers.user.application

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.user.application.model.UserChangePasswordCommand
import com.loopers.user.application.model.UserSignUpCommand
import com.loopers.user.domain.User
import com.loopers.user.domain.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun signUp(command: UserSignUpCommand): UserInfo {
        if (userRepository.existsByLoginId(command.loginId)) {
            throw CoreException(ErrorType.USER_DUPLICATE_LOGIN_ID)
        }

        val encodedPassword = passwordEncoder.encode(command.password)

        val user = User.register(
            loginId = command.loginId,
            password = encodedPassword,
            name = command.name,
            birthDate = command.birthDate,
            email = command.email,
        )

        val savedUser = userRepository.save(user)
        return UserInfo(loginId = savedUser.loginId)
    }

    @Transactional
    fun changePassword(loginId: String, headerPassword: String, command: UserChangePasswordCommand) {
        val user = userRepository.findByLoginId(loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED)

        if (!passwordEncoder.matches(headerPassword, user.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED)
        }

        if (!passwordEncoder.matches(command.currentPassword, user.password)) {
            throw CoreException(ErrorType.USER_INVALID_PASSWORD, "현재 비밀번호가 일치하지 않습니다.")
        }

        if (command.currentPassword == command.newPassword) {
            throw CoreException(ErrorType.USER_INVALID_PASSWORD, "새 비밀번호는 현재 비밀번호와 달라야 합니다.")
        }

        User.validatePassword(command.newPassword, user.birthDate)

        val encodedNewPassword = passwordEncoder.encode(command.newPassword)
        val updatedUser = User.retrieve(
            id = user.id!!,
            loginId = user.loginId,
            password = encodedNewPassword,
            name = user.name,
            birthDate = user.birthDate,
            email = user.email,
        )
        userRepository.save(updatedUser)
    }

    @Transactional(readOnly = true)
    fun getMe(loginId: String, password: String): UserInfo {
        val user = userRepository.findByLoginId(loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED)

        if (!passwordEncoder.matches(password, user.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED)
        }

        return UserInfo(
            loginId = user.loginId,
            name = user.maskedName,
            birthDate = user.birthDate,
            email = user.email,
        )
    }
}
