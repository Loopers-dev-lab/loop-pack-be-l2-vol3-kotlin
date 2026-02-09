package com.loopers.domain.member

/**
 * Member Repository 인터페이스
 * - 도메인 레이어에서 정의
 * - 구현체는 infrastructure 레이어에서 제공
 */
interface MemberRepository {

    fun save(member: Member): Member

    fun findById(id: Long): Member?

    fun findByLoginId(loginId: String): Member?

    fun existsByLoginId(loginId: String): Boolean
}
