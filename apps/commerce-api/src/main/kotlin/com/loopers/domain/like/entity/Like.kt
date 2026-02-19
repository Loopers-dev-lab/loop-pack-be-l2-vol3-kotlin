package com.loopers.domain.like.entity

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "likes",
    uniqueConstraints = [UniqueConstraint(columnNames = ["ref_user_id", "ref_product_id"])],
)
class Like(
    refUserId: Long,
    refProductId: Long,
) {

    init {
        if (refUserId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "refUserId는 양수여야 합니다.")
        if (refProductId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "refProductId는 양수여야 합니다.")
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "ref_user_id", nullable = false)
    var refUserId: Long = refUserId
        protected set

    @Column(name = "ref_product_id", nullable = false)
    var refProductId: Long = refProductId
        protected set
}
