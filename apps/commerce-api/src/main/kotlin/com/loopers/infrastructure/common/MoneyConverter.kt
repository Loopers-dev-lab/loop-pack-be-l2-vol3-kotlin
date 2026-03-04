package com.loopers.infrastructure.common

import com.loopers.domain.common.Money
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class MoneyConverter : AttributeConverter<Money, Long> {
    override fun convertToDatabaseColumn(attribute: Money?): Long? = attribute?.value
    override fun convertToEntityAttribute(dbData: Long?): Money? = dbData?.let { Money.of(it) }
}
