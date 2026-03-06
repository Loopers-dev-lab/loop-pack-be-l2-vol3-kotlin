package com.loopers.domain.like

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "likes")
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

    init {
        if (userId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 양수여야 합니다.")
        }
        if (productId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품 ID는 양수여야 합니다.")
        }
    }
}
