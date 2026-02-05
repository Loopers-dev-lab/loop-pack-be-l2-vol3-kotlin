package com.loopers.infrastructure.member

import com.loopers.domain.member.MemberModel
import com.loopers.domain.member.vo.LoginId
import org.springframework.data.jpa.repository.JpaRepository

interface MemberJpaRepository : JpaRepository<MemberModel, Long> {
    fun findByLoginId(loginId: LoginId): MemberModel?
    fun existsByLoginId(loginId: LoginId): Boolean
}
