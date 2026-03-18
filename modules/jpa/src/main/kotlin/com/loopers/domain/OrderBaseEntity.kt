package com.loopers.domain

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import java.time.ZonedDateTime

/**
 * Order 전용 BaseEntity.
 * orders 테이블에는 updated_at, deleted_at이 없고
 * order_items 테이블에는 created_at도 없으므로,
 * id + createdAt만 제공한다.
 *
 * Order.id는 SnowflakeIdGenerator에서 채번하여 직접 할당한다.
 * OrderItem.id는 @GeneratedValue를 별도로 선언한다.
 *
 * @property id 엔티티 ID
 * @property createdAt 생성 시점
 */
@MappedSuperclass
abstract class OrderBaseEntity {
    @Id
    var id: Long = 0
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        protected set

    @PrePersist
    private fun prePersist() {
        createdAt = ZonedDateTime.now()
    }
}
