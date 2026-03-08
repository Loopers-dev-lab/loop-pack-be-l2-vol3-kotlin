package com.loopers.domain.order

import java.math.BigDecimal
import java.math.RoundingMode

object DiscountDistributer {
    /**
     * 총 할인액을 여러 OrderItem에 비율에 따라 분배합니다.
     * 마지막 항목에 나머지를 할당하여 정확한 합계를 보장합니다.
     *
     * @param items 할인을 적용할 OrderItem 목록
     * @param totalDiscount 전체 할인액
     * @param totalAmount 할인 전 총 주문액
     */
    fun distributeDiscount(
        items: List<OrderItem>,
        totalDiscount: BigDecimal,
        totalAmount: BigDecimal,
    ) {
        if (items.isEmpty() || totalDiscount <= BigDecimal.ZERO) {
            return
        }

        if (totalAmount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("총 주문액은 0보다 커야 합니다")
        }

        var remainingDiscount = totalDiscount
        val itemsWithoutLast = items.dropLast(1)

        // 마지막 항목을 제외한 나머지에 비율 적용
        for (i in itemsWithoutLast.indices) {
            val item = itemsWithoutLast[i]
            val itemAmount = item.getItemAmount() // price * quantity
            val ratio = itemAmount.divide(totalAmount, 10, RoundingMode.HALF_UP)
            val discountForItem = (totalDiscount * ratio).setScale(0, RoundingMode.DOWN)

            item.applyDiscountAmount(discountForItem)
            remainingDiscount = remainingDiscount.subtract(discountForItem)
        }

        // 마지막 항목에 나머지 할당
        if (items.isNotEmpty()) {
            items.last().applyDiscountAmount(remainingDiscount)
        }
    }
}
