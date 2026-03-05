package com.loopers.application.user.auth

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserMeUseCase(
    private val userAuthenticateUseCase: UserAuthenticateUseCase,
) {
    @Transactional(readOnly = true)
    fun getMe(loginId: String, password: String): UserResult.Me {
        val user = userAuthenticateUseCase.authenticate(loginId, password)
        return UserResult.Me(
            loginId = user.loginId.value,
            name = user.maskedName,
            birthDate = user.birthDate,
            email = user.email.value,
        )
    }
}
