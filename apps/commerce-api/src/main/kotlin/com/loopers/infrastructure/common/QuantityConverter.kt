package com.loopers.infrastructure.common

import com.loopers.domain.common.Quantity
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class QuantityConverter : AttributeConverter<Quantity, Int> {
    override fun convertToDatabaseColumn(attribute: Quantity?): Int? = attribute?.value
    override fun convertToEntityAttribute(dbData: Int?): Quantity? = dbData?.let { Quantity.of(it) }
}
