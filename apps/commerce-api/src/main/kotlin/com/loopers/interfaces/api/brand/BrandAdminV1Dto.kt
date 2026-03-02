package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandInfo
import java.time.ZonedDateTime

class BrandAdminV1Dto {

    data class CreateBrandRequest(
        val name: String,
        val logoImageUrl: String,
        val description: String,
        val zipCode: String,
        val roadAddress: String,
        val detailAddress: String,
        val email: String,
        val phoneNumber: String,
        val businessNumber: String,
    )

    data class UpdateBrandRequest(
        val name: String,
        val logoImageUrl: String,
        val description: String,
        val zipCode: String,
        val roadAddress: String,
        val detailAddress: String,
        val email: String,
        val phoneNumber: String,
        val businessNumber: String,
    )

    data class BrandResponse(
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
            fun from(info: BrandInfo) = BrandResponse(
                id = info.id,
                name = info.name,
                logoImageUrl = info.logoImageUrl,
                description = info.description,
                zipCode = info.zipCode,
                roadAddress = info.roadAddress,
                detailAddress = info.detailAddress,
                email = info.email,
                phoneNumber = info.phoneNumber,
                businessNumber = info.businessNumber,
                createdAt = info.createdAt,
                updatedAt = info.updatedAt,
            )
        }
    }
}
