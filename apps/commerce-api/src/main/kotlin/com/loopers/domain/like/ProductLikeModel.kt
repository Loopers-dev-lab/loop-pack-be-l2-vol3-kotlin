package com.loopers.domain.like

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "product_like",
    uniqueConstraints = [UniqueConstraint(name = "uk_product_like_member_product", columnNames = ["member_id", "product_id"])],
)
class ProductLikeModel(
    memberId: Long,
    productId: Long,
) : BaseEntity() {
    @Column(name = "member_id", nullable = false)
    var memberId: Long = memberId
        protected set

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set
}
