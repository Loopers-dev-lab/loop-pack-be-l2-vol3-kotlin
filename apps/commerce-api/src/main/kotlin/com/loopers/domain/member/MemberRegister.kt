package com.loopers.domain.member

import com.loopers.domain.member.vo.BirthDate
import com.loopers.domain.member.vo.Email
import com.loopers.domain.member.vo.LoginId
import com.loopers.domain.member.vo.Name
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 회원 등록 담당 서비스
 */
@Component
class MemberRegister(
    private val memberRepository: MemberRepository,
    private val passwordPolicy: PasswordPolicy,
) {

    /**
     * 새 회원을 등록합니다.
     * @throws CoreException DUPLICATE_LOGIN_ID if loginId already exists
     */
    fun register(
        loginId: String,
        rawPassword: String,
        name: String,
        birthDate: LocalDate,
        email: String,
    ): Member {
        val loginIdVo = LoginId(loginId)

        if (memberRepository.existsByLoginId(loginIdVo)) {
            throw CoreException(ErrorType.DUPLICATE_LOGIN_ID)
        }

        val birthDateVo = BirthDate(birthDate)
        val password = passwordPolicy.createPassword(rawPassword, birthDateVo.value)

        val member = Member(
            loginId = loginIdVo,
            password = password,
            name = Name(name),
            birthDate = birthDateVo,
            email = Email(email),
        )

        return memberRepository.save(member)
    }
}
