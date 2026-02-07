package com.loopers.application.member

import com.loopers.domain.member.Member
import java.time.LocalDate

class MemberInfo {

    data class MyProfile(
        val loginId: String,
        // 마스킹 적용
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        companion object {
            fun from(member: Member) = MyProfile(
                loginId = member.loginId.value,
                // 마지막 글자 마스킹
                name = member.name.masked(),
                birthDate = member.birthDate.value,
                email = member.email.value,
            )
        }
    }
}
