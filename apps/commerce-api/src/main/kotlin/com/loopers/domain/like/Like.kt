package com.loopers.domain.like

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "likes",
    indexes = [
        Index(name = "idx_likes_user_id", columnList = "user_id"),
    ],
    uniqueConstraints = [
        jakarta.persistence.UniqueConstraint(
            name = "uk_likes_user_product",
            columnNames = ["user_id", "product_id"],
        ),
    ],
)
class Like(
    userId: Long,
    productId: Long,
) : BaseEntity() {

    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @Column(name = "product_id", nullable = false)
    val productId: Long = productId

    init {
        require(userId > 0) { throw CoreException(ErrorType.BAD_REQUEST, "유저 ID는 1 이상이어야 합니다.") }
        require(productId > 0) { throw CoreException(ErrorType.BAD_REQUEST, "상품 ID는 1 이상이어야 합니다.") }
    }

    fun isDeleted(): Boolean = deletedAt != null
}
