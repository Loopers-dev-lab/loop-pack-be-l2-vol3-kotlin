package com.loopers.user.interfaces.api

import com.loopers.example.interfaces.api.ApiResponse
import com.loopers.user.application.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserV1Controller(
    private val userService: UserService,
) : UserV1ApiSpec {
    @PostMapping
    override fun signUp(
        @Valid @RequestBody request: UserV1Dto.SignUpRequest,
    ): ResponseEntity<ApiResponse<UserV1Dto.SignUpResponse>> {
        return userService.signUp(request.toCommand())
            .let { UserV1Dto.SignUpResponse.from(it) }
            .let { ApiResponse.success(it) }
            .let { ResponseEntity.status(HttpStatus.CREATED).body(it) }
    }
}
