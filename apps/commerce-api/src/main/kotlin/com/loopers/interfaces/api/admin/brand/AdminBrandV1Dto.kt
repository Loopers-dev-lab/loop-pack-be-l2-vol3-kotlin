package com.loopers.interfaces.api.admin.brand

import com.loopers.application.brand.BrandInfo
import jakarta.validation.constraints.NotBlank
import java.time.ZonedDateTime

class AdminBrandV1Dto {
    data class CreateRequest(
        @field:NotBlank(message = "브랜드명은 필수입니다.")
        val name: String,
        @field:NotBlank(message = "브랜드 설명은 필수입니다.")
        val description: String,
        @field:NotBlank(message = "이미지 URL은 필수입니다.")
        val imageUrl: String,
    )

    data class UpdateRequest(
        @field:NotBlank(message = "브랜드명은 필수입니다.")
        val name: String,
        @field:NotBlank(message = "브랜드 설명은 필수입니다.")
        val description: String,
        @field:NotBlank(message = "이미지 URL은 필수입니다.")
        val imageUrl: String,
    )

    data class BrandResponse(
        val id: Long,
        val name: String,
        val description: String,
        val imageUrl: String,
        val status: String,
        val createdAt: ZonedDateTime?,
        val updatedAt: ZonedDateTime?,
    ) {
        companion object {
            fun from(info: BrandInfo): BrandResponse {
                return BrandResponse(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                    imageUrl = info.imageUrl,
                    status = info.status,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
            }
        }
    }
}
