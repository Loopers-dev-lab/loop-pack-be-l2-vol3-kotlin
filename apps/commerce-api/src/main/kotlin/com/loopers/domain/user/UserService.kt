package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun register(user: UserModel): UserModel {
        userRepository.findByUsername(user.username)?.let {
            throw CoreException(errorType = ErrorType.CONFLICT, customMessage = "이미 존재하는 아이디입니다.")
        }
        user.applyEncodedPassword(passwordEncoder.encode(user.password))
        return userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun authenticate(username: String, password: String) {
        val user = getUser(username)
        if (!passwordEncoder.matches(password, user.password)) {
            throw CoreException(errorType = ErrorType.UNAUTHORIZED, customMessage = "비밀번호가 일치하지 않습니다.")
        }
    }

    @Transactional(readOnly = true)
    fun getUser(username: String): UserModel {
        return userRepository.findByUsername(username)
            ?: throw CoreException(errorType = ErrorType.NOT_FOUND, customMessage = "사용자를 찾을 수 없습니다.")
    }

    @Transactional
    fun updatePassword(command: UpdatePasswordCommand): UserModel {
        val user = getUser(command.username)
        if (passwordEncoder.matches(command.newPassword, user.password)) {
            throw CoreException(errorType = ErrorType.BAD_REQUEST, customMessage = "비밀번호는 이전과 동일할 수 없습니다.")
        }
        user.updatePassword(command.newPassword)
        user.applyEncodedPassword(passwordEncoder.encode(command.newPassword))
        return userRepository.save(user)
    }
}

data class UpdatePasswordCommand(
    val username: String,
    val currentPassword: String,
    val newPassword: String,
)
