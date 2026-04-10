package com.loopers.domain.ranking

import java.time.LocalDate

interface ViewDedupOperations {
    fun isDuplicate(productId: Long, loginId: String?, clientIp: String?, date: LocalDate): Boolean
    fun markViewed(productId: Long, loginId: String?, clientIp: String?, date: LocalDate)
}
