package com.loopers.application.product

import com.loopers.domain.brand.BrandReader
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
    private val productCacheStore: ProductCacheStore,
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
        productCacheStore.evictList()
        productCacheStore.evictList(command.brandId)
        return ProductInfo.Detail.from(product, brand)
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): ProductInfo.Detail {
        return productCacheStore.getDetail(id) {
            val product = productReader.getById(id)
            val brand = brandReader.getById(product.brandId)
            ProductInfo.Detail.from(product, brand)
        }
    }

    @Transactional(readOnly = true)
    fun getAll(sortType: ProductSortType, brandId: Long?): List<ProductInfo.Main> {
        return productCacheStore.getList(sortType, brandId) {
            val products = productReader.getAll(sortType, brandId)
            if (products.isEmpty()) {
                return@getList emptyList()
            }
            val brandIds = products.map { it.brandId }.distinct()
            val brandMap = brandReader.getAllByIds(brandIds).associateBy { it.id }

            products.map { product ->
                val brand = brandMap[product.brandId]
                ProductInfo.Main.from(product, brand)
            }
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
        productCacheStore.evictDetail(requireNotNull(product.id))
        productCacheStore.evictList()
        productCacheStore.evictList(product.brandId)
        return ProductInfo.Detail.from(product, brand)
    }

    @Transactional
    fun remove(id: Long) {
        val product = productReader.getById(id)
        productRemover.remove(id)
        productCacheStore.evictDetail(id)
        productCacheStore.evictList()
        productCacheStore.evictList(product.brandId)
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
