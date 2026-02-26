package com.loopers.interfaces.api.member

import com.loopers.application.member.MemberFacade
import com.loopers.interfaces.config.auth.MemberAuthenticated
import com.loopers.interfaces.config.auth.AuthenticatedMember
import com.loopers.interfaces.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class MemberV1Controller(
    private val memberFacade: MemberFacade,
) : MemberV1ApiSpec {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun register(
        @RequestBody @Valid request: MemberV1Dto.RegisterRequest,
    ): ApiResponse<MemberV1Dto.MemberResponse> {
        return memberFacade.register(
            loginId = request.loginId,
            password = request.password,
            name = request.name,
            birthday = request.birthday,
            email = request.email,
        ).let { MemberV1Dto.MemberResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @MemberAuthenticated
    @GetMapping("/me")
    override fun getMe(
        authenticatedMember: AuthenticatedMember,
    ): ApiResponse<MemberV1Dto.MemberResponse> {
        return memberFacade.getMyInfo(authenticatedMember.loginId)
            .let { MemberV1Dto.MemberResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @MemberAuthenticated
    @PatchMapping("/me/password")
    override fun changePassword(
        authenticatedMember: AuthenticatedMember,
        @RequestBody @Valid request: MemberV1Dto.ChangePasswordRequest,
    ): ApiResponse<MemberV1Dto.MemberResponse> {
        return memberFacade.changePassword(authenticatedMember.loginId, request.newPassword)
            .let { MemberV1Dto.MemberResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
