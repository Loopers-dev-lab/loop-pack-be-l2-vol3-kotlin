package com.loopers.infrastructure.member

import com.loopers.domain.member.Member
import com.loopers.domain.member.MemberRepository
import com.loopers.domain.member.vo.LoginId
import org.springframework.stereotype.Component

@Component
class MemberRepositoryImpl(
    private val memberJpaRepository: MemberJpaRepository,
    private val memberMapper: MemberMapper,
) : MemberRepository {

    override fun save(member: Member): Member {
        val entity = resolveEntity(member)
        val savedEntity = memberJpaRepository.save(entity)
        return memberMapper.toDomain(savedEntity)
    }

    override fun findByLoginId(loginId: LoginId): Member? {
        return memberJpaRepository.findByLoginId(loginId.value)?.let { memberMapper.toDomain(it) }
    }

    override fun existsByLoginId(loginId: LoginId): Boolean {
        return memberJpaRepository.existsByLoginId(loginId.value)
    }

    private fun resolveEntity(member: Member): MemberEntity {
        if (member.id == null) return memberMapper.toEntity(member)

        val entity = memberJpaRepository.getReferenceById(member.id)
        memberMapper.update(entity, member)
        return entity
    }
}
