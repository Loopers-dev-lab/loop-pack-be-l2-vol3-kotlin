package com.loopers.infrastructure.member

import org.springframework.data.jpa.repository.JpaRepository

interface MemberJpaRepository : JpaRepository<MemberJpaModel, Long> {
    fun findByLoginId(loginId: String): MemberJpaModel?

    fun existsByLoginId(loginId: String): Boolean
}
