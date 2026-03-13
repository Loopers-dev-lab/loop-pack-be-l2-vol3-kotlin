package com.loopers.interfaces.api.user.brand

import com.loopers.application.user.brand.UserBrandResult

class UserBrandV1Response {
    data class Detail(
        val id: Long,
        val name: String,
    ) {
        companion object {
            fun from(result: UserBrandResult.Detail): Detail = Detail(
                id = result.id,
                name = result.name,
            )
        }
    }
}
