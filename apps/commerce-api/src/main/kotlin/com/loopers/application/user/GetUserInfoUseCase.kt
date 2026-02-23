package com.loopers.application.user

import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component

@Component
class GetUserInfoUseCase(
    private val userService: UserService,
) {
    fun execute(userId: Long): UserInfo {
        return UserInfo.from(userService.getUser(userId))
    }
}
