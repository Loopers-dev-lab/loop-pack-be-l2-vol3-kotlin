package com.loopers.domain.brand

class BrandCommand {
    data class Create(
        val name: String,
        val description: String,
        val imageUrl: String,
    )

    data class Update(
        val name: String,
        val description: String,
        val imageUrl: String,
    )
}
