package com.loopers.infrastructure.member

import com.loopers.domain.member.MemberModel
import com.loopers.domain.member.MemberRepository
import org.springframework.stereotype.Component

@Component
class MemberRepositoryImpl(
    private val memberJpaRepository: MemberJpaRepository,
) : MemberRepository {
    override fun save(member: MemberModel): MemberModel {
        if (member.id == 0L) {
            return memberJpaRepository.save(MemberJpaModel.from(member)).toModel()
        }
        val existing = memberJpaRepository.findByLoginId(member.loginId)
            ?: throw IllegalStateException("Member not found: ${member.loginId}")
        existing.updateFrom(member)
        return existing.toModel()
    }

    override fun findByLoginId(loginId: String): MemberModel? {
        return memberJpaRepository.findByLoginId(loginId)?.toModel()
    }

    override fun existsByLoginId(loginId: String): Boolean {
        return memberJpaRepository.existsByLoginId(loginId)
    }
}
