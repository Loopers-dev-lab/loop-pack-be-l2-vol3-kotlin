package com.loopers.interfaces.api.member

import com.loopers.application.auth.AuthUseCase
import com.loopers.application.member.MemberUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class MemberV1Controller(
    private val authUseCase: AuthUseCase,
    private val memberUseCase: MemberUseCase,
) : MemberV1ApiSpec {

    @GetMapping("/me")
    override fun getMyProfile(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<MemberV1Dto.MyProfileResponse> {
        authUseCase.authenticate(loginId, password)

        return memberUseCase.getMyProfile(loginId)
            .let { MemberV1Dto.MyProfileResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/me/password")
    override fun changePassword(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestBody request: MemberV1Dto.ChangePasswordRequest,
    ): ApiResponse<Any> {
        authUseCase.authenticate(loginId, password)

        memberUseCase.changePassword(request.toCommand(loginId))

        return ApiResponse.success()
    }
}
