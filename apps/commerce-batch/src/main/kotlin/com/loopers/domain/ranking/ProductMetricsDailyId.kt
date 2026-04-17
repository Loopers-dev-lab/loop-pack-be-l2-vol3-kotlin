package com.loopers.domain.ranking

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.time.LocalDate

@Embeddable
data class ProductMetricsDailyId(

    @Column(name = "product_id", nullable = false)
    val productId: Long = 0L,

    @Column(name = "metric_date", nullable = false)
    val metricDate: LocalDate = LocalDate.MIN,
) : Serializable
