package com.loopers.application.catalog

import com.loopers.application.SliceResult
import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandInfo
import com.loopers.domain.catalog.BrandRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminGetBrandsUseCase(
    private val brandRepository: BrandRepository,
) : UseCase<ListBrandsCriteria, ListBrandsResult> {

    @Transactional(readOnly = true)
    override fun execute(criteria: ListBrandsCriteria): ListBrandsResult {
        val pageable = PageRequest.of(criteria.page, criteria.size)
        val slice = brandRepository.findAll(pageable)
        val sliceResult = SliceResult.from(slice) { GetBrandResult.from(BrandInfo.from(it)) }
        return ListBrandsResult.from(sliceResult)
    }
}
