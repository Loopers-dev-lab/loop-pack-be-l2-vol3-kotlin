package com.loopers.interfaces.apiadmin.brand

import com.loopers.application.brand.BrandInfo
import java.time.ZonedDateTime

class AdminBrandDto {
    data class BrandResponse(
        val id: Long,
        val name: String,
        val description: String?,
    ) {
        companion object {
            fun from(info: BrandInfo): BrandResponse {
                return BrandResponse(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                )
            }
        }
    }

    data class CreateRequest(
        val name: String,
        val description: String?,
    )

    data class CreateResponse(
        val id: Long,
        val name: String,
        val description: String?,
    ) {
        companion object {
            fun from(info: BrandInfo): CreateResponse {
                return CreateResponse(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                )
            }
        }
    }

    data class UpdateRequest(
        val name: String,
        val description: String?,
    )

    data class DetailResponse(
        val id: Long,
        val name: String,
        val description: String?,
        val createdAt: ZonedDateTime,
        val updatedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: BrandInfo): DetailResponse {
                return DetailResponse(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
            }
        }
    }
}
