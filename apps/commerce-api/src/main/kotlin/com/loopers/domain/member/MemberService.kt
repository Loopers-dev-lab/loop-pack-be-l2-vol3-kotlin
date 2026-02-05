package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberService(
    private val memberRepository: MemberRepository,
) {
    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

    @Transactional
    fun register(command: RegisterCommand): MemberModel {
        if (memberRepository.existsByLoginId(command.loginId)) {
            throw CoreException(errorType = ErrorType.CONFLICT, customMessage = "이미 존재하는 로그인ID입니다.")
        }

        // Validate raw password before encoding
        val tempMember = MemberModel(
            loginId = command.loginId,
            password = command.password,
            name = command.name,
            birthDate = command.birthDate,
            email = command.email,
        )

        val encodedPassword = passwordEncoder.encode(command.password)
        val member = MemberModel(
            loginId = command.loginId,
            password = encodedPassword,
            name = command.name,
            birthDate = command.birthDate,
            email = command.email,
            skipPasswordValidation = true,
        )

        return memberRepository.save(member)
    }

    @Transactional(readOnly = true)
    fun getMyInfo(loginId: String, password: String): MemberModel {
        val member = memberRepository.findByLoginId(loginId)
            ?: throw CoreException(errorType = ErrorType.NOT_FOUND, customMessage = "회원을 찾을 수 없습니다.")

        if (!passwordEncoder.matches(password, member.password)) {
            throw CoreException(errorType = ErrorType.BAD_REQUEST, customMessage = "비밀번호가 일치하지 않습니다.")
        }

        return member
    }

    @Transactional
    fun changePassword(loginId: String, currentPassword: String, newPassword: String) {
        val member = memberRepository.findByLoginId(loginId)
            ?: throw CoreException(errorType = ErrorType.NOT_FOUND, customMessage = "회원을 찾을 수 없습니다.")

        if (!passwordEncoder.matches(currentPassword, member.password)) {
            throw CoreException(errorType = ErrorType.BAD_REQUEST, customMessage = "현재 비밀번호가 일치하지 않습니다.")
        }

        if (currentPassword == newPassword) {
            throw CoreException(errorType = ErrorType.BAD_REQUEST, customMessage = "새 비밀번호는 현재 비밀번호와 달라야 합니다.")
        }

        val encodedNewPassword = passwordEncoder.encode(newPassword)
        member.updateEncodedPassword(encodedNewPassword)
    }
}
