package com.loopers.infrastructure.common

import com.loopers.domain.common.LikeCount
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class LikeCountConverter : AttributeConverter<LikeCount, Int> {
    override fun convertToDatabaseColumn(attribute: LikeCount?): Int? = attribute?.value
    override fun convertToEntityAttribute(dbData: Int?): LikeCount? = dbData?.let { LikeCount.of(it) }
}
