package com.loopers.domain

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.ZonedDateTime

/**
 * 공통 영속성 필드(id, 생성일시, 수정일시)를 자동으로 관리한다.
 * deleted_at 등 삭제 관련 필드는 도메인마다 맥락이 다르므로 각 엔티티가 직접 정의한다.
 *
 * @property id 엔티티 ID
 * @property createdAt 생성 시점
 * @property updatedAt 수정 시점
 */
@MappedSuperclass
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        protected set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: ZonedDateTime
        protected set

    /**
     * 엔티티의 유효성을 검증한다.
     *
     * 이 메소드는 [PrePersist] 및 [PreUpdate] 시점에 호출된다.
     */
    open fun guard() = Unit

    @PrePersist
    private fun prePersist() {
        guard()

        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    private fun preUpdate() {
        guard()

        val now = ZonedDateTime.now()
        updatedAt = now
    }
}
