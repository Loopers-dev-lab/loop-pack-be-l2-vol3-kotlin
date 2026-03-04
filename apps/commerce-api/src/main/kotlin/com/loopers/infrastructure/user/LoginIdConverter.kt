package com.loopers.infrastructure.user

import com.loopers.domain.user.LoginId
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class LoginIdConverter : AttributeConverter<LoginId, String> {
    override fun convertToDatabaseColumn(attribute: LoginId?): String? = attribute?.value
    override fun convertToEntityAttribute(dbData: String?): LoginId? = dbData?.let { LoginId.of(it) }
}
