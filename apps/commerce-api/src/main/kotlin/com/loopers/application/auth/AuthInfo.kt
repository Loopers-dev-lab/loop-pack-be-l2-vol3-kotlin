package com.loopers.application.auth

import com.loopers.domain.member.Member
import java.time.LocalDate

class AuthInfo {

    data class SignupResult(
        val id: Long,
        val loginId: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        companion object {
            fun from(member: Member) = SignupResult(
                id = requireNotNull(member.id) { "회원 저장 후 ID가 할당되지 않았습니다." },
                loginId = member.loginId.value,
                name = member.name.value,
                birthDate = member.birthDate.value,
                email = member.email.value,
            )
        }
    }
}
