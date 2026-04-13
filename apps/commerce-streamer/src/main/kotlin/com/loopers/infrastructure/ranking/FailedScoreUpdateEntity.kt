package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.model.FailedScoreUpdate
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(name = "failed_score_update")
class FailedScoreUpdateEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "event_id", nullable = false)
    val eventId: String,
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    @Column(name = "score", nullable = false)
    val score: Double,
    @Column(name = "ranking_date", nullable = false)
    val rankingDate: LocalDate,
    @Column(name = "created_at", nullable = false)
    val createdAt: ZonedDateTime,
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,
) {

    companion object {
        fun fromDomain(domain: FailedScoreUpdate): FailedScoreUpdateEntity {
            return FailedScoreUpdateEntity(
                id = domain.id,
                eventId = domain.eventId,
                productId = domain.productId,
                score = domain.score,
                rankingDate = domain.rankingDate,
                createdAt = domain.createdAt,
                retryCount = domain.retryCount,
            )
        }
    }

    fun toDomain(): FailedScoreUpdate = FailedScoreUpdate(
        id = id,
        eventId = eventId,
        productId = productId,
        score = score,
        rankingDate = rankingDate,
        createdAt = createdAt,
        retryCount = retryCount,
    )
}
