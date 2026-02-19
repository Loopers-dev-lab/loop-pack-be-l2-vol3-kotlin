package com.loopers.application.auth

import com.loopers.domain.user.UserService
import com.loopers.domain.user.entity.User
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class AuthService(
    private val userService: UserService,
) {
    fun authenticate(loginId: String, loginPw: String): User {
        val user = userService.getUserInfo(loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")

        if (!user.verifyPassword(loginPw)) throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")

        return user
    }
}
