package com.loopers.domain.productlike

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

/**
 * 상품별 좋아요 수 집계 요약 테이블
 * product_likes 테이블의 데이터를 기반으로 likeCount를 관리합니다.
 *
 * @property productId 상품 ID (Unique)
 * @property likeCount 좋아요 수
 */
@Entity
@Table(
    name = "product_like_counts",
    uniqueConstraints = [UniqueConstraint(columnNames = ["product_id"])],
)
class ProductLikeCount private constructor(
    productId: Long,
) : BaseEntity() {

    @Column(name = "product_id", nullable = false, unique = true)
    val productId: Long = productId

    @Column(nullable = false)
    var likeCount: Long = 0
        protected set

    /**
     * 좋아요 수를 증가시킵니다.
     */
    fun increment() {
        this.likeCount++
        this.updatedAt = ZonedDateTime.now()
    }

    /**
     * 좋아요 수를 감소시킵니다 (0 미만으로 내려가지 않음).
     */
    fun decrement() {
        if (this.likeCount > 0) {
            this.likeCount--
            this.updatedAt = ZonedDateTime.now()
        }
    }

    companion object {
        fun create(productId: Long): ProductLikeCount = ProductLikeCount(productId).apply {
            createdAt = ZonedDateTime.now()
            updatedAt = ZonedDateTime.now()
        }
    }
}
