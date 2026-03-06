package com.loopers.domain.productlike

import com.loopers.domain.BaseEntity
import com.loopers.domain.product.Product
import com.loopers.domain.user.User
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

/**
 * 사용자가 상품에 좋아요를 표현한 기록
 *
 * @property user 좋아요한 사용자
 * @property product 좋아요된 상품
 */
@Entity
@Table(name = "product_likes", uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "product_id"])])
class ProductLike private constructor(
    user: User,
    product: Product,
) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product = product
        protected set

    val userId: Long
        get() = user.id

    val productId: Long
        get() = product.id

    companion object {
        fun create(user: User, product: Product): ProductLike {
            return ProductLike(user = user, product = product).apply {
                createdAt = ZonedDateTime.now()
                updatedAt = ZonedDateTime.now()
            }
        }
    }
}
