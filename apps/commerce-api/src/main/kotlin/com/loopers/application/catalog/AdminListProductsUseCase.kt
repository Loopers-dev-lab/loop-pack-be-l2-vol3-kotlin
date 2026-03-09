package com.loopers.application.catalog

import com.loopers.application.SliceResult
import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.ProductService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminListProductsUseCase(
    private val productService: ProductService,
    private val brandService: BrandService,
) : UseCase<ListProductsCriteria, ListProductsResult> {

    @Transactional(readOnly = true)
    override fun execute(criteria: ListProductsCriteria): ListProductsResult {
        val pageable = PageRequest.of(criteria.page, criteria.size)
        val slice = productService.getProducts(pageable, criteria.brandId)

        val brandIds = slice.content.map { it.brandId }.distinct()
        val brandNameMap = brandIds.associateWith { id ->
            brandService.findBrand(id)?.name ?: ""
        }

        val sliceResult = SliceResult.from(slice) { info ->
            GetProductResult.from(info, brandName = brandNameMap[info.brandId] ?: "")
        }
        return ListProductsResult.from(sliceResult)
    }
}
