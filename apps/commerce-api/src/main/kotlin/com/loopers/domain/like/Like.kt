package com.loopers.domain.like

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "likes",
    uniqueConstraints = [UniqueConstraint(name = "uk_likes_user_product", columnNames = ["user_id", "product_id"])],
)
class Like(
    userId: Long,
    productId: Long,
) : BaseEntity() {

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set
}
