package com.loopers.domain.member

import com.loopers.domain.member.vo.LoginId

interface MemberRepository {
    fun save(member: Member): Member
    fun findByLoginId(loginId: LoginId): Member?
    fun existsByLoginId(loginId: LoginId): Boolean
}
