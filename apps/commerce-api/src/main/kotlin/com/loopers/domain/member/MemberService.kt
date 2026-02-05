package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun register(request: MemberRegisterRequest): MemberModel {
        memberRepository.findByLoginId(request.loginId)?.let {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.")
        }

        val member = MemberModel(
            loginId = request.loginId,
            password = request.password,
            name = request.name,
            email = request.email,
            birthDate = request.birthDate,
        )

        member.encryptPassword(passwordEncoder.encode(request.password))

        return memberRepository.save(member)
    }

    @Transactional(readOnly = true)
    fun getMember(id: Long): MemberModel {
        return memberRepository.find(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "회원을 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun authenticate(loginId: String, password: String): MemberModel {
        val member = memberRepository.findByLoginId(loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")

        if (!passwordEncoder.matches(password, member.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")
        }

        return member
    }

    @Transactional
    fun changePassword(loginId: String, currentPassword: String, newPassword: String) {
        val member = authenticate(loginId, currentPassword)

        if (passwordEncoder.matches(newPassword, member.password)) {
            throw CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호는 사용할 수 없습니다.")
        }
        member.changePassword(newPassword)
        member.encryptPassword(passwordEncoder.encode(newPassword))

        memberRepository.save(member)
    }
}
