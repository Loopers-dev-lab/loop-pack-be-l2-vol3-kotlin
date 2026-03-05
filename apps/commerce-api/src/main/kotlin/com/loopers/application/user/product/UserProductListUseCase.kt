package com.loopers.application.user.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProductListUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {
    @Transactional(readOnly = true)
    fun getList(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: String?,
    ): PageResponse<UserProductResult.Summary> {
        val sortType = sort?.let {
            try {
                Product.SortType.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                throw CoreException(ErrorType.BAD_REQUEST)
            }
        }
        val productPage = productRepository.findAllActive(pageRequest, brandId, sortType)

        val brandIds = productPage.content.map { it.brandId }.distinct()
        val activeBrands = brandRepository.findAllByIdIn(brandIds)
            .filter { it.status == Brand.Status.ACTIVE }
            .associateBy { it.id!! }

        // TODO: Application 레벨 필터링으로 인해 content 건수와 totalElements가 불일치할 수 있음.
        //  Repository 레벨 Brand JOIN 필터로 개선 필요 (페이징 정합성).
        val filteredProducts = productPage.content.filter { activeBrands.containsKey(it.brandId) }

        return PageResponse(
            content = filteredProducts.map { product ->
                val brand = activeBrands[product.brandId]!!
                UserProductResult.Summary.from(product, brand)
            },
            totalElements = productPage.totalElements,
            page = productPage.page,
            size = productPage.size,
        )
    }
}
