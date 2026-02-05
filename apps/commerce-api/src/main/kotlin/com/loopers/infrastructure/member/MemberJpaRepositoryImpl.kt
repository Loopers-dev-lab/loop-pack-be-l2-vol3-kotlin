package com.loopers.infrastructure.member

import com.loopers.domain.member.MemberModel
import com.loopers.domain.member.MemberRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class MemberJpaRepositoryImpl(
    private val memberJpaRepository: MemberJpaRepository,
) : MemberRepository {
    override fun find(id: Long): MemberModel? {
        return memberJpaRepository.findByIdOrNull(id)
    }

    override fun findByLoginId(loginId: String): MemberModel? {
        return memberJpaRepository.findByLoginId(loginId)
    }

    override fun save(member: MemberModel): MemberModel {
        return memberJpaRepository.save(member)
    }
}
