package com.loopers.interfaces.api.auth

import com.loopers.application.auth.TokenInfo
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class AuthV1Dto {
    data class LoginRequest(
        val loginId: String,
        val password: String,
    ) {
        init {
            if (loginId.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 비어있을 수 없습니다.")
            }
            if (password.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 비어있을 수 없습니다.")
            }
        }
    }

    data class LoginResponse(
        val accessToken: String,
        val tokenType: String,
        val expiresIn: Long,
    ) {
        companion object {
            fun from(tokenInfo: TokenInfo): LoginResponse {
                return LoginResponse(
                    accessToken = tokenInfo.accessToken,
                    tokenType = tokenInfo.tokenType,
                    expiresIn = tokenInfo.expiresIn,
                )
            }
        }
    }
}
