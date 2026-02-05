package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class MemberService(
    private val memberRepository: MemberRepository,
) {
    @Transactional
    fun register(
        loginId: String,
        password: String,
        name: String,
        birthday: LocalDate,
        email: String,
    ) {
        if (memberRepository.existsByLoginId(loginId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.")
        }

        val member = MemberModel(
            loginId = loginId,
            password = password,
            name = name,
            birthday = birthday,
            email = email,
        )
        try {
            memberRepository.save(member)
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.")
        }
    }

    @Transactional(readOnly = true)
    fun getMemberByLoginId(loginId: String): MemberModel {
        return memberRepository.findByLoginId(loginId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다.")
    }

    @Transactional(readOnly = true)
    fun authenticate(loginId: String, password: String): MemberModel {
        val member = memberRepository.findByLoginId(loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")

        if (!member.matchesPassword(password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")
        }

        return member
    }

    @Transactional
    fun changePassword(loginId: String, newPassword: String) {
        val member = memberRepository.findByLoginId(loginId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다.")

        member.changePassword(newPassword, member.birthday)
    }
}
