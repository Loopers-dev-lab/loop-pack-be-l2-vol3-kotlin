package com.loopers.infrastructure.coupon

import com.loopers.domain.BaseEntity
import com.loopers.domain.coupon.model.CouponIssueRequest
import com.loopers.domain.coupon.repository.CouponIssueRequestRepository
import com.loopers.domain.withBaseFields
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Entity
@Table(
    name = "coupon_issue_requests",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_coupon_issue_requests_request_id", columnNames = ["request_id"]),
    ],
)
class CouponIssueRequestEntity(
    @Column(name = "request_id", nullable = false)
    var requestId: String,
    @Column(name = "coupon_id", nullable = false)
    var couponId: Long,
    @Column(name = "user_id", nullable = false)
    var userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: CouponIssueRequest.CouponIssueStatus,
) : BaseEntity() {

    companion object {
        fun fromDomain(request: CouponIssueRequest): CouponIssueRequestEntity {
            return CouponIssueRequestEntity(
                requestId = request.requestId,
                couponId = request.couponId,
                userId = request.userId,
                status = request.status,
            ).withBaseFields(id = request.id)
        }
    }

    fun toDomain(): CouponIssueRequest = CouponIssueRequest(
        id = id,
        requestId = requestId,
        couponId = couponId,
        userId = userId,
        status = status,
    )
}

interface CouponIssueRequestJpaRepository : JpaRepository<CouponIssueRequestEntity, Long> {
    fun findByRequestId(requestId: String): CouponIssueRequestEntity?
}

@Repository
class CouponIssueRequestRepositoryImpl(
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
) : CouponIssueRequestRepository {

    override fun findByRequestId(requestId: String): CouponIssueRequest? =
        couponIssueRequestJpaRepository.findByRequestId(requestId)?.toDomain()

    override fun save(request: CouponIssueRequest): CouponIssueRequest =
        couponIssueRequestJpaRepository.save(CouponIssueRequestEntity.fromDomain(request)).toDomain()
}
