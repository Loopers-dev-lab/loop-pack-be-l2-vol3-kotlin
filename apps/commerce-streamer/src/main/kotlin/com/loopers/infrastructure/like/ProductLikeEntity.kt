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

@Table(
    name = "product_like",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "product_id"])],
    indexes = [Index(columnList = "product_id")],
)
@Entity
class ProductLikeEntity(
    id: Long? = null,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "product_id", nullable = false)
    val productId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        private set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        private set

    init {
        this.id = id
    }

    @PrePersist
    private fun prePersist() {
        createdAt = ZonedDateTime.now()
    }
}
