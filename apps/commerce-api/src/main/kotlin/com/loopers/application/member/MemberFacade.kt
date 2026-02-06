package com.loopers.application.member

import com.loopers.domain.member.MemberService
import com.loopers.infrastructure.member.BCryptPasswordEncoder
import com.loopers.interfaces.api.member.MemberV1Dto
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.support.util.PasswordValidator
import org.springframework.stereotype.Component

@Component
class MemberFacade(
    private val memberService: MemberService,
    private val passwordEncoder: BCryptPasswordEncoder,
) {
    fun getMyInfo(memberId: Long): MemberInfo {
        val member = memberService.findById(memberId)
        return MemberInfo.from(member)
    }

    fun signUp(request: MemberV1Dto.SignUpRequest): MemberInfo {
        val encodedPassword = passwordEncoder.encode(request.password)

        val member = memberService.signUp(
            loginId = request.loginId,
            encodedPassword = encodedPassword,
            name = request.name,
            birthDate = request.birthDate,
            email = request.email,
        )

        return MemberInfo.from(member)
    }

    fun changePassword(memberId: Long, currentPassword: String, newPassword: String) {
        val member = memberService.findById(memberId)

        if (!passwordEncoder.matches(currentPassword, member.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.")
        }

        if (currentPassword == newPassword) {
            throw CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.")
        }

        PasswordValidator.validatePassword(newPassword, member.birthDate, member.loginId)

        val encodedPassword = passwordEncoder.encode(newPassword)
        memberService.changePassword(memberId, encodedPassword)
    }
}
