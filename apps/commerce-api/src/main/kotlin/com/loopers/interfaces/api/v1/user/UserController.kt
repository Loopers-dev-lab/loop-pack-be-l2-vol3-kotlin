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
    /**
     * Creates a new user account.
     *
     * @param request The user registration payload containing required user details.
     * @return An ApiResponse containing a CreateUserResponse with the newly created user's id.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: CreateUserRequest,
    ): ApiResponse<CreateUserResponse> {
        val id = registerUserUseCase.register(request.toCommand())
        return ApiResponse.success(CreateUserResponse(id))
    }

    /**
     * Retrieves the currently authenticated user's profile information.
     *
     * @param authUser The authenticated user principal whose id is used to fetch profile data.
     * @return An ApiResponse containing the current user's GetMyInfoResponse.
     */
    @GetMapping("/me")
    fun getMyInfo(
        @AuthenticatedUser authUser: AuthUser,
    ): ApiResponse<GetMyInfoResponse> {
        val userInfo = getMyInfoUseCase.execute(authUser.id)
        return ApiResponse.success(GetMyInfoResponse.from(userInfo))
    }

    /**
     * Changes the authenticated user's password.
     *
     * @param authUser The authenticated user performing the request.
     * @param request The change password request containing current and new password data.
     * @return An ApiResponse with a null payload indicating the operation succeeded.
     */
    @PutMapping("/password")
    fun changePassword(
        @AuthenticatedUser authUser: AuthUser,
        @Valid @RequestBody request: ChangePasswordRequest,
    ): ApiResponse<Nothing?> {
        changePasswordUseCase.execute(authUser.id, request.toCommand())
        return ApiResponse.success(null)
    }
}