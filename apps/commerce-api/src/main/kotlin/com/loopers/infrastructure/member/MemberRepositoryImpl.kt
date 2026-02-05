package com.loopers.infrastructure.member

import com.loopers.domain.member.MemberModel
import com.loopers.domain.member.MemberRepository
import com.loopers.domain.member.vo.LoginId
import org.springframework.stereotype.Component

@Component
class MemberRepositoryImpl(
    private val memberJpaRepository: MemberJpaRepository,
) : MemberRepository {

    override fun save(member: MemberModel): MemberModel {
        return memberJpaRepository.save(member)
    }

    override fun findByLoginId(loginId: LoginId): MemberModel? {
        return memberJpaRepository.findByLoginId(loginId)
    }

    override fun existsByLoginId(loginId: LoginId): Boolean {
        return memberJpaRepository.existsByLoginId(loginId)
    }
}
