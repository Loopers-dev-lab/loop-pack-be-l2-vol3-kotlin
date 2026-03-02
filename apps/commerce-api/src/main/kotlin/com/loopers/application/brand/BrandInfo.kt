package com.loopers.application.brand

import com.loopers.domain.brand.BrandModel
import java.time.ZonedDateTime

data class BrandInfo(
    val id: Long,
    val name: String,
    val logoImageUrl: String,
    val description: String,
    val zipCode: String,
    val roadAddress: String,
    val detailAddress: String,
    val email: String,
    val phoneNumber: String,
    val businessNumber: String,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    companion object {
        fun from(model: BrandModel): BrandInfo = BrandInfo(
            id = model.id,
            name = model.name.value,
            logoImageUrl = model.logoImageUrl.value,
            description = model.description.value,
            zipCode = model.zipCode,
            roadAddress = model.roadAddress,
            detailAddress = model.detailAddress,
            email = model.email.value,
            phoneNumber = model.phoneNumber.value,
            businessNumber = model.businessNumber.value,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
        )
    }
}
