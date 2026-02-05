package com.loopers.interfaces.api.member

import com.loopers.application.member.MemberFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
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
    private val memberFacade: MemberFacade,
) : MemberV1ApiSpec {
    @PostMapping
    override fun register(
        @RequestBody request: MemberV1Dto.RegisterRequest,
    ): ApiResponse<Any> {
        memberFacade.register(
            loginId = request.loginId,
            password = request.password,
            name = request.name,
            birthday = request.birthday,
            email = request.email,
        )
        return ApiResponse.success()
    }

    @GetMapping("/me")
    override fun getMe(
        @RequestHeader(value = "X-Loopers-LoginId", required = false) loginId: String?,
        @RequestHeader(value = "X-Loopers-LoginPw", required = false) loginPw: String?,
    ): ApiResponse<MemberV1Dto.MemberResponse> {
        if (loginId.isNullOrBlank() || loginPw.isNullOrBlank()) {
            throw CoreException(ErrorType.UNAUTHORIZED, "인증 헤더가 누락되었습니다.")
        }
        return memberFacade.getMyInfo(loginId, loginPw)
            .let { MemberV1Dto.MemberResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/me/password")
    override fun changePassword(
        @RequestHeader(value = "X-Loopers-LoginId", required = false) loginId: String?,
        @RequestHeader(value = "X-Loopers-LoginPw", required = false) loginPw: String?,
        @RequestBody request: MemberV1Dto.ChangePasswordRequest,
    ): ApiResponse<Any> {
        if (loginId.isNullOrBlank() || loginPw.isNullOrBlank()) {
            throw CoreException(ErrorType.UNAUTHORIZED, "인증 헤더가 누락되었습니다.")
        }
        memberFacade.changePassword(loginId, loginPw, request.newPassword)
        return ApiResponse.success()
    }
}
