package com.loopers.infrastructure.member

import org.springframework.data.jpa.repository.JpaRepository

interface MemberJpaRepository : JpaRepository<MemberEntity, Long> {
    fun findByLoginId(loginId: String): MemberEntity?
    fun existsByLoginId(loginId: String): Boolean
}
