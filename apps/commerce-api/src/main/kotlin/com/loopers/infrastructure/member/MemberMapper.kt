package com.loopers.infrastructure.member

import com.loopers.domain.member.Member
import com.loopers.domain.member.vo.BirthDate
import com.loopers.domain.member.vo.Email
import com.loopers.domain.member.vo.LoginId
import com.loopers.domain.member.vo.Name
import com.loopers.domain.member.vo.Password
import org.springframework.stereotype.Component

@Component
class MemberMapper {

    fun toDomain(entity: MemberEntity): Member {
        return Member(
            id = entity.id,
            loginId = LoginId(entity.loginId),
            password = Password.fromEncoded(entity.password),
            name = Name(entity.name),
            birthDate = BirthDate(entity.birthDate),
            email = Email(entity.email),
        )
    }

    fun toEntity(domain: Member): MemberEntity {
        return MemberEntity(
            loginId = domain.loginId.value,
            password = domain.password.value,
            name = domain.name.value,
            birthDate = domain.birthDate.value,
            email = domain.email.value,
        )
    }

    fun update(entity: MemberEntity, domain: Member) {
        entity.changePassword(domain.password.value)
    }
}
