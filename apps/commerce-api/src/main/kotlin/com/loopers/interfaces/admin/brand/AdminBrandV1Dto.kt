package com.loopers.interfaces.admin.brand

import jakarta.validation.constraints.NotBlank

class AdminBrandV1Dto {

    data class CreateBrandRequest(
        @field:NotBlank(message = "브랜드 명은 비어있을 수 없습니다.")
        val name: String,

        @field:NotBlank(message = "설명은 비어있을 수 없습니다.")
        val description: String,
    )

    data class UpdateBrandRequest(
        @field:NotBlank(message = "브랜드 명은 비어있을 수 없습니다.")
        val name: String,

        @field:NotBlank(message = "설명은 비어있을 수 없습니다.")
        val description: String,
    )
}
