package com.loopers.application.brand

class BrandCommand {

    data class Register(
        val name: String,
    )

    data class Update(
        val brandId: Long,
        val name: String,
    )
}
