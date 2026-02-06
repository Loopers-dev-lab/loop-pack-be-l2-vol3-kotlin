package com.loopers.domain.member

interface MemberRepository {
    fun existsByLoginId(loginId: String): Boolean
    fun save(member: MemberModel): MemberModel
    fun findByLoginId(loginId: String): MemberModel?
}
