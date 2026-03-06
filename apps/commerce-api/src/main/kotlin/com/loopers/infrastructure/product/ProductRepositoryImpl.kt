package com.loopers.infrastructure.product

import com.loopers.domain.product.Name
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import org.springframework.data.domain.Page
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Pageable

@Repository
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
) : ProductRepository {

    override fun save(product: ProductModel): ProductModel {
        return productJpaRepository.save(product)
    }

    override fun findById(id: Long): ProductModel? {
        return productJpaRepository.findById(id).orElse(null)
    }

    override fun findAll(pageable: Pageable): Page<ProductModel> {
        return productJpaRepository.findAll(pageable)
    }

    override fun findAllByBrandId(brandId: Long, pageable: Pageable): Page<ProductModel> {
        return productJpaRepository.findAllByBrandId(brandId, pageable)
    }

    override fun findAllByBrandId(brandId: Long): List<ProductModel> {
        return productJpaRepository.findAllByBrandId(brandId)
    }

    override fun existsBy(brandId: Long, name: Name): Boolean {
        return productJpaRepository.existsByBrandIdAndName(brandId, name)
    }

    override fun incrementLikeCount(id: Long) =
        productJpaRepository.incrementLikeCount(id)

    override fun decrementLikeCount(id: Long) =
        productJpaRepository.decrementLikeCount(id)
}
