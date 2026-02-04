package com.loopers.interfaces.api.member

import com.loopers.domain.member.MemberService
import com.loopers.interfaces.api.ApiResponse
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
}
