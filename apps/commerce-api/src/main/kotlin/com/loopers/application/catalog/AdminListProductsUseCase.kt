package com.loopers.application.catalog

import com.loopers.application.SliceResult
import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandRepository
import com.loopers.domain.catalog.ProductInfo
import com.loopers.domain.catalog.ProductRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminListProductsUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) : UseCase<ListProductsCriteria, ListProductsResult> {

    @Transactional(readOnly = true)
    override fun execute(criteria: ListProductsCriteria): ListProductsResult {
        val pageable = PageRequest.of(criteria.page, criteria.size)
        val slice = criteria.brandId
            ?.let { productRepository.findAllByBrandId(it, pageable) }
            ?: productRepository.findAll(pageable)

        val brandIds = slice.content.map { it.brandId }.distinct()
        val brandNameMap = brandIds.associateWith { id ->
            brandRepository.findById(id)?.name ?: ""
        }

        val sliceResult = SliceResult.from(slice) { model ->
            val info = ProductInfo.from(model)
            GetProductResult.from(info, brandName = brandNameMap[model.brandId] ?: "")
        }
        return ListProductsResult.from(sliceResult)
    }
}
