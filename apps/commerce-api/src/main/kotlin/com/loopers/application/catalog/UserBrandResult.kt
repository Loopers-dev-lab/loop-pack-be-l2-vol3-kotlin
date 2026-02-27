package com.loopers.application.catalog

import com.loopers.domain.catalog.BrandInfo

data class UserGetBrandResult(
    val id: Long,
    val name: String
) {
    companion object {
        fun from(info: BrandInfo): UserGetBrandResult {
            return UserGetBrandResult(
                id = info.id,
                name = info.name,
            )
        }
    }
}
