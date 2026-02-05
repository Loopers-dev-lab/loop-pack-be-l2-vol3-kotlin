package com.loopers.domain.member

interface MemberRepository {
    fun save(member: MemberModel): MemberModel
    fun findByLoginId(loginId: String): MemberModel?
    fun existsByLoginId(loginId: String): Boolean
}
