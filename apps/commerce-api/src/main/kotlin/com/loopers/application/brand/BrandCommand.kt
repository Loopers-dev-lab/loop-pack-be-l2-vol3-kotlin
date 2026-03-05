package com.loopers.application.brand

import com.loopers.domain.brand.vo.BrandName

class BrandCommand {
    data class Create(
        val name: String,
        val description: String,
        val imageUrl: String,
    ) {
        init {
            BrandName.of(name)
        }
    }

    data class Update(
        val name: String,
        val description: String,
        val imageUrl: String,
    ) {
        init {
            BrandName.of(name)
        }
    }
}
