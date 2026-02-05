package com.loopers.domain.user

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class NameConverter : AttributeConverter<Name, String> {
    override fun convertToDatabaseColumn(attribute: Name): String {
        return attribute.value
    }
    override fun convertToEntityAttribute(dbData: String?): Name? {
        return dbData?.let { Name(it) }
    }
}
