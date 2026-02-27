package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandInfo
import com.loopers.application.brand.CreateBrandCriteria
import com.loopers.application.brand.UpdateBrandCriteria
import io.swagger.v3.oas.annotations.media.Schema
import java.time.ZonedDateTime

class BrandV1Dto {

    @Schema(description = "브랜드 응답 (대고객)")
    data class BrandResponse(
        @Schema(description = "브랜드 ID", example = "1")
        val id: Long,
        @Schema(description = "브랜드명", example = "나이키")
        val name: String,
        @Schema(description = "브랜드 설명", example = "스포츠 브랜드")
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

    @Schema(description = "브랜드 응답 (어드민)")
    data class BrandAdminResponse(
        @Schema(description = "브랜드 ID", example = "1")
        val id: Long,
        @Schema(description = "브랜드명", example = "나이키")
        val name: String,
        @Schema(description = "브랜드 설명", example = "스포츠 브랜드")
        val description: String?,
        @Schema(description = "생성일시")
        val createdAt: ZonedDateTime,
        @Schema(description = "수정일시")
        val updatedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: BrandInfo): BrandAdminResponse {
                return BrandAdminResponse(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
            }
        }
    }

    @Schema(description = "브랜드 등록 요청")
    data class CreateRequest(
        @Schema(description = "브랜드명", example = "나이키")
        val name: String,
        @Schema(description = "브랜드 설명", example = "스포츠 브랜드")
        val description: String?,
    ) {
        fun toCriteria(): CreateBrandCriteria {
            return CreateBrandCriteria(
                name = name,
                description = description,
            )
        }
    }

    @Schema(description = "브랜드 수정 요청")
    data class UpdateRequest(
        @Schema(description = "브랜드명", example = "아디다스")
        val name: String,
        @Schema(description = "브랜드 설명", example = "독일 스포츠 브랜드")
        val description: String?,
    ) {
        fun toCriteria(): UpdateBrandCriteria {
            return UpdateBrandCriteria(
                name = name,
                description = description,
            )
        }
    }
}
