package com.loopers.domain.user

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class LoginIdConverter : AttributeConverter<LoginId, String> {
    override fun convertToDatabaseColumn(attribute: LoginId?): String? {
        return attribute?.value
    }

    override fun convertToEntityAttribute(dbData: String?): LoginId? {
        return dbData?.let { LoginId(it) }
    }
}
