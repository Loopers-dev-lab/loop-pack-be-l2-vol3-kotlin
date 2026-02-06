package com.loopers.infrastructure.member

import com.loopers.domain.member.Member
import org.springframework.data.jpa.repository.JpaRepository

interface MemberJpaRepository : JpaRepository<Member, Long> {

    fun findByLoginId(loginId: String): Member?

    fun existsByLoginId(loginId: String): Boolean
}
