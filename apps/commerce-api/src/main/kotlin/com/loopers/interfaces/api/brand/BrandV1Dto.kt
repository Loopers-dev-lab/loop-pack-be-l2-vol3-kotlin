package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandUseCase
import com.loopers.application.brand.BrandInfo
import jakarta.validation.constraints.NotBlank

class BrandV1Dto {

    data class RegisterRequest(@field:NotBlank val name: String) {
        fun toCommand() = BrandUseCase.RegisterCommand(name = name)
    }

    data class ChangeNameRequest(@field:NotBlank val name: String) {
        fun toCommand() = BrandUseCase.ChangeNameCommand(name = name)
    }

    data class DetailResponse(
        val id: Long,
        val name: String,
        val status: String,
    ) {
        companion object {
            fun from(info: BrandInfo.Detail) = DetailResponse(
                id = info.id,
                name = info.name,
                status = info.status,
            )
        }
    }

    data class MainResponse(
        val id: Long,
        val name: String,
    ) {
        companion object {
            fun from(info: BrandInfo.Main) = MainResponse(
                id = info.id,
                name = info.name,
            )
        }
    }
}
