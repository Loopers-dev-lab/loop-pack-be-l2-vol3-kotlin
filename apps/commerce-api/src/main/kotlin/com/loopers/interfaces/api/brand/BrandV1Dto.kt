package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandFacade
import com.loopers.application.brand.BrandInfo

class BrandV1Dto {

    data class RegisterRequest(val name: String) {
        fun toCommand() = BrandFacade.RegisterCommand(name = name)
    }

    data class ChangeNameRequest(val name: String) {
        fun toCommand() = BrandFacade.ChangeNameCommand(name = name)
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
