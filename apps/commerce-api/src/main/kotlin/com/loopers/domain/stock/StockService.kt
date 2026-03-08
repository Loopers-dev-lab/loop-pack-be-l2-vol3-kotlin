package com.loopers.domain.stock

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class StockService(
    private val stockRepository: StockRepository,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun decreaseAllStocks(items: List<StockDecreaseCommand>) {
        // 상품별 정렬하여 데드락 방지
        val sortedItems = items.sortedBy { it.productId }

        sortedItems.forEach { item ->
            val stock = stockRepository.findStockWithLock(item.productId)
                ?: throw CoreException(ErrorType.NOT_FOUND, "상품 재고가 존재하지 않습니다: ${item.productId}")
            stock.minusStock(item.quantity)
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun increaseAllStocks(items: List<StockIncreaseCommand>) {
        // 상품별 정렬하여 데드락 방지
        val sortedItems = items.sortedBy { it.productId }

        sortedItems.forEach { item ->
            val stock = stockRepository.findStockWithLock(item.productId)
                ?: throw CoreException(ErrorType.NOT_FOUND, "상품 재고가 존재하지 않습니다: ${item.productId}")
            stock.plusStock(item.quantity)
        }
    }

    fun getStock(productId: Long): Stock? = stockRepository.findByProductId(productId)
}

data class StockDecreaseCommand(
    val productId: Long,
    val quantity: Int,
)

data class StockIncreaseCommand(
    val productId: Long,
    val quantity: Int,
)
