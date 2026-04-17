package com.loopers.domain.ranking

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class MonthlyRankId(

    @Column(name = "product_id", nullable = false)
    val productId: Long = 0L,

    @Column(name = "yearmonth", nullable = false, length = 6)
    val yearMonth: String = "",
) : Serializable
