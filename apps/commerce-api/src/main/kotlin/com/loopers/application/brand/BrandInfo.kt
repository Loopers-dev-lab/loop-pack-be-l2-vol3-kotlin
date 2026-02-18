package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import java.time.ZonedDateTime

data class BrandInfo(
    val id: Long,
    val name: String,
    val description: String?,
    val logoUrl: String?,
    val status: String,
    val deletedAt: ZonedDateTime?,
) {
    companion object {
        fun from(brand: Brand): BrandInfo {
            val id = requireNotNull(brand.persistenceId) {
                "Brand.persistenceId가 null입니다. 저장된 Brand만 매핑 가능합니다."
            }
            return BrandInfo(
                id = id,
                name = brand.name.value,
                description = brand.description,
                logoUrl = brand.logoUrl,
                status = brand.status.name,
                deletedAt = brand.deletedAt,
            )
        }
    }
}
