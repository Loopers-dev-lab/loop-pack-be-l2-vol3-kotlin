package com.loopers.interfaces.api.member

import com.loopers.domain.member.AuthService
import com.loopers.domain.member.MemberService
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class MemberV1Controller(
    private val memberService: MemberService,
    private val authService: AuthService,
) {

    @PostMapping("/signup")
    fun signUp(
        @RequestBody request: MemberV1Dto.SignUpRequest,
    ): ApiResponse<MemberV1Dto.SignUpResponse> {
        return memberService.signUp(request.toCommand())
            .let { MemberV1Dto.SignUpResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/me")
    fun getMyInfo(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<MemberV1Dto.MemberInfoResponse> {
        val member = authService.authenticate(loginId, password)
        return MemberV1Dto.MemberInfoResponse.from(member)
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/me/password")
    fun changePassword(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestBody request: MemberV1Dto.ChangePasswordRequest,
    ): ApiResponse<Any> {
        val member = authService.authenticate(loginId, password)
        memberService.changePassword(member.id, request.currentPassword, request.newPassword)
        return ApiResponse.success()
    }
}
