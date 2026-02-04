package com.loopers.infrastructure.member

import com.loopers.domain.member.Member
import com.loopers.domain.member.MemberRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class MemberRepositoryImpl(
    private val memberJpaRepository: MemberJpaRepository,
) : MemberRepository {

    override fun save(member: Member): Member {
        return memberJpaRepository.save(member)
    }

    override fun findById(id: Long): Member? {
        return memberJpaRepository.findByIdOrNull(id)
    }

    override fun findByLoginId(loginId: String): Member? {
        return memberJpaRepository.findByLoginId(loginId)
    }

    override fun existsByLoginId(loginId: String): Boolean {
        return memberJpaRepository.existsByLoginId(loginId)
    }
}
