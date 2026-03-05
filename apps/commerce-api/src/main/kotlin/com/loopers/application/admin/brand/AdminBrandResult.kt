package com.loopers.application.admin.brand

import com.loopers.domain.brand.Brand

class AdminBrandResult {
    data class Register(
        val id: Long,
        val name: String,
        val status: String,
    ) {
        companion object {
            fun from(brand: Brand): Register =
                Register(id = brand.id!!, name = brand.name.value, status = brand.status.name)
        }
    }

    data class Update(
        val id: Long,
        val name: String,
        val status: String,
    ) {
        companion object {
            fun from(brand: Brand): Update =
                Update(id = brand.id!!, name = brand.name.value, status = brand.status.name)
        }
    }

    data class Detail(
        val id: Long,
        val name: String,
        val status: String,
    ) {
        companion object {
            fun from(brand: Brand): Detail =
                Detail(id = brand.id!!, name = brand.name.value, status = brand.status.name)
        }
    }

    data class Summary(
        val id: Long,
        val name: String,
        val status: String,
    ) {
        companion object {
            fun from(brand: Brand): Summary =
                Summary(id = brand.id!!, name = brand.name.value, status = brand.status.name)
        }
    }
}
