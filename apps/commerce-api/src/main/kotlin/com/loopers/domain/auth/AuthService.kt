package com.loopers.domain.auth

import com.loopers.domain.member.MemberModel
import com.loopers.domain.member.MemberRepository
import com.loopers.infrastructure.member.BCryptPasswordEncoder
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class AuthService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
) {
    fun authenticate(loginId: String, rawPassword: String): MemberModel {
        val member = memberRepository.findByLoginId(loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.")

        if (!passwordEncoder.matches(rawPassword, member.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.")
        }

        return member
    }
}
