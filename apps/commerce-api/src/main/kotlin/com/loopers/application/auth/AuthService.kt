package com.loopers.application.auth

import com.loopers.domain.user.User
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class AuthService(
    private val userService: UserService,
) {
    fun authenticate(loginId: String, loginPw: String): User {
        val user = userService.getUserInfo(loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "사용자를 찾을 수 없습니다.")

        if (!user.verifyPassword(loginPw)) throw CoreException(ErrorType.UNAUTHORIZED, "로그인 정보가 일치하지 않습니다.")

        return user
    }
}
