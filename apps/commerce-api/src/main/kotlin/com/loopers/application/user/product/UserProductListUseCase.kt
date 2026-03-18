package com.loopers.application.user.product

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductQueryRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProductListUseCase(
    private val productQueryRepository: ProductQueryRepository,
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
        return productQueryRepository.getList(pageRequest, brandId, sortType)
            .map(UserProductResult.Summary::from)
    }
}
