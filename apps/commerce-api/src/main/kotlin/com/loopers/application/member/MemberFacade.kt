package com.loopers.application.member

import com.loopers.domain.member.MemberService
import com.loopers.infrastructure.member.BCryptPasswordEncoder
import com.loopers.interfaces.api.member.MemberV1Dto
import org.springframework.stereotype.Component

@Component
class MemberFacade(
    private val memberService: MemberService,
    private val passwordEncoder: BCryptPasswordEncoder,
) {
    fun getMyInfo(memberId: Long): MemberInfo {
        val member = memberService.findById(memberId)
        return MemberInfo.from(member)
    }

    fun signUp(request: MemberV1Dto.SignUpRequest): MemberInfo {
        val encodedPassword = passwordEncoder.encode(request.password)

        val member = memberService.signUp(
            loginId = request.loginId,
            encodedPassword = encodedPassword,
            name = request.name,
            birthDate = request.birthDate,
            email = request.email,
        )

        return MemberInfo.from(member)
    }
}
