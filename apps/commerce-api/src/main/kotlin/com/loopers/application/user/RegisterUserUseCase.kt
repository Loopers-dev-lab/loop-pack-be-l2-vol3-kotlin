package com.loopers.application.user

import com.loopers.domain.point.UserPointService
import com.loopers.domain.user.UserCommand
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class RegisterUserUseCase(
    private val userService: UserService,
    private val userPointService: UserPointService,
) {
    @Transactional
    fun execute(loginId: String, password: String, name: String, birthDate: LocalDate, email: String): UserInfo {
        val command = UserCommand.SignUp(
            loginId = loginId,
            password = password,
            name = name,
            birthDate = birthDate,
            email = email,
        )
        val user = userService.signUp(command)
        userPointService.createUserPoint(user.id)
        return UserInfo.from(user)
    }
}
