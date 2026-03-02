package com.loopers.infrastructure.like

import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.like.model.Like
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(name = "likes", uniqueConstraints = [UniqueConstraint(columnNames = ["ref_user_id", "ref_product_id"])])
class LikeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "ref_user_id", nullable = false)
    var refUserId: Long,
    @Column(name = "ref_product_id", nullable = false)
    var refProductId: Long,
) {

    companion object {
        fun fromDomain(like: Like): LikeEntity = LikeEntity(
            id = like.id,
            refUserId = like.refUserId.value,
            refProductId = like.refProductId.value,
        )
    }

    fun toDomain(): Like = Like(
        id = id,
        refUserId = UserId(refUserId),
        refProductId = ProductId(refProductId),
    )
}
