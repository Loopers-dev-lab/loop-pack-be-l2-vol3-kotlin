package com.loopers.domain.brand

import java.time.ZonedDateTime

data class BrandModel(
    val id: Long = 0,
    val name: String,
    val description: String,
    val imageUrl: String,
    val status: BrandStatus = BrandStatus.ACTIVE,
    val createdAt: ZonedDateTime? = null,
    val updatedAt: ZonedDateTime? = null,
    val deletedAt: ZonedDateTime? = null,
) {
    init {
        require(name.isNotBlank()) { "브랜드 이름은 비어있을 수 없습니다." }
    }

    fun update(name: String, description: String, imageUrl: String): BrandModel =
        copy(name = name, description = description, imageUrl = imageUrl)

    fun delete(): BrandModel =
        copy(status = BrandStatus.DELETED, deletedAt = deletedAt ?: ZonedDateTime.now())

    fun isDeleted(): Boolean = status == BrandStatus.DELETED
}
