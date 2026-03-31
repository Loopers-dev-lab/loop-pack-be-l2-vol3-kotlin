package com.loopers.infrastructure.fcfscoupon

import com.loopers.domain.fcfscoupon.FcfsCouponIssueRequestModel
import com.loopers.domain.fcfscoupon.FcfsCouponIssueStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "fcfs_coupon_issue_request")
class FcfsCouponIssueRequestJpaModel(
    templateId: Long,
    memberId: Long,
    status: FcfsCouponIssueStatus,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "template_id", nullable = false)
    var templateId: Long = templateId
        protected set

    @Column(name = "member_id", nullable = false)
    var memberId: Long = memberId
        protected set

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: FcfsCouponIssueStatus = status
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        protected set

    @Column(name = "processed_at")
    var processedAt: ZonedDateTime? = null
        protected set

    @PrePersist
    private fun prePersist() {
        createdAt = ZonedDateTime.now()
    }

    fun toModel(): FcfsCouponIssueRequestModel = FcfsCouponIssueRequestModel(
        id = id,
        templateId = templateId,
        memberId = memberId,
        status = status,
        createdAt = createdAt,
        processedAt = processedAt,
    )

    companion object {
        fun from(model: FcfsCouponIssueRequestModel): FcfsCouponIssueRequestJpaModel =
            FcfsCouponIssueRequestJpaModel(
                templateId = model.templateId,
                memberId = model.memberId,
                status = model.status,
            )
    }
}
