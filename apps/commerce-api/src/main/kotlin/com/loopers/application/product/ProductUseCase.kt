package com.loopers.application.product

import com.loopers.domain.brand.BrandReader
import com.loopers.domain.like.LikeReader
import com.loopers.domain.product.ProductChanger
import com.loopers.domain.product.ProductReader
import com.loopers.domain.product.ProductRegister
import com.loopers.domain.product.ProductRemover
import com.loopers.domain.product.ProductSortType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductUseCase(
    private val productRegister: ProductRegister,
    private val productReader: ProductReader,
    private val productChanger: ProductChanger,
    private val productRemover: ProductRemover,
    private val brandReader: BrandReader,
    private val likeReader: LikeReader,
) {

    @Transactional
    fun register(command: RegisterCommand): ProductInfo.Detail {
        val brand = brandReader.getActiveById(command.brandId)
        val product = productRegister.register(
            brandId = command.brandId,
            name = command.name,
            price = command.price,
            description = command.description,
            stock = command.stock,
        )
        return ProductInfo.Detail.from(product, brand, 0L)
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): ProductInfo.Detail {
        val product = productReader.getById(id)
        val brand = brandReader.getById(product.brandId)
        val likeCount = likeReader.countByProductId(product.id!!)
        return ProductInfo.Detail.from(product, brand, likeCount)
    }

    @Transactional(readOnly = true)
    fun getAll(sortType: ProductSortType, brandId: Long?): List<ProductInfo.Main> {
        val products = if (brandId != null) {
            productReader.getAllByBrandId(brandId)
        } else {
            productReader.getAll()
        }

        val brandIds = products.map { it.brandId }.distinct()
        val brandMap = brandReader.getAllByIds(brandIds).associateBy { it.id }

        val productIds = products.mapNotNull { it.id }
        val likeCountMap = likeReader.countByProductIds(productIds)

        val productInfos = products.map { product ->
            val brand = brandMap[product.brandId]
            val likeCount = likeCountMap[product.id] ?: 0L
            ProductInfo.Main.from(product, brand, likeCount)
        }

        return when (sortType) {
            ProductSortType.LATEST -> productInfos.sortedByDescending { it.id }
            ProductSortType.PRICE_ASC -> productInfos.sortedBy { it.price }
            ProductSortType.LIKES_DESC -> productInfos.sortedByDescending { it.likeCount }
        }
    }

    @Transactional
    fun changeInfo(id: Long, command: ChangeInfoCommand): ProductInfo.Detail {
        val product = productChanger.changeInfo(
            id = id,
            name = command.name,
            price = command.price,
            description = command.description,
        )
        val brand = brandReader.getById(product.brandId)
        val likeCount = likeReader.countByProductId(product.id!!)
        return ProductInfo.Detail.from(product, brand, likeCount)
    }

    @Transactional
    fun remove(id: Long) {
        productRemover.remove(id)
    }

    data class RegisterCommand(
        val brandId: Long,
        val name: String,
        val price: Long,
        val description: String,
        val stock: Int,
    )

    data class ChangeInfoCommand(
        val name: String,
        val price: Long,
        val description: String,
    )
}
