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
        TODO("Not yet implemented")
    }

    override fun findByLoginId(loginId: String): MemberModel? {
        TODO("Not yet implemented")
    }

    override fun save(member: MemberModel): MemberModel {
        TODO("Not yet implemented")
    }
}
