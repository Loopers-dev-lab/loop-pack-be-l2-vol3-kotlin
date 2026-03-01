package com.loopers.application.admin.brand

class AdminBrandCommand {
    data class Register(
        val name: String,
        val admin: String,
    )

    data class Update(
        val brandId: Long,
        val name: String,
        val status: String,
        val admin: String,
    )
}
