package com.loopers.domain.member

import com.loopers.domain.member.vo.LoginId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

/**
 * 회원 인증 담당 서비스
 */
@Component
class MemberAuthenticator(
    private val memberRepository: MemberRepository,
    private val passwordPolicy: PasswordPolicy,
) {

    /**
     * 로그인ID와 비밀번호로 인증합니다.
     * @throws CoreException MEMBER_NOT_FOUND if member doesn't exist
     * @throws CoreException AUTHENTICATION_FAILED if password doesn't match
     */
    fun authenticate(loginId: String, rawPassword: String): Member {
        val member = memberRepository.findByLoginId(LoginId(loginId))
            ?: throw CoreException(ErrorType.MEMBER_NOT_FOUND)

        if (!member.authenticate(rawPassword, passwordPolicy)) {
            throw CoreException(ErrorType.AUTHENTICATION_FAILED)
        }

        return member
    }
}
