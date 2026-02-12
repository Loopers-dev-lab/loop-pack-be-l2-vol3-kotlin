package com.loopers.domain.user

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class BirthDateConverter : AttributeConverter<BirthDate, String> {
    override fun convertToDatabaseColumn(attribute: BirthDate): String {
        return attribute.value
    }

    override fun convertToEntityAttribute(dbData: String?): BirthDate? {
        return dbData?.let { BirthDate(it) }
    }
}
