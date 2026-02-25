package com.loopers.infrastructure.like

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

@Entity
@Table(
    name = "likes",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_likes_member_product", columnNames = ["member_id", "product_id"]),
    ],
    indexes = [
        Index(name = "idx_likes_member_id", columnList = "member_id"),
    ],
)
class LikeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: ZonedDateTime? = null,
) {
    @PrePersist
    fun prePersist() {
        createdAt = ZonedDateTime.now()
    }
}
