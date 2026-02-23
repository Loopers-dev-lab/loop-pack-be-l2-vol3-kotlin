package com.loopers.application.product

import com.loopers.domain.product.Description
import com.loopers.domain.product.ImageUrl
import com.loopers.domain.product.Name
import com.loopers.domain.product.Price
import com.loopers.domain.product.ProductInventoryModel
import com.loopers.domain.product.ProductInventoryRepository
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.Stock
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productInventoryRepository: ProductInventoryRepository,
) {

    @Transactional
    fun createProduct(
        brandId: Long,
        name: String,
        imageUrl: String,
        description: String,
        price: Long,
        quantity: Long,
    ): ProductModel {
        if (productRepository.existsBy(brandId, Name(name))) {
            throw CoreException(
                errorType = ErrorType.CONFLICT,
                customMessage = "이미 존재하는 상품입니다.",
            )
        }

        val product = ProductModel(
            brandId = brandId,
            name = Name(name),
            imageUrl = ImageUrl(imageUrl),
            description = Description(description),
            price = Price(price),
        )

        val savedProduct = productRepository.save(product)

        val inventory = ProductInventoryModel(
            productId = savedProduct.id,
            stock = Stock(quantity),
        )

        productInventoryRepository.save(inventory)
        return savedProduct
    }

    @Transactional(readOnly = true)
    fun getProducts(brandId: Long?, pageable: Pageable = PageRequest.of(0, 20)): Page<ProductModel> {
        return if (brandId != null) {
            productRepository.findAllByBrandId(brandId, pageable)
        } else {
            productRepository.findAll(pageable)
        }
    }

    @Transactional(readOnly = true)
    fun getProductById(id: Long): ProductModel {
        return productRepository.findById(id) ?: throw CoreException(
            errorType = ErrorType.NOT_FOUND,
            customMessage = "해당 상품을 찾을 수 없습니다.",
        )
    }

    @Transactional
    fun updateProduct(
        id: Long,
        name: String,
        imageUrl: String,
        description: String,
        price: Long,
        quantity: Long,
    ): ProductModel {
        val product = productRepository.findById(id) ?: throw CoreException(
            errorType = ErrorType.NOT_FOUND,
            customMessage = "해당 상품을 찾을 수 없습니다.",
        )

        val inventory = productInventoryRepository.findByProductId(id) ?: throw CoreException(
            errorType = ErrorType.NOT_FOUND,
            customMessage = "해당 상품의 재고를 찾을 수 없습니다.",
        )

        product.update(
            name = Name(name),
            imageUrl = ImageUrl(imageUrl),
            description = Description(description),
            price = Price(price),
        )
        inventory.updateStock(quantity)

        return product
    }

    @Transactional
    fun deleteProduct(id: Long) {
        val product = productRepository.findById(id) ?: throw CoreException(
            errorType = ErrorType.NOT_FOUND,
            customMessage = "해당 상품을 찾을 수 없습니다.",
        )

        val inventory = productInventoryRepository.findByProductId(id)

        product.delete()
        inventory?.delete()
    }
}
