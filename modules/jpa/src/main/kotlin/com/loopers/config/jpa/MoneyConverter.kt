package com.loopers.config.jpa

import com.loopers.domain.Money
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class MoneyConverter : AttributeConverter<Money, Long> {

    override fun convertToDatabaseColumn(money: Money?): Long? = money?.amount

    override fun convertToEntityAttribute(amount: Long?): Money? = amount?.let { Money(it) }
}
