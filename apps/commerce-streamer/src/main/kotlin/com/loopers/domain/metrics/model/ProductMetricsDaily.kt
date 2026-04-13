package com.loopers.domain.metrics.model

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.LocalDate

class ProductMetricsDaily(
    val id: Long = 0,
    val productId: Long,
    val metricDate: LocalDate,
    viewCount: Long = 0,
    likeCount: Long = 0,
    salesCount: Long = 0,
) {

    var viewCount: Long = viewCount
        private set
    var likeCount: Long = likeCount
        private set
    var salesCount: Long = salesCount
        private set

    init {
        if (productId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "productId는 양수여야 합니다.")
    }

    fun incrementViewCount() {
        viewCount++
    }

    fun incrementLikeCount() {
        likeCount++
    }

    fun decrementLikeCount(): Boolean {
        check(likeCount >= 0) { "likeCount는 음수일 수 없다: $likeCount" }
        if (likeCount == 0L) return false
        likeCount--
        return true
    }

    fun incrementSalesCount(quantity: Long) {
        if (quantity <= 0) throw CoreException(ErrorType.BAD_REQUEST, "quantity는 양수여야 합니다.")
        salesCount += quantity
    }
}
