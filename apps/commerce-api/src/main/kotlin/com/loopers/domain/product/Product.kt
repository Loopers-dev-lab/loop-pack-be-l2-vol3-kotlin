package com.loopers.domain.product

import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Product private constructor(
    val id: Long?,
    val name: String,
    val regularPrice: Money,
    val sellingPrice: Money,
    val brandId: Long,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val likeCount: Int,
    val status: Status,
) {
    init {
        if (name.isBlank() || name.length > MAX_NAME_LENGTH) {
            throw CoreException(ErrorType.PRODUCT_INVALID_NAME)
        }
        if (sellingPrice.isGreaterThan(regularPrice)) {
            throw CoreException(ErrorType.PRODUCT_INVALID_PRICE)
        }
    }

    enum class Status {
        ACTIVE,
        INACTIVE,
    }

    enum class SortType {
        LATEST,
        PRICE_ASC,
        LIKES_DESC,
    }

    fun changeInfo(
        name: String,
        regularPrice: Money,
        sellingPrice: Money,
        imageUrl: String?,
        thumbnailUrl: String?,
    ): Product = copy(
        name = name,
        regularPrice = regularPrice,
        sellingPrice = sellingPrice,
        imageUrl = imageUrl,
        thumbnailUrl = thumbnailUrl,
    )

    fun activate(): Product = copy(status = Status.ACTIVE)

    fun deactivate(): Product = copy(status = Status.INACTIVE)

    private fun copy(
        id: Long? = this.id,
        name: String = this.name,
        regularPrice: Money = this.regularPrice,
        sellingPrice: Money = this.sellingPrice,
        brandId: Long = this.brandId,
        imageUrl: String? = this.imageUrl,
        thumbnailUrl: String? = this.thumbnailUrl,
        likeCount: Int = this.likeCount,
        status: Status = this.status,
    ): Product = Product(id, name, regularPrice, sellingPrice, brandId, imageUrl, thumbnailUrl, likeCount, status)

    companion object {
        private const val MAX_NAME_LENGTH = 100

        fun register(
            name: String,
            regularPrice: Money,
            sellingPrice: Money,
            brandId: Long,
            imageUrl: String? = null,
            thumbnailUrl: String? = null,
        ): Product = Product(
            id = null,
            name = name,
            regularPrice = regularPrice,
            sellingPrice = sellingPrice,
            brandId = brandId,
            imageUrl = imageUrl,
            thumbnailUrl = thumbnailUrl,
            likeCount = 0,
            status = Status.INACTIVE,
        )

        fun retrieve(
            id: Long,
            name: String,
            regularPrice: Money,
            sellingPrice: Money,
            brandId: Long,
            imageUrl: String?,
            thumbnailUrl: String?,
            likeCount: Int,
            status: Status,
        ): Product = Product(
            id = id,
            name = name,
            regularPrice = regularPrice,
            sellingPrice = sellingPrice,
            brandId = brandId,
            imageUrl = imageUrl,
            thumbnailUrl = thumbnailUrl,
            likeCount = likeCount,
            status = status,
        )
    }
}
