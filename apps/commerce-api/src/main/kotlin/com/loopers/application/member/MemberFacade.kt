package com.loopers.application.member

import com.loopers.domain.member.MemberService
import com.loopers.domain.member.RegisterCommand
import com.loopers.interfaces.api.member.MemberV1Dto
import org.springframework.stereotype.Component

@Component
class MemberFacade(
    private val memberService: MemberService,
) {
    fun register(request: MemberV1Dto.RegisterRequest) {
        val command = RegisterCommand(
            loginId = request.loginId,
            password = request.password,
            name = request.name,
            birthDate = request.birthDate,
            email = request.email,
        )
        memberService.register(command)
    }

    fun getMyInfo(loginId: String, password: String): MemberInfo {
        return memberService.getMyInfo(loginId, password)
            .let { MemberInfo.from(it) }
    }

    fun changePassword(loginId: String, currentPassword: String, newPassword: String) {
        memberService.changePassword(loginId, currentPassword, newPassword)
    }
}
