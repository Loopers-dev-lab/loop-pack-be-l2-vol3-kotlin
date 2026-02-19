package com.loopers.application.user

import com.loopers.domain.point.UserPointService
import com.loopers.domain.user.User
import com.loopers.domain.user.UserCommand
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserFacade(
    private val userService: UserService,
    private val userPointService: UserPointService,
) {

    @Transactional
    fun signUp(command: UserCommand.SignUp): User {
        val user = userService.signUp(command)
        userPointService.createUserPoint(user.id)
        return user
    }
}
