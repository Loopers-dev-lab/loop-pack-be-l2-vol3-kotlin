package com.loopers.domain.member

import com.loopers.domain.member.vo.BirthDate
import com.loopers.domain.member.vo.Email
import com.loopers.domain.member.vo.LoginId
import com.loopers.domain.member.vo.Name
import com.loopers.domain.member.vo.Password
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

/**
 * 회원 도메인 객체 (순수 도메인, JPA 의존성 없음)
 */
class Member(
    val id: Long? = null,
    val loginId: LoginId,
    password: Password,
    val name: Name,
    val birthDate: BirthDate,
    val email: Email,
) {
    var password: Password = password
        private set

    /**
     * 비밀번호를 변경합니다.
     * @param currentRawPassword 현재 평문 비밀번호 (검증용)
     * @param newRawPassword 새 평문 비밀번호
     * @param encoder 비밀번호 인코더
     */
    fun changePassword(currentRawPassword: String, newRawPassword: String, encoder: PasswordEncoder) {
        if (!authenticate(currentRawPassword, encoder)) {
            throw CoreException(ErrorType.AUTHENTICATION_FAILED, "현재 비밀번호가 일치하지 않습니다.")
        }

        if (currentRawPassword == newRawPassword) {
            throw CoreException(ErrorType.SAME_PASSWORD_NOT_ALLOWED)
        }

        this.password = Password.of(newRawPassword, birthDate.value, encoder)
    }

    /**
     * 비밀번호로 인증합니다.
     * @param rawPassword 평문 비밀번호
     * @param encoder 비밀번호 인코더
     * @return 인증 성공 여부
     */
    fun authenticate(rawPassword: String, encoder: PasswordEncoder): Boolean {
        return password.matches(rawPassword, encoder)
    }
}
