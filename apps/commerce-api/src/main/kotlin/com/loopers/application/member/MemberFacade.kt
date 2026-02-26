package com.loopers.application.member

import com.loopers.application.auth.AuthService
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class MemberFacade(
    private val memberService: MemberService,
    private val authService: AuthService,
) {
    @Transactional
    fun register(
        loginId: String,
        password: String,
        name: String,
        birthday: LocalDate,
        email: String,
    ): MemberInfo {
        try {
            val member = memberService.register(
                loginId = loginId,
                password = password,
                name = name,
                birthday = birthday,
                email = email,
            )
            return MemberInfo.from(member)
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.")
        }
    }

    @Transactional(readOnly = true)
    fun getMyInfo(loginId: String): MemberInfo {
        val member = memberService.getMemberByLoginId(loginId)
        return MemberInfo.from(member)
    }

    @Transactional
    fun changePassword(loginId: String, newPassword: String): MemberInfo {
        memberService.changePassword(loginId, newPassword)
        authService.evictAuthCache(loginId)
        val member = memberService.getMemberByLoginId(loginId)
        return MemberInfo.from(member)
    }
}
