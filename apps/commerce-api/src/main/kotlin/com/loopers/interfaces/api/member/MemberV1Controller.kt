package com.loopers.interfaces.api.member

import com.loopers.domain.member.MemberRegisterRequest
import com.loopers.domain.member.MemberService
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class MemberV1Controller(
    private val memberService: MemberService,
) {
    @PostMapping
    fun register(
        @RequestBody request: MemberV1Dto.RegisterRequest,
    ): ApiResponse<MemberV1Dto.RegisterResponse> {
        val memberRegisterRequest = MemberRegisterRequest(
            loginId = request.loginId,
            password = request.password,
            name = request.name,
            email = request.email,
            birthDate = request.birthDate,
        )
        return memberService.register(memberRegisterRequest)
            .let { MemberV1Dto.RegisterResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/me")
    fun getMe(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<MemberV1Dto.MemberResponse> {
        return memberService.authenticate(loginId, password)
            .let { MemberV1Dto.MemberResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PutMapping("/me/password")
    fun changePassword(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestBody request: MemberV1Dto.ChangePasswordRequest,
    ): ApiResponse<Any> {
        memberService.changePassword(loginId, request.currentPassword, request.newPassword)
        return ApiResponse.success()
    }
}
