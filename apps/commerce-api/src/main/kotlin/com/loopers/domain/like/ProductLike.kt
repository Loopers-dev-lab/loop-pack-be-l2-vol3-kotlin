package com.loopers.domain.like

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "product_likes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "product_id"])],
)
class ProductLike(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,
) : BaseEntity()
