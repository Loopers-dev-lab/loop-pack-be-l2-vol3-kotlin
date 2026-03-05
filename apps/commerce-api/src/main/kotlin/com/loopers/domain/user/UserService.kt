package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun register(command: RegisterCommand): UserModel {
        val username = Username.of(command.username)
        val email = Email.of(command.email)
        val password = Password.of(command.password, command.birthDate)
        val user = UserModel(
            username = username,
            password = password,
            name = command.name,
            email = email,
            birthDate = command.birthDate,
        )
        user.applyEncodedPassword(passwordEncoder.encode(password.value))
        try {
            val saved = userRepository.save(user)
            userRepository.flush()
            return saved
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(errorType = ErrorType.CONFLICT, customMessage = "이미 존재하는 아이디입니다.")
        }
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

    @Transactional(readOnly = true)
    fun getUserById(id: Long): UserModel {
        return userRepository.find(id)
            ?: throw CoreException(errorType = ErrorType.NOT_FOUND, customMessage = "사용자를 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun findUserById(id: Long): UserModel? {
        return userRepository.find(id)
    }

    @Transactional
    fun updatePassword(command: UpdatePasswordCommand): UserModel {
        val user = getUser(command.username)
        if (passwordEncoder.matches(command.newPassword, user.password)) {
            throw CoreException(errorType = ErrorType.BAD_REQUEST, customMessage = "비밀번호는 이전과 동일할 수 없습니다.")
        }
        val newPassword = Password.of(command.newPassword, user.birthDate)
        user.applyEncodedPassword(passwordEncoder.encode(newPassword.value))
        return userRepository.save(user)
    }
}

data class RegisterCommand(
    val username: String,
    val password: String,
    val name: String,
    val email: String,
    val birthDate: ZonedDateTime,
)

data class UpdatePasswordCommand(
    val username: String,
    val currentPassword: String,
    val newPassword: String,
)
