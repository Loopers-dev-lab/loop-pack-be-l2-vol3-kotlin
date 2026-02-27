package com.loopers.domain.order

import com.loopers.domain.Money
import com.loopers.domain.product.Product
import com.loopers.config.jpa.MoneyConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import org.hibernate.annotations.Comment

@Embeddable
data class PriceSnapshot(
    @Comment("주문 당시 상품 원가")
    @Convert(converter = MoneyConverter::class)
    @Column(name = "snapshot_original_price", nullable = false)
    val originalPrice: Money,

    @Comment("할인액")
    @Convert(converter = MoneyConverter::class)
    @Column(name = "snapshot_discount_amount", nullable = false)
    val discountAmount: Money = Money.ZERO,

    @Comment("최종 결제가")
    @Convert(converter = MoneyConverter::class)
    @Column(name = "snapshot_final_price", nullable = false)
    val finalPrice: Money,
) {
    companion object {
        fun from(product: Product): PriceSnapshot {
            return PriceSnapshot(
                originalPrice = product.price,
                discountAmount = Money.ZERO,
                finalPrice = product.price,
            )
        }
    }
}
