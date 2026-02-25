package com.loopers.interfaces.api.admin.brand

import com.loopers.application.brand.BrandCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AdminBrandRegisterRequest(
    @field:NotBlank(message = "브랜드명은 필수입니다.")
    @field:Size(min = 1, max = 50, message = "브랜드명은 1~50자여야 합니다.")
    val name: String,
) {
    fun toCommand(): BrandCommand.Register {
        return BrandCommand.Register(name = name.trim())
    }
}

data class AdminBrandUpdateRequest(
    @field:NotBlank(message = "브랜드명은 필수입니다.")
    @field:Size(min = 1, max = 50, message = "브랜드명은 1~50자여야 합니다.")
    val name: String,
) {
    fun toCommand(brandId: Long): BrandCommand.Update {
        return BrandCommand.Update(brandId = brandId, name = name.trim())
    }
}
