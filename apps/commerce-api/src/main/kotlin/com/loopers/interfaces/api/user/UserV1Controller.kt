package com.loopers.interfaces.api.user

import com.loopers.application.auth.AuthFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.security.AuthHeader
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserV1Controller(
    private val authFacade: AuthFacade,
) : UserV1ApiSpec {

    /**
     * 사용자 회원가입
     * @param request [UserV1Dto.SignupRequest] 회원가입 양식 DTO
     * @return ApiResponse<UserV1Dto.UserResponse>
     */
    @PostMapping("/signup")
    override fun signup(@RequestBody request: UserV1Dto.SignupRequest): ApiResponse<UserV1Dto.UserResponse> {
        return authFacade.signup(
            userId = request.userId,
            rawPassword = request.password,
            name = request.name,
            birthDate = request.birthDate,
            email = request.email,
        )
            .let { UserV1Dto.UserResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    /**
     * 나의 정보 조회
     * @param loginId [AuthHeader.HEADER_LOGIN_ID]
     * @param loginPw [AuthHeader.HEADER_LOGIN_PW]
     * @return ApiResponse<UserV1Dto.UserResponse>
     */
    @GetMapping("/me")
    override fun getMyInfo(
        @RequestHeader(AuthHeader.HEADER_LOGIN_ID) loginId: String,
        @RequestHeader(AuthHeader.HEADER_LOGIN_PW) loginPw: String,
    ): ApiResponse<UserV1Dto.UserResponse> {
        return authFacade.authenticate(loginId, loginPw)
            .let { UserV1Dto.UserResponse.fromMasked(it) }
            .let { ApiResponse.success(it) }
    }

    /**
     * 사용자 패스워드 변경
     * @param loginId [AuthHeader.HEADER_LOGIN_ID]
     * @param loginPw [AuthHeader.HEADER_LOGIN_PW]
     * @param request [UserV1Dto.UserChangePasswordRequest] 비밀번호 변경 양식 DTO
     * @return ApiResponse<Any> 사용자 패스워드 변경 성공여부
     */
    @PutMapping("/password")
    override fun changePassword(
        @RequestHeader(AuthHeader.HEADER_LOGIN_ID) loginId: String,
        @RequestHeader(AuthHeader.HEADER_LOGIN_PW) loginPw: String,
        @RequestBody @Valid request: UserV1Dto.UserChangePasswordRequest,
    ): ApiResponse<Any> {
        authFacade.authenticate(loginId, loginPw)
        authFacade.changePassword(loginId, request.oldPassword, request.newPassword)
        return ApiResponse.success()
    }
}
