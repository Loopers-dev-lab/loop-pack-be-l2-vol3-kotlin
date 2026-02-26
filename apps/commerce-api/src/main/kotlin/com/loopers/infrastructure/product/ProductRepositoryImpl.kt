package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductSortType
import com.loopers.support.PageResult
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
) : ProductRepository {

    override fun findByIdOrNull(id: Long): Product? {
        return productJpaRepository.findByIdOrNull(id)
    }

    override fun findActiveByIdOrNull(id: Long): Product? {
        return productJpaRepository.findActiveByIdOrNull(id)
    }

    override fun findAllByCondition(condition: ProductSearchCondition): PageResult<Product> {
        val spec = buildSpecification(condition)
        val sort = buildSort(condition.sort)
        val pageable = PageRequest.of(condition.page, condition.size, sort)

        val page = productJpaRepository.findAll(spec, pageable)

        return PageResult.of(
            content = page.content,
            page = condition.page,
            size = condition.size,
            totalElements = page.totalElements,
        )
    }

    override fun findAllActiveByBrandId(brandId: Long): List<Product> {
        return productJpaRepository.findAllActiveByBrandId(brandId)
    }

    override fun save(product: Product): Product {
        return productJpaRepository.save(product)
    }

    override fun saveAll(products: List<Product>): List<Product> {
        return productJpaRepository.saveAll(products)
    }

    private fun buildSpecification(condition: ProductSearchCondition): Specification<Product> {
        return Specification { root: Root<Product>, _, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            if (!condition.includeDeleted) {
                predicates.add(cb.isNull(root.get<Any>("deletedAt")))
            }

            condition.brandId?.let { brandId ->
                predicates.add(cb.equal(root.get<Long>("brandId"), brandId))
            }

            cb.and(*predicates.toTypedArray())
        }
    }

    private fun buildSort(sortType: ProductSortType): Sort {
        return when (sortType) {
            ProductSortType.LATEST -> Sort.by(Sort.Direction.DESC, "createdAt")
            ProductSortType.PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price.amount")
            ProductSortType.POPULARITY -> Sort.by(Sort.Direction.DESC, "likeCount")
        }
    }
}
