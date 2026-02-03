package com.loopers.application.auth

import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class AuthService(
    private val userService: UserService
) {
    fun authenticate(loginId: String, loginPw: String) {
        val user = userService.getUserInfo(loginId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.")

        require(user.verifyPassword(loginPw)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.")
        }
    }
}
