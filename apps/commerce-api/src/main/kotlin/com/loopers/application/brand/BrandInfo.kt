package com.loopers.application.brand

import com.loopers.domain.brand.Brand

class BrandInfo {

    data class Detail(
        val id: Long,
        val name: String,
        val status: String,
    ) {
        companion object {
            fun from(brand: Brand) = Detail(
                id = requireNotNull(brand.id) { "브랜드 저장 후 ID가 할당되지 않았습니다." },
                name = brand.name.value,
                status = brand.status.name,
            )
        }
    }

    data class Main(
        val id: Long,
        val name: String,
    ) {
        companion object {
            fun from(brand: Brand) = Main(
                id = requireNotNull(brand.id) { "브랜드 저장 후 ID가 할당되지 않았습니다." },
                name = brand.name.value,
            )
        }
    }
}
