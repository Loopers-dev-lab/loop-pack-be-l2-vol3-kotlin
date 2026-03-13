package com.loopers.application.user.brand

import com.loopers.domain.brand.Brand

class UserBrandResult {
    data class Detail(
        val id: Long,
        val name: String,
    ) {
        companion object {
            fun from(brand: Brand): Detail = Detail(
                id = brand.id!!,
                name = brand.name.value,
            )
        }
    }
}
