package com.loopers.application.auth

import com.loopers.domain.member.Member
import com.loopers.domain.member.MemberAuthenticator
import com.loopers.domain.member.MemberRegister
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class AuthUseCase(
    private val memberRegister: MemberRegister,
    private val memberAuthenticator: MemberAuthenticator,
) {

    @Transactional
    fun signup(command: SignupCommand): AuthInfo.SignupResult {
        val member = memberRegister.register(
            loginId = command.loginId,
            rawPassword = command.rawPassword,
            name = command.name,
            birthDate = command.birthDate,
            email = command.email,
        )
        return AuthInfo.SignupResult.from(member)
    }

    @Transactional(readOnly = true)
    fun authenticate(loginId: String, rawPassword: String): Member {
        return memberAuthenticator.authenticate(
            loginId = loginId,
            rawPassword = rawPassword,
        )
    }

    data class SignupCommand(
        val loginId: String,
        val rawPassword: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    )
}
