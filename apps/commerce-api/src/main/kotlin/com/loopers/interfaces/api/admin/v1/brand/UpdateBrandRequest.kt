package com.loopers.interfaces.api.admin.v1.brand

import com.loopers.application.brand.UpdateBrandCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateBrandRequest(
    @field:NotBlank(message = "브랜드명은 필수입니다.")
    @field:Size(max = 100, message = "브랜드명은 100자 이내여야 합니다.")
    val name: String,

    val description: String?,

    @field:Size(max = 500, message = "로고 URL은 500자 이내여야 합니다.")
    val logoUrl: String?,
) {
    fun toCommand() = UpdateBrandCommand(
        name = name,
        description = description,
        logoUrl = logoUrl,
    )
}
