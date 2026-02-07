package com.loopers.domain.member

import com.loopers.domain.member.vo.LoginId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

/**
 * 회원 비밀번호 변경 담당 서비스
 */
@Component
class MemberPasswordChanger(
    private val memberRepository: MemberRepository,
    private val passwordPolicy: PasswordPolicy,
) {

    /**
     * 비밀번호를 변경합니다.
     * @throws CoreException MEMBER_NOT_FOUND if member doesn't exist
     * @throws CoreException AUTHENTICATION_FAILED if current password doesn't match
     * @throws CoreException SAME_PASSWORD_NOT_ALLOWED if new password is same as current
     * @throws CoreException PASSWORD_CONTAINS_BIRTHDATE if new password contains birthdate
     */
    fun changePassword(
        loginId: String,
        currentRawPassword: String,
        newRawPassword: String,
    ) {
        val member = memberRepository.findByLoginId(LoginId(loginId))
            ?: throw CoreException(ErrorType.MEMBER_NOT_FOUND)

        member.changePassword(currentRawPassword, newRawPassword, passwordPolicy)
        memberRepository.save(member)
    }
}
