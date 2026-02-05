package com.loopers.interfaces.api.member

import com.loopers.application.member.MemberFacade
import com.loopers.interfaces.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
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
        @Valid @RequestBody request: MemberV1Dto.RegisterRequest,
    ): ApiResponse<*> {
        memberFacade.register(request)
        return ApiResponse.success()
    }

    @GetMapping("/me")
    override fun getMyInfo(
        @RequestHeader(value = "X-Loopers-LoginId", required = true) loginId: String,
        @RequestHeader(value = "X-Loopers-LoginPw", required = true) password: String,
    ): ApiResponse<MemberV1Dto.MemberResponse> {
        return memberFacade.getMyInfo(loginId, password)
            .let { MemberV1Dto.MemberResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/me/password")
    override fun changePassword(
        @RequestHeader(value = "X-Loopers-LoginId", required = true) loginId: String,
        @RequestHeader(value = "X-Loopers-LoginPw", required = true) currentPassword: String,
        @Valid @RequestBody request: MemberV1Dto.ChangePasswordRequest,
    ): ApiResponse<*> {
        memberFacade.changePassword(loginId, currentPassword, request.newPassword)
        return ApiResponse.success()
    }
}
