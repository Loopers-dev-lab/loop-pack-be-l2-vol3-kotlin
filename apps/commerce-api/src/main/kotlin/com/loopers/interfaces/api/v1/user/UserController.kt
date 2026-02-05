package com.loopers.interfaces.api.v1.user

import com.loopers.application.user.ChangePasswordUseCase
import com.loopers.application.user.GetMyInfoUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthUser
import com.loopers.interfaces.api.auth.AuthenticatedUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val getMyInfoUseCase: GetMyInfoUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: CreateUserRequest,
    ): ApiResponse<CreateUserResponse> {
        val id = registerUserUseCase.register(request.toCommand())
        return ApiResponse.success(CreateUserResponse(id))
    }

    @GetMapping("/me")
    fun getMyInfo(
        @AuthenticatedUser authUser: AuthUser,
    ): ApiResponse<GetMyInfoResponse> {
        val userInfo = getMyInfoUseCase.execute(authUser.id)
        return ApiResponse.success(GetMyInfoResponse.from(userInfo))
    }

    @PutMapping("/password")
    fun changePassword(
        @AuthenticatedUser authUser: AuthUser,
        @Valid @RequestBody request: ChangePasswordRequest,
    ): ApiResponse<Nothing?> {
        changePasswordUseCase.execute(authUser.id, request.toCommand())
        return ApiResponse.success(null)
    }
}
