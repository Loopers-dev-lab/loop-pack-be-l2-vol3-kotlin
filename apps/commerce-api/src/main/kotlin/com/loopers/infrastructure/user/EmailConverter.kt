package com.loopers.infrastructure.user

import com.loopers.domain.user.Email
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class EmailConverter : AttributeConverter<Email, String> {
    override fun convertToDatabaseColumn(attribute: Email?): String? = attribute?.value
    override fun convertToEntityAttribute(dbData: String?): Email? = dbData?.let { Email.of(it) }
}
