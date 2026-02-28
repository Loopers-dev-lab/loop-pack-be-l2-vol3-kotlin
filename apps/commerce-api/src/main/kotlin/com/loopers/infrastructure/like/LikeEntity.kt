package com.loopers.infrastructure.like

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

@Entity
@Table(
    name = "likes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "product_id"])],
)
class LikeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: ZonedDateTime,
)
