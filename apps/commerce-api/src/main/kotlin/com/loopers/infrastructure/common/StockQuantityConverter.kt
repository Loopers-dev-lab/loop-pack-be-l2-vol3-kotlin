package com.loopers.infrastructure.common

import com.loopers.domain.common.StockQuantity
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class StockQuantityConverter : AttributeConverter<StockQuantity, Int> {
    override fun convertToDatabaseColumn(attribute: StockQuantity?): Int? = attribute?.value
    override fun convertToEntityAttribute(dbData: Int?): StockQuantity? = dbData?.let { StockQuantity.of(it) }
}
