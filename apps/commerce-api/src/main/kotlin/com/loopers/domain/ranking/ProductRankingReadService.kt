package com.loopers.domain.ranking

import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId

@Service
class ProductRankingReadService(
    private val productRankingRepository: ProductRankingRepository,
) {

    companion object {
        private val KST_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }

    fun getRankedProducts(processingDate: LocalDate?, page: Int, size: Int): List<ProductRankingReadModel> {
        return productRankingRepository.getRankedProducts(resolveProcessingDate(processingDate), page, size)
    }

    fun getRank(processingDate: LocalDate?, productId: Long): Long? {
        return productRankingRepository.getRank(resolveProcessingDate(processingDate), productId)
    }

    fun count(processingDate: LocalDate?): Long {
        return productRankingRepository.count(resolveProcessingDate(processingDate))
    }

    private fun resolveProcessingDate(processingDate: LocalDate?): LocalDate {
        return processingDate ?: LocalDate.now(KST_ZONE_ID)
    }
}
