package com.loopers.interfaces.api.member

import com.loopers.application.auth.AuthFacade
import com.loopers.application.member.MemberFacade
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
    private val authFacade: AuthFacade,
    private val memberFacade: MemberFacade,
) : MemberV1ApiSpec {

    @GetMapping("/me")
    override fun getMyProfile(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<MemberV1Dto.MyProfileResponse> {
        // 인증 검증
        authFacade.authenticate(loginId, password)

        return memberFacade.getMyProfile(loginId)
            .let { MemberV1Dto.MyProfileResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/me/password")
    override fun changePassword(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestBody request: MemberV1Dto.ChangePasswordRequest,
    ): ApiResponse<Any> {
        // 인증 검증
        authFacade.authenticate(loginId, password)

        memberFacade.changePassword(request.toCommand(loginId))

        return ApiResponse.success()
    }
}
