package com.loopers.domain.like

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "tb_like",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "product_id"])],
)
class LikeModel(
    userId: Long,
    productId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @Column(name = "product_id", nullable = false)
    val productId: Long = productId
}
