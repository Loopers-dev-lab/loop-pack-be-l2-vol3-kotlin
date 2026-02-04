package com.loopers.interfaces.api.member

import com.loopers.domain.member.MemberService
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class MemberV1Controller(
    private val memberService: MemberService,
) {

    @PostMapping("/signup")
    fun signUp(
        @RequestBody request: MemberV1Dto.SignUpRequest,
    ): ApiResponse<MemberV1Dto.SignUpResponse> {
        return memberService.signUp(request.toCommand())
            .let { MemberV1Dto.SignUpResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{memberId}")
    fun getMyInfo(
        @PathVariable memberId: Long,
    ): ApiResponse<MemberV1Dto.MemberInfoResponse> {
        return memberService.getMyInfo(memberId)
            .let { MemberV1Dto.MemberInfoResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/{memberId}/password")
    fun changePassword(
        @PathVariable memberId: Long,
        @RequestBody request: MemberV1Dto.ChangePasswordRequest,
    ): ApiResponse<Any> {
        memberService.changePassword(memberId, request.currentPassword, request.newPassword)
        return ApiResponse.success()
    }
}
