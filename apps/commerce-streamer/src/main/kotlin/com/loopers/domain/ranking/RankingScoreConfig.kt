package com.loopers.domain.ranking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "ranking_score_config")
class RankingScoreConfig(
    configKey: String,
    configValue: Double,
    description: String?,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "config_key", nullable = false, unique = true, length = 50)
    val configKey: String = configKey

    @Column(name = "config_value", nullable = false)
    var configValue: Double = configValue
        private set

    @Column(name = "description", length = 200)
    var description: String? = description
        private set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        private set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: ZonedDateTime
        private set

    @PrePersist
    private fun prePersist() {
        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    private fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }
}
