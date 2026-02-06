package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class MemberService(
    private val memberRepository: MemberRepository,
) {
    @Transactional
    fun signUp(
        loginId: String,
        encodedPassword: String,
        name: String,
        birthDate: LocalDate,
        email: String,
    ): MemberModel {
        if (memberRepository.existsByLoginId(loginId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다: $loginId")
        }

        val member = MemberModel(
            loginId = loginId,
            password = encodedPassword,
            name = name,
            birthDate = birthDate,
            email = email,
        )

        return memberRepository.save(member)
    }
}
