package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

/**
 * 회원 도메인 서비스
 */
@Component
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun signUp(command: SignUpCommand): Member {
        validateLoginIdNotDuplicated(command.loginId)
        validatePassword(command.password, command.birthDate.format(BIRTH_DATE_FORMAT))

        val encodedPassword = passwordEncoder.encode(command.password)

        return memberRepository.save(
            Member(
                loginId = command.loginId,
                password = encodedPassword,
                name = command.name,
                birthDate = command.birthDate,
                email = command.email,
            ),
        )
    }

    @Transactional(readOnly = true)
    fun getMyInfo(memberId: Long): Member {
        return memberRepository.findById(memberId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "회원을 찾을 수 없습니다.")
    }

    @Transactional
    fun changePassword(memberId: Long, currentPassword: String, newPassword: String) {
        val member = memberRepository.findById(memberId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "회원을 찾을 수 없습니다.")

        if (!passwordEncoder.matches(currentPassword, member.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.")
        }

        if (currentPassword == newPassword) {
            throw CoreException(ErrorType.BAD_REQUEST, "새 비밀번호는 현재 비밀번호와 달라야 합니다.")
        }

        validatePassword(newPassword, member.birthDate.format(BIRTH_DATE_FORMAT))

        val encodedNewPassword = passwordEncoder.encode(newPassword)
        member.changePassword(encodedNewPassword)
        memberRepository.save(member)
    }

    private fun validateLoginIdNotDuplicated(loginId: String) {
        if (memberRepository.existsByLoginId(loginId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.")
        }
    }

    private fun validatePassword(password: String, birthDateStr: String) {
        if (password.length < MIN_PASSWORD_LENGTH) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 ${MIN_PASSWORD_LENGTH}자 이상이어야 합니다.")
        }
        if (password.length > MAX_PASSWORD_LENGTH) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 ${MAX_PASSWORD_LENGTH}자 이하여야 합니다.")
        }
        if (!password.matches(PASSWORD_PATTERN)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 영문 대소문자, 숫자, 특수문자만 사용 가능합니다.")
        }
        if (password.contains(birthDateStr)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다.")
        }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_PASSWORD_LENGTH = 16
        private val PASSWORD_PATTERN = Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$")
        private val BIRTH_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
