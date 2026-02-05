package com.loopers.application.member

import com.loopers.domain.member.MemberCommand
import com.loopers.domain.member.MemberService
import org.springframework.stereotype.Component

@Component
class MemberFacade(
    private val memberService: MemberService,
) {
    fun register(command: MemberCommand.Register): MemberInfo {
        return memberService.register(command)
            .let { MemberInfo.from(it) }
    }

    fun changePassword(command: MemberCommand.ChangePassword) {
        memberService.changePassword(command)
    }
}
