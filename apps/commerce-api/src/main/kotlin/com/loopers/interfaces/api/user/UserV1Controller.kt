package com.loopers.interfaces.api.user

import com.loopers.application.user.UserFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserV1Controller(
    private val userFacade: UserFacade,
)  : UserV1ApiSpec {
    @PostMapping("/register")
    override fun registerUser(@RequestBody req: UserV1Dto.RegisterUserRequest): ApiResponse<UserV1Dto.RegisterUserResponse> {
        return userFacade.registerUser(req.loginId, req.password, req.name, req.birth, req.email)
            .let { UserV1Dto.RegisterUserResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
