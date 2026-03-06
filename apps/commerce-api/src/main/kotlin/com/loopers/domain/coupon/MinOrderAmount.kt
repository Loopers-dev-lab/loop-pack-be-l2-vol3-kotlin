package com.loopers.domain.coupon

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@JvmInline
value class MinOrderAmount(val value: Long) {
    init {
        require(value > 0) { "최소 주문 금액은 0보다 커야 합니다." }
    }
}

@Converter(autoApply = true)
class MinOrderAmountConverter : AttributeConverter<MinOrderAmount?, Long?> {
    override fun convertToDatabaseColumn(attribute: MinOrderAmount?): Long? = attribute?.value
    override fun convertToEntityAttribute(dbData: Long?): MinOrderAmount? = dbData?.let { MinOrderAmount(it) }
}
