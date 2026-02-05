package com.loopers.application.member

import com.loopers.domain.member.MemberService
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class MemberFacade(
    private val memberService: MemberService,
) {
    fun register(
        loginId: String,
        password: String,
        name: String,
        birthday: LocalDate,
        email: String,
    ) {
        memberService.register(
            loginId = loginId,
            password = password,
            name = name,
            birthday = birthday,
            email = email,
        )
    }

    fun getMyInfo(loginId: String, password: String): MemberInfo {
        val member = memberService.authenticate(loginId, password)
        return MemberInfo.from(member)
    }

    fun changePassword(loginId: String, password: String, newPassword: String) {
        memberService.changePassword(loginId, password, newPassword)
    }
}
