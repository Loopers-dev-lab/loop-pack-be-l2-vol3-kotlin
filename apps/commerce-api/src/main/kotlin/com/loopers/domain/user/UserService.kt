package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class UserService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun registerUser(
        loginId: String,
        password: String,
        name: String,
        birth: String,
        email: String,
    ): User {
        if (userRepository.existsByLoginId(loginId)) throw CoreException(ErrorType.CONFLICT, "User already exists")

        return userRepository.save(User(loginId, password, name, birth, email))
    }

    fun getUserByLoginIdAndPassword(loginId: String, password: String): User? {
        val user = userRepository.findByLoginId(loginId)

        if (user == null || !user.matchPassword(password)) return null

        return user
    }

    @Transactional
    fun chagePassword(loginId: String, oldPassword: String, newPassword: String): User? {
        val user = userRepository.findByLoginId(loginId)

        if (user == null || !user.matchPassword(oldPassword)) return null

        user.changePassword(newPassword)

        return user
    }
}
