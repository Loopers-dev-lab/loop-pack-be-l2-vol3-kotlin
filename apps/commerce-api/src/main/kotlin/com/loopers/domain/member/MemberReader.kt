package com.loopers.domain.member

import com.loopers.domain.member.vo.LoginId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

/**
 * 회원 조회 담당 서비스
 */
@Component
class MemberReader(
    private val memberRepository: MemberRepository,
) {

    /**
     * 로그인ID로 회원을 조회합니다.
     * @throws CoreException MEMBER_NOT_FOUND if member doesn't exist
     */
    fun getByLoginId(loginId: String): Member {
        return memberRepository.findByLoginId(LoginId(loginId))
            ?: throw CoreException(ErrorType.MEMBER_NOT_FOUND)
    }
}
