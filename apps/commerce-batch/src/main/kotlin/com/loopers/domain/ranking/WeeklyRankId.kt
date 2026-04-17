package com.loopers.domain.ranking

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.time.LocalDate

@Embeddable
data class WeeklyRankId(

    @Column(name = "product_id", nullable = false)
    val productId: Long = 0L,

    @Column(name = "week_end", nullable = false)
    val weekEnd: LocalDate = LocalDate.MIN,
) : Serializable
