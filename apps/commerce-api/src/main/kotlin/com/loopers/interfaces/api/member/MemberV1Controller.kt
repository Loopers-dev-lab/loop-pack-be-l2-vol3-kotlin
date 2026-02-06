package com.loopers.interfaces.api.member

import com.loopers.application.member.MemberFacade
import com.loopers.domain.auth.AuthenticatedMember
import com.loopers.infrastructure.auth.JwtAuthenticationFilter
import com.loopers.interfaces.api.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class MemberV1Controller(
    private val memberFacade: MemberFacade,
) : MemberV1ApiSpec {

    @GetMapping("/me")
    override fun getMyInfo(
        request: HttpServletRequest,
    ): ApiResponse<MemberV1Dto.MyInfoResponse> {
        val authenticatedMember = request.getAttribute(
            JwtAuthenticationFilter.AUTHENTICATED_MEMBER_ATTRIBUTE,
        ) as AuthenticatedMember

        return memberFacade.getMyInfo(authenticatedMember.memberId)
            .let { MemberV1Dto.MyInfoResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping("/sign-up")
    override fun signUp(
        @RequestBody request: MemberV1Dto.SignUpRequest,
    ): ApiResponse<MemberV1Dto.SignUpResponse> {
        return memberFacade.signUp(request)
            .let { MemberV1Dto.SignUpResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
