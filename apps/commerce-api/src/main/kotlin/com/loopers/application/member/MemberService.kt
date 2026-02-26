package com.loopers.application.member

import com.loopers.domain.common.vo.Email
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.domain.member.MemberModel
import com.loopers.domain.member.MemberRepository
import com.loopers.domain.member.RawPassword
import com.loopers.domain.member.vo.LoginId
import com.loopers.domain.member.vo.MemberName
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun register(
        loginId: String,
        password: String,
        name: String,
        birthday: LocalDate,
        email: String,
    ): MemberModel {
        LoginId.of(loginId)
        MemberName.of(name)
        Email.of(email)
        RawPassword.validate(password, birthday)

        val member = MemberModel(
            loginId = loginId,
            password = passwordEncoder.encode(password),
            name = name,
            birthday = birthday,
            email = email,
        )
        return memberRepository.save(member)
    }

    fun getMemberByLoginId(loginId: String): MemberModel {
        return memberRepository.findByLoginId(loginId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다.")
    }

    fun authenticate(loginId: String, password: String): MemberModel {
        val member = memberRepository.findByLoginId(loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")

        if (!passwordEncoder.matches(password, member.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")
        }

        return member
    }

    fun changePassword(loginId: String, newPassword: String) {
        val member = memberRepository.findByLoginId(loginId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다.")

        if (passwordEncoder.matches(newPassword, member.password)) {
            throw CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.")
        }

        RawPassword.validate(newPassword, member.birthday)
        val updated = member.changePassword(passwordEncoder.encode(newPassword))
        memberRepository.save(updated)
    }
}
