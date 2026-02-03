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
        TODO("Not yet implemented")
    }

    @Transactional(readOnly = true)
    fun getMember(id: Long): MemberModel {
        TODO("Not yet implemented")
    }

    @Transactional(readOnly = true)
    fun authenticate(loginId: String, password: String): MemberModel {
        TODO("Not yet implemented")
    }

    @Transactional
    fun changePassword(loginId: String, currentPassword: String, newPassword: String) {
        TODO("Not yet implemented")
    }
}
