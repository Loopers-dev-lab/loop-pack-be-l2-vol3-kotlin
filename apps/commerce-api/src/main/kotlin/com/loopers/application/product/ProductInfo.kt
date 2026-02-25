package com.loopers.application.product

import com.loopers.domain.product.Product

class ProductInfo {

    data class Detail(
        val id: Long,
        val brandId: Long,
        val brandName: String,
        val name: String,
        val price: Long,
        val description: String,
        val stock: Int,
        val status: String,
        val likeCount: Long,
    ) {
        companion object {
            fun from(product: Product, brandName: String, likeCount: Long) = Detail(
                id = requireNotNull(product.id) { "상품 저장 후 ID가 할당되지 않았습니다." },
                brandId = product.brandId,
                brandName = brandName,
                name = product.name.value,
                price = product.price.value,
                description = product.description.value,
                stock = product.stock.value,
                status = product.status.name,
                likeCount = likeCount,
            )
        }
    }

    data class Main(
        val id: Long,
        val brandId: Long,
        val brandName: String,
        val name: String,
        val price: Long,
        val stock: Int,
        val status: String,
        val likeCount: Long,
    ) {
        companion object {
            fun from(product: Product, brandName: String, likeCount: Long) = Main(
                id = requireNotNull(product.id) { "상품 저장 후 ID가 할당되지 않았습니다." },
                brandId = product.brandId,
                brandName = brandName,
                name = product.name.value,
                price = product.price.value,
                stock = product.stock.value,
                status = product.status.name,
                likeCount = likeCount,
            )
        }
    }
}
