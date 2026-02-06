package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

/**
 * 인증 서비스
 * - X-Loopers-LoginId, X-Loopers-LoginPw 헤더를 통한 인증 처리
 */
@Component
class AuthService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    fun authenticate(loginId: String, password: String): Member {
        val member = memberRepository.findByLoginId(loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "로그인 정보가 올바르지 않습니다.")

        if (!passwordEncoder.matches(password, member.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "로그인 정보가 올바르지 않습니다.")
        }

        return member
    }
}
